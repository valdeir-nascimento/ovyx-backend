package io.github.ovyx.production.fixtures;

import io.github.ovyx.production.domain.model.DailyReport;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.production.domain.port.DailyReportRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dublê em memória do repositório de relatórios, para os testes do domínio e dos casos de uso.
 *
 * <p>Fiel ao adaptador real: o relatório é do setor, e a data em uso é a de outro relatório do mesmo
 * setor.
 */
public final class InMemoryDailyReportRepository implements DailyReportRepository {

    private final Map<DailyReportId, DailyReport> stored = new LinkedHashMap<>();
    private int saves;

    @Override
    public void save(DailyReport report) {
        stored.put(report.id(), report);
        saves++;
    }

    @Override
    public Optional<DailyReport> findById(SectorId sectorId, DailyReportId id) {
        return Optional.ofNullable(stored.get(id)).filter(report -> report.sectorId().equals(sectorId));
    }

    @Override
    public boolean anotherReportOn(SectorId sectorId, LocalDate collectionDate, DailyReportId exceptId) {
        return stored.values().stream()
                .filter(report -> !report.id().equals(exceptId))
                .filter(report -> report.sectorId().equals(sectorId))
                .anyMatch(report -> report.collectionDate().value().equals(collectionDate));
    }

    /** Quantas gravações houve: é como o teste prova que uma recusa não gravou nada. */
    public int saves() {
        return saves;
    }
}
