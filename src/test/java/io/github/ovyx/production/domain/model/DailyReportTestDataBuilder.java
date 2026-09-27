package io.github.ovyx.production.domain.model;

import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.aFarmSector;

import io.github.ovyx.production.domain.port.DailyReportRoster;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Relatório diário para os testes, aberto pelo próprio agregado: por padrão, o de 24/09/2026 do setor do
 * {@link FarmSectorTestDataBuilder}, às 06:30, com 98 aves e 20 semanas, aberto por Marina Alves.
 */
public final class DailyReportTestDataBuilder {

    public static final Actor MARINA =
            new Actor(UUID.fromString("1e3a5c7b-9d1f-4b3d-8e5a-7c9e1a3b5d77"), "Marina Alves");
    public static final Actor JOAO =
            new Actor(UUID.fromString("5d7f9b1d-3f5a-4c7e-9a1b-3d5f7a9c1e88"), "João Pereira");
    public static final LocalDate TODAY = LocalDate.of(2026, 9, 25);
    public static final Instant OPENED_AT = Instant.parse("2026-09-24T09:31:40Z");

    private FarmSector sector = aFarmSector().build();
    private String date = "2026-09-24";
    private String time = "06:30";
    private String birds = "98";
    private String age = "20";
    private String note = "Bebedouro da bateria B trocado.";
    private Actor actor = MARINA;
    private DailyReportRoster roster = (sectorId, collectionDate, exceptId) -> false;

    private DailyReportTestDataBuilder() {}

    public static DailyReportTestDataBuilder aDailyReport() {
        return new DailyReportTestDataBuilder();
    }

    public DailyReportTestDataBuilder in(FarmSector sector) {
        this.sector = sector;
        return this;
    }

    public DailyReportTestDataBuilder on(String date) {
        this.date = date;
        return this;
    }

    public DailyReportTestDataBuilder withOpeningBirds(int birds) {
        this.birds = String.valueOf(birds);
        return this;
    }

    public DailyReportTestDataBuilder withoutNote() {
        this.note = null;
        return this;
    }

    public DailyReportTestDataBuilder by(Actor actor) {
        this.actor = actor;
        return this;
    }

    public DailyReportTestDataBuilder withRoster(DailyReportRoster roster) {
        this.roster = roster;
        return this;
    }

    public DailyReport build() {
        return DailyReport.open(sector, date, time, birds, age, note, actor, TODAY, roster, OPENED_AT);
    }
}
