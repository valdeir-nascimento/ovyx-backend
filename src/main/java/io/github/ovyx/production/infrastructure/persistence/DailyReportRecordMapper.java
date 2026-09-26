package io.github.ovyx.production.infrastructure.persistence;

import io.github.ovyx.production.domain.model.Actor;
import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.production.domain.model.DailyReport;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.ReportCage;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.production.domain.valueobject.CollectionDate;
import io.github.ovyx.production.domain.valueobject.CollectionTime;
import io.github.ovyx.production.domain.valueobject.EggGrades;
import io.github.ovyx.production.domain.valueobject.FlockAge;
import io.github.ovyx.production.domain.valueobject.MortalityEntry;
import io.github.ovyx.production.domain.valueobject.MortalityNote;
import io.github.ovyx.production.domain.valueobject.OpeningBirdCount;
import io.github.ovyx.production.domain.valueobject.ProductionEntry;
import io.github.ovyx.production.domain.valueobject.ReportNote;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Traducao entre o agregado, com as gaiolas, e as linhas das tabelas.
 *
 * <p>Na volta usa {@code DailyReport.restore} e {@code ReportCage.restore}, e nao a abertura: os dados ja
 * foram validados quando entraram.
 */
final class DailyReportRecordMapper {

    private DailyReportRecordMapper() {}

    static DailyReportRecord toRecord(DailyReport report) {
        DailyReportRecord record = new DailyReportRecord(
                report.id().value(),
                report.sectorId().value(),
                report.collectionDate().value(),
                report.collectionTime().value(),
                report.openingBirdCount().value(),
                report.flockAge().value(),
                noteOf(report),
                report.noMortalityConfirmed(),
                report.openedBy().id(),
                report.openedBy().name(),
                report.openedAt(),
                report.lastCorrectedBy().map(Actor::id).orElse(null),
                report.lastCorrectedBy().map(Actor::name).orElse(null),
                report.lastCorrectedAt().orElse(null));
        report.cages().forEach(cage -> record.getCages().add(toRecord(report.id().value(), cage)));
        return record;
    }

    /**
     * Aplica o agregado sobre as linhas carregadas: as colunas do relatorio e os lancamentos de cada
     * gaiola. As gaiolas nao mudam de lugar nem de quantidade: foram fixadas na abertura (FR-005).
     *
     * @return se alguma coluna do relatorio mudou: nesse caso a versao ja sobe sozinha; nos outros, quem
     *     grava precisa sobe-la
     */
    static boolean applyTo(DailyReportRecord record, DailyReport report) {
        UUID correctedById = report.lastCorrectedBy().map(Actor::id).orElse(null);
        boolean changed = !Objects.equals(record.getCollectionDate(), report.collectionDate().value())
                || !Objects.equals(record.getCollectionTime(), report.collectionTime().value())
                || record.getOpeningBirdCount() != report.openingBirdCount().value()
                || record.getFlockAge() != report.flockAge().value()
                || !Objects.equals(record.getNote(), noteOf(report))
                || record.isNoMortalityConfirmed() != report.noMortalityConfirmed()
                || !Objects.equals(record.getLastCorrectedById(), correctedById)
                || !Objects.equals(record.getLastCorrectedAt(), report.lastCorrectedAt().orElse(null));
        record.apply(
                report.collectionDate().value(),
                report.collectionTime().value(),
                report.openingBirdCount().value(),
                report.flockAge().value(),
                noteOf(report),
                report.noMortalityConfirmed(),
                correctedById,
                report.lastCorrectedBy().map(Actor::name).orElse(null),
                report.lastCorrectedAt().orElse(null));
        Map<UUID, ReportCage> cages =
                report.cages().stream().collect(Collectors.toMap(cage -> cage.cageId().value(), Function.identity()));
        record.getCages().forEach(cage -> applyEntries(cage, cages.get(cage.getCageId())));
        return changed;
    }

    static DailyReport toDomain(DailyReportRecord record) {
        return DailyReport.restore(
                DailyReportId.of(record.getId()),
                SectorId.of(record.getSectorId()),
                new CollectionDate(record.getCollectionDate()),
                new CollectionTime(record.getCollectionTime()),
                new OpeningBirdCount(record.getOpeningBirdCount()),
                new FlockAge(record.getFlockAge()),
                record.getNote() == null ? null : new ReportNote(record.getNote()),
                record.isNoMortalityConfirmed(),
                record.getCages().stream().map(DailyReportRecordMapper::toDomain).toList(),
                new Actor(record.getOpenedById(), record.getOpenedByName()),
                record.getOpenedAt(),
                record.getLastCorrectedById() == null
                        ? null
                        : new Actor(record.getLastCorrectedById(), record.getLastCorrectedByName()),
                record.getLastCorrectedAt());
    }

    private static ReportCageRecord toRecord(UUID reportId, ReportCage cage) {
        ReportCageRecord record = new ReportCageRecord(
                reportId, cage.cageId().value(), cage.battery(), cage.number(), cage.birdCount());
        applyEntries(record, cage);
        return record;
    }

    private static void applyEntries(ReportCageRecord record, ReportCage cage) {
        applyProduction(record, cage);
        MortalityEntry mortality = cage.mortality().orElse(null);
        if (mortality == null) {
            record.applyMortality(null, null, null);
        } else {
            record.applyMortality(
                    mortality.deaths(),
                    mortality.culls(),
                    mortality.note() == null ? null : mortality.note().value());
        }
    }

    private static void applyProduction(ReportCageRecord record, ReportCage cage) {
        ProductionEntry production = cage.production().orElse(null);
        if (production == null) {
            record.applyProduction(null, null, null, null, null, null, null);
            return;
        }
        EggGrades grades = production.grades();
        record.applyProduction(
                production.eggs(),
                grades.small(),
                grades.jumbo(),
                grades.dirty(),
                grades.cracked(),
                grades.bloodSpot(),
                grades.abnormal());
    }

    private static ReportCage toDomain(ReportCageRecord record) {
        return ReportCage.restore(
                CageId.of(record.getCageId()),
                record.getBattery(),
                record.getNumber(),
                record.getBirdCount(),
                productionOf(record),
                mortalityOf(record));
    }

    private static MortalityEntry mortalityOf(ReportCageRecord record) {
        if (record.getDeaths() == null) {
            return null;
        }
        return new MortalityEntry(
                record.getDeaths(),
                record.getCulls(),
                record.getMortalityNote() == null ? null : new MortalityNote(record.getMortalityNote()));
    }

    private static ProductionEntry productionOf(ReportCageRecord record) {
        if (record.getEggs() == null) {
            return null;
        }
        return new ProductionEntry(
                record.getEggs(),
                new EggGrades(
                        record.getSmall(),
                        record.getJumbo(),
                        record.getDirty(),
                        record.getCracked(),
                        record.getBloodSpot(),
                        record.getAbnormal()));
    }

    private static String noteOf(DailyReport report) {
        return report.note().map(ReportNote::value).orElse(null);
    }
}
