package io.github.ovyx.production.infrastructure.persistence;

import io.github.ovyx.production.domain.model.DailyReport;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.production.domain.port.DailyReportRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador da porta {@code DailyReportRepository} sobre JPA. */
@Repository
public class JpaDailyReportRepository implements DailyReportRepository {

    private final DailyReportJpaRepository jpaRepository;
    private final EntityManager entityManager;

    JpaDailyReportRepository(DailyReportJpaRepository jpaRepository, EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void save(DailyReport report) {
        jpaRepository
                .findById(report.id().value())
                .ifPresentOrElse(
                        existing -> {
                            // A versao e do relatorio inteiro, gaiolas inclusive (R-003). Mudar so uma gaiola
                            // nao muda a linha do relatorio, e a versao nao subiria sozinha.
                            if (!DailyReportRecordMapper.applyTo(existing, report)) {
                                entityManager.lock(existing, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                            }
                        },
                        // Relatorio novo: persist, e nao merge, que consultaria cada gaiola antes de inserir.
                        () -> entityManager.persist(DailyReportRecordMapper.toRecord(report)));
    }

    /**
     * O relatorio para uma escrita, com a linha dele bloqueada ate o fim da transacao do comando.
     *
     * <p>A versao e do relatorio inteiro (R-003): sem o bloqueio, as pessoas que lancam gaiolas do mesmo
     * dia disputavam a versao, e a terceira perdia as duas tentativas do despachante e virava 500 (revisao
     * da T110). Com ele, os lancamentos no mesmo relatorio esperam um ao outro, cada um de poucos
     * milissegundos; relatorios diferentes nao se esperam. So os comandos carregam o agregado: as leituras
     * vao pela porta de consulta, sem bloqueio.
     */
    @Override
    @Transactional
    public Optional<DailyReport> findById(SectorId sectorId, DailyReportId id) {
        return Optional.ofNullable(
                        entityManager.find(DailyReportRecord.class, id.value(), LockModeType.PESSIMISTIC_WRITE))
                .filter(record -> record.getSectorId().equals(sectorId.value()))
                .map(DailyReportRecordMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean anotherReportOn(SectorId sectorId, LocalDate collectionDate, DailyReportId exceptId) {
        return jpaRepository.existsOn(sectorId.value(), collectionDate, exceptId.value());
    }
}
