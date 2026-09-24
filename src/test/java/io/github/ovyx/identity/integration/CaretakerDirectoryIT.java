package io.github.ovyx.identity.integration;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.application.caretaker.CaretakerDetail;
import io.github.ovyx.identity.application.caretaker.CaretakerDirectory;
import io.github.ovyx.identity.application.caretaker.CaretakerSummary;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.application.PageResponse;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Adaptador da porta de leitura de responsaveis contra PostgreSQL real (T083, T084).
 *
 * <p>O banco e compartilhado por todas as classes, e outros testes cadastram responsaveis o tempo
 * todo. Por isso cada teste marca os nomes que cria com um sinal unico e pesquisa por ele: a
 * pesquisa so enxerga o que o proprio teste criou, e o que ele criou sem o sinal prova o que nao
 * deve aparecer.
 */
@DisplayName("Caretaker directory")
class CaretakerDirectoryIT extends IntegrationTestSupport {

    @Autowired
    private CaretakerDirectory directory;

    @Autowired
    private CaretakerRepository repository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private Clock clock;

    /** Sinal unico deste teste, em maiusculas; so letras, para caber em qualquer nome. */
    private final String mark = "Q" + UUID.randomUUID().toString().replaceAll("[^a-f]", "").toUpperCase(Locale.ROOT);

    private Caretaker saved(String fullName) {
        Caretaker caretaker = aUniqueCaretaker()
                .withFullName(fullName)
                .withHasher(passwordHasher)
                .withRoster(repository)
                .withClock(clock)
                .build();
        repository.save(caretaker);
        return caretaker;
    }

    private Caretaker savedInactive(String fullName) {
        Caretaker caretaker = saved(fullName);
        caretaker.deactivate(repository, clock);
        repository.save(caretaker);
        return caretaker;
    }

    @Test
    @DisplayName("finds names containing the fragment, whatever the case, and nothing else")
    void givenNamesInAnotherCase_whenSearchingByAFragment_thenFindThemAndOnlyThem() {
        // given
        Caretaker ana = saved("Ana " + mark + " Pereira");
        Caretaker bruno = saved("Bruno " + mark.toLowerCase(Locale.ROOT) + " Souza");
        saved("Carla Sem Sinal " + UUID.randomUUID().toString().replaceAll("[^a-f]", ""));

        // when
        PageResponse<CaretakerSummary> page = directory.search(mark.toLowerCase(Locale.ROOT), null, 0, 20);

        // then
        assertThat(page.content()).extracting(CaretakerSummary::id).containsExactly(ana.id(), bruno.id());
        assertThat(page.metadata().totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("orders alphabetically ignoring case, and pages with the totals of the whole search")
    void givenThreeMatches_whenSearchingInPagesOfTwo_thenOrderAlphabeticallyAndReportTheTotals() {
        // given
        Caretaker carla = saved("Carla " + mark);
        Caretaker ana = saved("ana " + mark);
        Caretaker bruno = saved("Bruno " + mark);

        // when
        PageResponse<CaretakerSummary> first = directory.search(mark, null, 0, 2);
        PageResponse<CaretakerSummary> second = directory.search(mark, null, 1, 2);

        // then
        assertThat(first.content()).extracting(CaretakerSummary::id).containsExactly(ana.id(), bruno.id());
        assertThat(second.content()).extracting(CaretakerSummary::id).containsExactly(carla.id());
        assertThat(first.metadata().totalElements()).isEqualTo(3);
        assertThat(first.metadata().totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("breaks ties between equal names by the identifier, so nobody repeats or goes missing between pages")
    void givenFiveCaretakersWithTheSameName_whenPagingOneByOne_thenVisitEachOnceInIdentifierOrder() {
        // given
        // Sem o desempate, o PostgreSQL devolve os empates na ordem que quiser a cada consulta, e uma
        // pessoa podia aparecer em duas paginas enquanto outra sumia da lista.
        List<CaretakerId> namesakes = Stream.generate(() -> saved("Homônimo " + mark))
                .limit(5)
                .map(Caretaker::id)
                .toList();

        // when
        List<CaretakerId> visited = IntStream.range(0, 5)
                .mapToObj(page -> directory.search(mark, null, page, 1).content().getFirst().id())
                .toList();

        // then
        // O uuid do PostgreSQL compara byte a byte, a mesma ordem do texto em hexadecimal.
        assertThat(visited)
                .containsExactlyElementsOf(namesakes.stream()
                        .sorted(Comparator.comparing(id -> id.value().toString()))
                        .toList());
    }

    @Test
    @DisplayName("filters by status, and brings both when no status is asked")
    void givenActiveAndInactiveMatches_whenFilteringByStatus_thenBringOnlyThatStatus() {
        // given
        Caretaker active = saved("Ana " + mark);
        Caretaker inactive = savedInactive("Bruno " + mark);

        // when
        PageResponse<CaretakerSummary> onlyActive = directory.search(mark, CaretakerStatus.ACTIVE, 0, 20);
        PageResponse<CaretakerSummary> onlyInactive = directory.search(mark, CaretakerStatus.INACTIVE, 0, 20);
        PageResponse<CaretakerSummary> both = directory.search(mark, null, 0, 20);

        // then
        assertThat(onlyActive.content()).extracting(CaretakerSummary::id).containsExactly(active.id());
        assertThat(onlyInactive.content()).extracting(CaretakerSummary::id).containsExactly(inactive.id());
        assertThat(both.content()).hasSize(2);
    }

    @ParameterizedTest(name = "[{0}] finds [{1}] and not [{2}]")
    @CsvSource({"50%, Ana 50%, Bruno 50x", "a_b, Ana a_b, Bruno axb", "a\\b, Ana a\\b, Bruno ab"})
    @DisplayName("searches wildcard characters as typed text")
    void givenFragmentWithAWildcardCharacter_whenSearching_thenMatchItLiterally(
            String typed, String matchingName, String wildcardOnlyName) {
        // given
        // Sem escapar, "50%" casava com "50x" e "a_b" com "axb": o curinga do LIKE vazava da pesquisa.
        Caretaker matching = saved(matchingName + " " + mark);
        saved(wildcardOnlyName + " " + mark);

        // when
        PageResponse<CaretakerSummary> page = directory.search(typed + " " + mark, null, 0, 20);

        // then
        assertThat(page.content()).extracting(CaretakerSummary::id).containsExactly(matching.id());
    }

    @Test
    @DisplayName("returns the detail of a caretaker, inactive ones included")
    void givenInactiveCaretaker_whenFindingItsDetail_thenReturnEveryField() {
        // given
        Caretaker caretaker = savedInactive("Ana " + mark);

        // when
        Optional<CaretakerDetail> detail = directory.findDetail(caretaker.id());

        // then
        assertThat(detail).hasValueSatisfying(found -> {
            assertThat(found.cpf()).isEqualTo(caretaker.cpf().value());
            assertThat(found.email()).isEqualTo(caretaker.email().value());
            assertThat(found.mobilePhone()).isEqualTo(caretaker.mobilePhone().value());
            assertThat(found.status()).isEqualTo(CaretakerStatus.INACTIVE);
            // O PostgreSQL guarda microssegundos; o relogio da aplicacao tem mais precisao que isso.
            assertThat(found.createdAt()).isCloseTo(caretaker.createdAt(), within(1, ChronoUnit.MICROS));
            assertThat(found.updatedAt()).isCloseTo(caretaker.updatedAt(), within(1, ChronoUnit.MICROS));
        });
    }

    @Test
    @DisplayName("finds no detail for an unknown caretaker")
    void givenUnknownCaretaker_whenFindingItsDetail_thenReturnNothing() {
        // given
        CaretakerId unknown = CaretakerId.generate();

        // when
        Optional<CaretakerDetail> detail = directory.findDetail(unknown);

        // then
        assertThat(detail).isEmpty();
    }
}
