package io.github.ovyx.production.domain.port;

import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.SectorId;
import java.time.LocalDate;

/** Pergunta ao conjunto dos relatorios de um setor, para a regra de um relatorio por data (R-005). */
public interface DailyReportRoster {

    /** Se outro relatorio do setor, diferente de {@code exceptId}, tem a data da coleta. */
    boolean anotherReportOn(SectorId sectorId, LocalDate collectionDate, DailyReportId exceptId);
}
