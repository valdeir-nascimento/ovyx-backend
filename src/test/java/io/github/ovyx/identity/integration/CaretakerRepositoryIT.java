package io.github.ovyx.identity.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.identity.domain.valueobject.AccessIdentifier;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Adaptador de {@code CaretakerRepository} contra PostgreSQL real (T047).
 *
 * <p>A precedencia do ativo sobre o inativo (desvio D-003) mora na consulta JPQL, e so o banco real
 * a exercita: o duble em memoria dos testes de aplicacao reimplementa a regra por conta propria.
 */
@DisplayName("Caretaker repository")
class CaretakerRepositoryIT extends IntegrationTestSupport {

    private static final String PASSWORD = "GranjaNorte2026";

    @Autowired
    private CaretakerRepository repository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private Clock clock;

    private Caretaker register(String email, String mobilePhone) {
        Caretaker caretaker = Caretaker.register(
                "Responsável do Aviário",
                TestCaretakers.randomValidCpf(),
                email,
                mobilePhone,
                PASSWORD,
                Role.USER,
                false,
                passwordHasher,
                clock);
        repository.save(caretaker);
        return caretaker;
    }

    private static String uniqueEmail() {
        return "granja." + UUID.randomUUID().toString().substring(0, 8) + "@ovyx.com.br";
    }

    private static String uniqueMobilePhone() {
        return "919" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    private CaretakerId found(String canonicalIdentifier) {
        return repository.findByEmailOrMobilePhone(canonicalIdentifier).map(Caretaker::id).orElse(null);
    }

    private void deactivate(Caretaker caretaker) {
        caretaker.deactivate(clock);
        repository.save(caretaker);
    }

    @Test
    @DisplayName("finds by the canonical form of a typed email")
    void findsByCanonicalEmail() {
        String email = uniqueEmail();
        Caretaker caretaker = register(email, uniqueMobilePhone());

        String typed = "  " + email.toUpperCase() + " ";

        assertThat(found(AccessIdentifier.of(typed).value())).isEqualTo(caretaker.id());
    }

    @Test
    @DisplayName("finds by the canonical form of a formatted mobile phone")
    void findsByCanonicalMobilePhone() {
        String mobilePhone = uniqueMobilePhone();
        Caretaker caretaker = register(uniqueEmail(), mobilePhone);

        String typed = "(" + mobilePhone.substring(0, 2) + ") " + mobilePhone.substring(2, 7) + "-"
                + mobilePhone.substring(7);

        assertThat(found(AccessIdentifier.of(typed).value())).isEqualTo(caretaker.id());
    }

    @Test
    @DisplayName("matches only the exact identifier, without normalizing on its own")
    void matchesOnlyTheExactIdentifier() {
        // A normalizacao e de AccessIdentifier. Um segundo ajuste aqui fazia a busca e a contencao
        // usarem chaves diferentes para a mesma conta (FR-023).
        String email = uniqueEmail();
        String mobilePhone = uniqueMobilePhone();
        register(email, mobilePhone);

        assertThat(found(email.toUpperCase())).isNull();
        assertThat(found("x" + mobilePhone)).isNull();
        assertThat(found("")).isNull();
    }

    @Test
    @DisplayName("the active caretaker wins over an inactive one with the same email, even a newer one")
    void activeWinsOverInactiveSharingTheEmail() {
        // O inativo e criado depois do ativo, para que a ordem por data nao explique o resultado.
        String email = uniqueEmail();
        Caretaker active = register(email, uniqueMobilePhone());
        deactivate(active);
        Caretaker inactive = register(email, uniqueMobilePhone());
        deactivate(inactive);
        active.reactivate(clock);
        repository.save(active);

        assertThat(inactive.createdAt()).isAfterOrEqualTo(active.createdAt());
        assertThat(found(email)).isEqualTo(active.id());
    }

    @Test
    @DisplayName("the active caretaker wins over an inactive one with the same mobile phone")
    void activeWinsOverInactiveSharingTheMobilePhone() {
        String mobilePhone = uniqueMobilePhone();
        Caretaker active = register(uniqueEmail(), mobilePhone);
        deactivate(active);
        Caretaker inactive = register(uniqueEmail(), mobilePhone);
        deactivate(inactive);
        active.reactivate(clock);
        repository.save(active);

        assertThat(found(mobilePhone)).isEqualTo(active.id());
    }

    @Test
    @DisplayName("still returns an inactive caretaker when no active one shares the identifier")
    void returnsTheInactiveCaretakerWhenAlone() {
        // O tratador precisa da conta inativa para registrar a causa certa na auditoria.
        String email = uniqueEmail();
        Caretaker caretaker = register(email, uniqueMobilePhone());
        deactivate(caretaker);

        Caretaker reloaded = repository.findByEmailOrMobilePhone(email).orElseThrow();

        assertThat(reloaded.id()).isEqualTo(caretaker.id());
        assertThat(reloaded.isActive()).isFalse();
    }
}
