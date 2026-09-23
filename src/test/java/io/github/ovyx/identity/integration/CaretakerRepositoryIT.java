package io.github.ovyx.identity.integration;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.identity.domain.valueobject.AccessIdentifier;
import java.time.Clock;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Adaptador de {@code CaretakerRepository} contra PostgreSQL real (T047).
 *
 * <p>A precedencia do ativo sobre o inativo (desvio D-003) mora na consulta JPQL, e so o banco real
 * a exercita: o duble em memoria dos testes de aplicacao reimplementa a regra por conta propria.
 */
@DisplayName("Caretaker repository")
class CaretakerRepositoryIT extends IntegrationTestSupport {

    @Autowired
    private CaretakerRepository repository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private Clock clock;

    /** Responsavel que nenhum outro teste usa, gravado com o hasher e o relogio da aplicacao. */
    private CaretakerTestDataBuilder aCaretakerForThisDatabase() {
        return aUniqueCaretaker().withHasher(passwordHasher).withClock(clock);
    }

    private Caretaker saved(CaretakerTestDataBuilder builder) {
        Caretaker caretaker = builder.build();
        repository.save(caretaker);
        return caretaker;
    }

    private CaretakerId found(String canonicalIdentifier) {
        return repository.findByEmailOrMobilePhone(canonicalIdentifier).map(Caretaker::id).orElse(null);
    }

    private void deactivate(Caretaker caretaker) {
        caretaker.deactivate(clock);
        repository.save(caretaker);
    }

    private void reactivate(Caretaker caretaker) {
        caretaker.reactivate(clock);
        repository.save(caretaker);
    }

    private static String formatted(String mobilePhone) {
        return "(" + mobilePhone.substring(0, 2) + ") " + mobilePhone.substring(2, 7) + "-" + mobilePhone.substring(7);
    }

    private static Stream<Arguments> nonCanonicalLookups() {
        return Stream.of(
                Arguments.of(
                        "upper-cased email",
                        (Function<Caretaker, String>) caretaker -> caretaker.email().value().toUpperCase()),
                Arguments.of(
                        "mobile phone with a stray letter",
                        (Function<Caretaker, String>) caretaker -> "x" + caretaker.mobilePhone().value()),
                Arguments.of("empty identifier", (Function<Caretaker, String>) caretaker -> ""));
    }

    @Test
    @DisplayName("finds by the canonical form of a typed email")
    void givenEmailTypedWithSpacesAndCapitals_whenFindingByItsCanonicalForm_thenReturnTheCaretaker() {
        // given
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        String typed = "  " + caretaker.email().value().toUpperCase() + " ";

        // when
        CaretakerId id = found(AccessIdentifier.of(typed).value());

        // then
        assertThat(id).isEqualTo(caretaker.id());
    }

    @Test
    @DisplayName("finds by the canonical form of a formatted mobile phone")
    void givenFormattedMobilePhone_whenFindingByItsCanonicalForm_thenReturnTheCaretaker() {
        // given
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        String typed = formatted(caretaker.mobilePhone().value());

        // when
        CaretakerId id = found(AccessIdentifier.of(typed).value());

        // then
        assertThat(id).isEqualTo(caretaker.id());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonCanonicalLookups")
    @DisplayName("matches only the exact identifier, without normalizing on its own")
    void givenNonCanonicalIdentifier_whenFinding_thenMatchNobody(
            String situation, Function<Caretaker, String> lookup) {
        // given
        // A normalizacao e de AccessIdentifier. Um segundo ajuste aqui fazia a busca e a contencao
        // usarem chaves diferentes para a mesma conta (FR-023).
        Caretaker caretaker = saved(aCaretakerForThisDatabase());

        // when
        CaretakerId id = found(lookup.apply(caretaker));

        // then
        assertThat(id).isNull();
    }

    @Test
    @DisplayName("the active caretaker wins over an inactive one with the same email, even a newer one")
    void givenActiveAndNewerInactiveSharingTheEmail_whenFinding_thenReturnTheActiveOne() {
        // given
        // O inativo e criado depois do ativo, para que a ordem por data nao explique o resultado.
        Caretaker active = saved(aCaretakerForThisDatabase());
        String email = active.email().value();
        deactivate(active);
        Caretaker inactive = saved(aCaretakerForThisDatabase().withEmail(email));
        deactivate(inactive);
        reactivate(active);
        assertThat(inactive.createdAt())
                .as("precondition: the inactive one is not older")
                .isAfterOrEqualTo(active.createdAt());

        // when
        CaretakerId id = found(email);

        // then
        assertThat(id).isEqualTo(active.id());
    }

    @Test
    @DisplayName("the active caretaker wins over an inactive one with the same mobile phone")
    void givenActiveAndInactiveSharingTheMobilePhone_whenFinding_thenReturnTheActiveOne() {
        // given
        Caretaker active = saved(aCaretakerForThisDatabase());
        String mobilePhone = active.mobilePhone().value();
        deactivate(active);
        Caretaker inactive = saved(aCaretakerForThisDatabase().withMobilePhone(mobilePhone));
        deactivate(inactive);
        reactivate(active);

        // when
        CaretakerId id = found(mobilePhone);

        // then
        assertThat(id).isEqualTo(active.id());
    }

    @Test
    @DisplayName("still returns an inactive caretaker when no active one shares the identifier")
    void givenOnlyAnInactiveCaretakerWithTheIdentifier_whenFinding_thenReturnIt() {
        // given
        // O tratador precisa da conta inativa para registrar a causa certa na auditoria.
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        deactivate(caretaker);

        // when
        Caretaker reloaded = repository.findByEmailOrMobilePhone(caretaker.email().value()).orElseThrow();

        // then
        assertThat(reloaded.id()).isEqualTo(caretaker.id());
        assertThat(reloaded.isActive()).isFalse();
    }
}
