package io.github.ovyx.farm.infrastructure.persistence;

import io.github.ovyx.farm.domain.model.FeedFormula;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.farm.domain.port.FeedFormulaRepository;
import io.github.ovyx.farm.domain.valueobject.ExpectedIntake;
import io.github.ovyx.farm.domain.valueobject.FeedFormulaDescription;
import io.github.ovyx.farm.domain.valueobject.FeedFormulaName;
import io.github.ovyx.farm.domain.valueobject.PricePerKg;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador da porta {@code FeedFormulaRepository} sobre JPA.
 *
 * <p>Na volta usa {@code FeedFormula.restore}, e nao a fabrica de cadastro: os dados ja foram validados
 * quando entraram.
 */
@Repository
public class JpaFeedFormulaRepository implements FeedFormulaRepository {

    private final FeedFormulaJpaRepository jpaRepository;

    JpaFeedFormulaRepository(FeedFormulaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void save(FeedFormula formula) {
        // Atualiza a linha existente em vez de substitui-la, para preservar created_at e conferir a
        // versao da linha carregada.
        jpaRepository
                .findById(formula.id().value())
                .ifPresentOrElse(
                        existing -> existing.apply(
                                formula.name().value(),
                                formula.pricePerKg().value(),
                                formula.expectedIntake().value(),
                                descriptionOf(formula),
                                formula.status(),
                                formula.updatedAt()),
                        () -> jpaRepository.save(new FeedFormulaRecord(
                                formula.id().value(),
                                formula.name().value(),
                                formula.pricePerKg().value(),
                                formula.expectedIntake().value(),
                                descriptionOf(formula),
                                formula.status(),
                                formula.createdAt(),
                                formula.updatedAt())));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FeedFormula> findById(FeedFormulaId id) {
        return jpaRepository.findById(id.value()).map(JpaFeedFormulaRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean nameInUse(FeedFormulaName name, FeedFormulaId exceptId) {
        return jpaRepository.existsNamed(name.value(), exceptId.value());
    }

    private static FeedFormula toDomain(FeedFormulaRecord record) {
        return FeedFormula.restore(
                FeedFormulaId.of(record.getId()),
                new FeedFormulaName(record.getName()),
                new PricePerKg(record.getPricePerKg()),
                new ExpectedIntake(record.getExpectedIntake()),
                record.getDescription() == null ? null : new FeedFormulaDescription(record.getDescription()),
                record.getStatus(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    private static String descriptionOf(FeedFormula formula) {
        return formula.description().map(FeedFormulaDescription::value).orElse(null);
    }
}
