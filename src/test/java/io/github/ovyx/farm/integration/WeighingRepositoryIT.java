package io.github.ovyx.farm.integration;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aUniqueSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.Weighing;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.farm.domain.model.WeighingStatus;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.farm.domain.port.WeighingRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** O repositório das pesagens contra o PostgreSQL (R-003, R-005 e R-012 da 005). */
@DisplayName("Weighing repository")
class WeighingRepositoryIT extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);
    /** Instantes inteiros: o banco guarda microssegundos. */
    private static final Instant NOW = Instant.parse("2026-09-24T10:12:40Z");
    private static final Actor MARINA = new Actor(UUID.fromString("5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22"), "Marina Alves");

    @Autowired
    private WeighingRepository repository;

    @Autowired
    private SectorRepository sectors;

    @Autowired
    private JdbcTemplate jdbc;

    private Sector sectorWithCage() {
        Sector sector = aUniqueSector().withCage("A", 1, 48).build();
        sectors.save(sector);
        return sector;
    }

    private Weighing recorded(Sector sector, String day, String weight) {
        Weighing weighing = Weighing.record(
                sector, sector.cages().get(0).id(), day, weight, TODAY, repository, MARINA, NOW);
        repository.save(weighing);
        return weighing;
    }

    @Test
    @DisplayName("saves the weighing and reads it back as it was, with the weight at one decimal place")
    void givenRecordedWeighing_whenSavingAndReadingBack_thenFindTheSameData() {
        // given
        Sector sector = sectorWithCage();

        // when
        Weighing weighing = recorded(sector, "2026-09-24", "161,4");

        // then
        Weighing read = repository.findById(weighing.id()).orElseThrow();
        assertThat(read.sectorId()).isEqualTo(sector.id());
        assertThat(read.cageId()).isEqualTo(sector.cages().get(0).id());
        assertThat(read.weighedOn().value()).isEqualTo(TODAY);
        assertThat(read.averageWeight().value()).isEqualByComparingTo("161.4").hasScaleOf(1);
        assertThat(read.status()).isEqualTo(WeighingStatus.VALID);
        assertThat(read.recordedBy()).isEqualTo(MARINA);
        assertThat(read.recordedAt()).isEqualTo(NOW);
        assertThat(read.lastCorrectedBy()).isEmpty();
        assertThat(read.voidedBy()).isEmpty();
        Map<String, Object> row = jdbc.queryForMap(
                "select average_weight, status, recorded_by_name from weighing where id = ?", weighing.id().value());
        assertThat(row)
                .containsEntry("average_weight", new BigDecimal("161.4"))
                .containsEntry("status", "VALID")
                .containsEntry("recorded_by_name", "Marina Alves");
    }

    @Test
    @DisplayName("answers that a day is taken by a valid weighing of the cage, except for the weighing itself")
    void givenSavedWeighing_whenAskingAboutItsDay_thenAnswerTakenExceptForItself() {
        // given
        Sector sector = sectorWithCage();
        Weighing weighing = recorded(sector, "2026-09-24", "161");
        CageId cage = sector.cages().get(0).id();

        // when
        boolean takenForAnother = repository.isDayTaken(cage, TODAY, WeighingId.generate());
        boolean takenForItself = repository.isDayTaken(cage, TODAY, weighing.id());
        boolean otherDay = repository.isDayTaken(cage, TODAY.minusDays(7), WeighingId.generate());

        // then
        assertThat(takenForAnother).isTrue();
        assertThat(takenForItself).isFalse();
        assertThat(otherDay).isFalse();
    }

    // ---------------------------------------------------------------- correção e anulação (US4)

    @Test
    @DisplayName("saves the correction, raising the version, and the voiding, keeping the weighing")
    void givenWeighing_whenCorrectingAndVoiding_thenSaveBothAndKeepTheRow() {
        // given
        Sector sector = sectorWithCage();
        Weighing weighing = recorded(sector, "2026-09-24", "116");
        long before = jdbc.queryForObject("select version from weighing where id = ?", Long.class, weighing.id().value());
        Actor admin = new Actor(UUID.randomUUID(), "Administrador");

        // when
        Weighing loaded = repository.findById(weighing.id()).orElseThrow();
        loaded.correct(sector, "2026-09-24", "161", TODAY, repository, admin, NOW);
        repository.save(loaded);
        Weighing reloaded = repository.findById(weighing.id()).orElseThrow();
        reloaded.voidBy(sector, admin, NOW);
        repository.save(reloaded);

        // then
        Weighing read = repository.findById(weighing.id()).orElseThrow();
        assertThat(read.averageWeight().value()).isEqualByComparingTo("161.0");
        assertThat(read.lastCorrectedBy()).contains(admin);
        assertThat(read.status()).isEqualTo(WeighingStatus.VOIDED);
        assertThat(read.voidedBy()).contains(admin);
        assertThat(read.voidedAt()).contains(NOW);
        assertThat(jdbc.queryForObject("select version from weighing where id = ?", Long.class, weighing.id().value()))
                .isGreaterThan(before);
    }

    @Test
    @DisplayName("frees the day of a voided weighing")
    void givenVoidedWeighing_whenAskingAboutItsDay_thenAnswerFree() {
        // given
        Sector sector = sectorWithCage();
        Weighing weighing = recorded(sector, "2026-09-24", "999");
        weighing.voidBy(sector, MARINA, NOW);
        repository.save(weighing);

        // when
        boolean taken = repository.isDayTaken(sector.cages().get(0).id(), TODAY, WeighingId.generate());

        // then
        assertThat(taken).isFalse();
    }
}
