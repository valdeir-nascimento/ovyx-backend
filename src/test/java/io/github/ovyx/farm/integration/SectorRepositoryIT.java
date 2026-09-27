package io.github.ovyx.farm.integration;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aUniqueSector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.farm.domain.model.Cage;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.farm.domain.valueobject.SectorDescription;
import io.github.ovyx.farm.domain.valueobject.SectorName;
import io.github.ovyx.shared.domain.FixedClock;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * O repositório de setores contra PostgreSQL real (R-003, R-005).
 *
 * <p>Cada gravação e cada leitura passam pelo adaptador, em transações separadas: o que volta vem do
 * banco, e não de um cache do ORM.
 */
@DisplayName("Sector repository")
class SectorRepositoryIT extends IntegrationTestSupport {

    @Autowired
    private SectorRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    /** Instantes inteiros: o banco guarda microssegundos, e o relógio do sistema pode ter mais. */
    private final FixedClock clock = FixedClock.at("2026-09-25T13:02:11Z");

    private Sector saved(Sector sector) {
        repository.save(sector);
        return sector;
    }

    private Map<String, Object> rowOf(SectorId id) {
        return jdbc.queryForMap("select name, description, status, version from sector where id = ?", id.value());
    }

    @Test
    @DisplayName("saves the sector and reads it back as it was")
    void givenRegisteredSector_whenSavingAndReadingBack_thenFindTheSameData() {
        // given
        Sector sector = aUniqueSector()
                .describedAs("Codornas japonesas em postura, baterias A e B")
                .withRoster(repository)
                .withClock(clock)
                .build();

        // when
        repository.save(sector);

        // then
        Sector read = repository.findById(sector.id()).orElseThrow();
        assertThat(read.name()).isEqualTo(sector.name());
        assertThat(read.description())
                .map(SectorDescription::value)
                .contains("Codornas japonesas em postura, baterias A e B");
        assertThat(read.status()).isEqualTo(sector.status());
        assertThat(read.createdAt()).isEqualTo(sector.createdAt());
        assertThat(rowOf(sector.id())).containsEntry("status", "ACTIVE");
    }

    @Test
    @DisplayName("saves a sector without description as a null column")
    void givenSectorWithoutDescription_whenSaving_thenStoreNoDescription() {
        // given
        Sector sector = aUniqueSector().describedAs(" ").withRoster(repository).withClock(clock).build();

        // when
        repository.save(sector);

        // then
        assertThat(rowOf(sector.id())).containsEntry("description", null);
        assertThat(repository.findById(sector.id()).orElseThrow().description()).isEmpty();
    }

    @Test
    @DisplayName("starts the version at zero and raises it on every change")
    void givenSavedSector_whenUpdatingAndSaving_thenRaiseTheVersion() {
        // given
        Sector sector = saved(aUniqueSector().withRoster(repository).withClock(clock).build());
        long before = (long) rowOf(sector.id()).get("version");
        Sector loaded = repository.findById(sector.id()).orElseThrow();
        loaded.update(loaded.name().value() + " (norte)", null, repository, clock);

        // when
        repository.save(loaded);

        // then
        assertThat(before).isZero();
        assertThat(rowOf(sector.id())).containsEntry("version", 1L);
    }

    @Test
    @DisplayName("finds the name of another active sector, whatever the case")
    void givenActiveSector_whenAskingForItsNameInOtherCase_thenFindItInUse() {
        // given
        Sector sector = saved(aUniqueSector().withRoster(repository).withClock(clock).build());
        SectorName otherCase = SectorName.of(sector.name().value().toUpperCase());

        // when
        boolean inUse = repository.anotherActiveSectorNamed(otherCase, SectorId.generate());

        // then
        assertThat(inUse).isTrue();
    }

    @Test
    @DisplayName("does not count the sector itself as using its own name")
    void givenActiveSector_whenAskingForItsOwnName_thenFindItFree() {
        // given
        Sector sector = saved(aUniqueSector().withRoster(repository).withClock(clock).build());

        // when
        boolean inUse = repository.anotherActiveSectorNamed(sector.name(), sector.id());

        // then
        assertThat(inUse).isFalse();
    }

    @Test
    @DisplayName("does not count an inactive sector as using its name")
    void givenInactiveSector_whenAskingForItsName_thenFindItFree() {
        // given
        Sector inactive = saved(aUniqueSector().inactive().withClock(clock).build());

        // when
        boolean inUse = repository.anotherActiveSectorNamed(inactive.name(), SectorId.generate());

        // then
        assertThat(inUse).isFalse();
    }

    @Test
    @DisplayName("lets the database refuse a second active sector with the same name, the net of the race")
    void givenActiveSector_whenSavingAnotherActiveWithTheSameNameDirectly_thenRefuseAtTheDatabase() {
        // given
        // Os dois nascem sem olhar o quadro, como dois cadastros simultâneos que passaram pela
        // checagem do agregado antes de qualquer um gravar.
        Sector first = aUniqueSector().withClock(clock).build();
        Sector second = aUniqueSector().named(first.name().value()).withClock(clock).build();
        repository.save(first);

        // when
        ThrowingCallable savingTheSecond = () -> repository.save(second);

        // then
        assertThatThrownBy(savingTheSecond)
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ux_sector_name_active");
    }

    @Test
    @DisplayName("saves the cages with the sector and reads them back with it")
    void givenSectorWithCages_whenSavingAndReadingBack_thenFindTheCages() {
        // given
        Sector sector = aUniqueSector().withRoster(repository).withClock(clock).build();
        CageId b07 = sector.registerCage("B", "7", "50", clock);
        sector.registerCage("A", "12", "0", clock);

        // when
        repository.save(sector);

        // then
        Sector read = repository.findById(sector.id()).orElseThrow();
        assertThat(read.cages()).extracting(Cage::code).containsExactlyInAnyOrder("B-07", "A-12");
        Cage cage = read.cage(b07).orElseThrow();
        assertThat(cage.birdCount().value()).isEqualTo(50);
        assertThat(cage.isActive()).isTrue();
        assertThat(cage.deactivatedWithSector()).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from cage where sector_id = ?", Integer.class, sector.id().value()))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("raises the version of the sector on every write in one of its cages, even at the same instant")
    void givenSavedSector_whenWritingInItsCages_thenRaiseTheSectorVersionEachTime() {
        // given
        // O relógio não anda: sem o incremento forçado, mudar só a gaiola não mudava a linha do setor,
        // e duas escritas simultâneas em gaiolas do mesmo setor passavam sem conflito (R-003).
        Sector sector = saved(aUniqueSector().withRoster(repository).withClock(clock).build());
        Sector withCage = repository.findById(sector.id()).orElseThrow();
        CageId b07 = withCage.registerCage("B", "7", "50", clock);
        repository.save(withCage);
        Sector corrected = repository.findById(sector.id()).orElseThrow();
        corrected.updateCage(b07, "B", "7", "48", clock);

        // when
        repository.save(corrected);

        // then
        assertThat(rowOf(sector.id())).containsEntry("version", 2L);
        assertThat(repository.findById(sector.id()).orElseThrow().cage(b07).orElseThrow().birdCount().value())
                .isEqualTo(48);
    }

    @Test
    @DisplayName("lets the database refuse a second active cage with the same battery and number")
    void givenActiveCage_whenSavingAnotherWithTheSameBatteryAndNumberDirectly_thenRefuseAtTheDatabase() {
        // given
        // Duas cópias do mesmo setor, cada uma com uma B-07: como dois cadastros simultâneos que
        // passaram pela checagem do agregado antes de qualquer um gravar.
        Sector sector = saved(aUniqueSector().withRoster(repository).withClock(clock).build());
        Sector first = repository.findById(sector.id()).orElseThrow();
        Sector second = repository.findById(sector.id()).orElseThrow();
        first.registerCage("B", "7", "50", clock);
        second.registerCage("B", "7", "48", clock);
        repository.save(first);

        // when
        ThrowingCallable savingTheSecond = () -> repository.save(second);

        // then
        assertThatThrownBy(savingTheSecond).isInstanceOf(DataIntegrityViolationException.class);
    }
}
