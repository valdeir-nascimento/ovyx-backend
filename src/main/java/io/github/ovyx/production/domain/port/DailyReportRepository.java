package io.github.ovyx.production.domain.port;

import io.github.ovyx.production.domain.model.DailyReport;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.SectorId;
import java.util.Optional;

/**
 * Repositorio do agregado {@link DailyReport}: carrega e grava o relatorio inteiro, com as gaiolas.
 * Estende o {@link DailyReportRoster}, como o repositorio de setores do farm estende o roster dele.
 */
public interface DailyReportRepository extends DailyReportRoster {

    /** O relatorio, se for deste setor. */
    Optional<DailyReport> findById(SectorId sectorId, DailyReportId id);

    void save(DailyReport report);
}
