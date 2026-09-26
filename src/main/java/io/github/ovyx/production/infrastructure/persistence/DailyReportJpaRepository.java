package io.github.ovyx.production.infrastructure.persistence;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data da tabela {@code daily_report}.
 *
 * <p>Detalhe de infraestrutura: quem o dominio conhece e {@code DailyReportRepository}, implementada por
 * {@code JpaDailyReportRepository} sobre esta interface.
 */
interface DailyReportJpaRepository extends JpaRepository<DailyReportRecord, UUID> {

    /** Se outro relatorio do setor tem a data, com a mesma comparacao do indice unico. */
    @Query(
            value = """
                    select exists(
                        select 1 from daily_report
                         where sector_id = :sectorId and collection_date = :collectionDate and id <> :exceptId)
                    """,
            nativeQuery = true)
    boolean existsOn(
            @Param("sectorId") UUID sectorId,
            @Param("collectionDate") LocalDate collectionDate,
            @Param("exceptId") UUID exceptId);
}
