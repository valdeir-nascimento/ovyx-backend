package io.github.ovyx.identity.domain.model;

import static io.github.ovyx.identity.domain.DomainViolations.of;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.domain.FakePasswordHasher;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.domain.FixedClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes do agregado {@code Caretaker} — sem mock de dominio, conforme o principio VI.
 *
 * <p>As invariantes verificadas aqui sao as de data-model.md. As invariantes <em>entre</em>
 * agregados (unicidade de e-mail e celular, ultimo administrador ativo) nao cabem aqui: dependem
 * de consulta ao repositorio e sao verificadas nos testes dos handlers.
 */
@DisplayName("Caretaker")
class CaretakerTest {

    private final PasswordHasher hasher = new FakePasswordHasher();
    private final FixedClock clock = FixedClock.at("2026-09-19T12:00:00Z");

    private Caretaker register(String name, String cpf, String email, String phone, String password) {
        return Caretaker.register(name, cpf, email, phone, password, Role.USER, false, hasher, clock);
    }

    private Caretaker validCaretaker() {
        return register(
                "João Pereira de Souza",
                "52998224725",
                "joao.pereira@ovyx.com.br",
                "91991234567",
                "AviarioSul2026");
    }

    @Test
    @DisplayName("is born active with identity and timestamps")
    void isBornActive() {
        Caretaker caretaker = validCaretaker();

        assertThat(caretaker.id()).isNotNull();
        assertThat(caretaker.status()).isEqualTo(CaretakerStatus.ACTIVE);
        assertThat(caretaker.role()).isEqualTo(Role.USER);
        assertThat(caretaker.mustChangePassword()).isFalse();
        assertThat(caretaker.createdAt()).isEqualTo(clock.instant());
        assertThat(caretaker.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("accumulates ALL violations at once instead of stopping at the first")
    void accumulatesEveryViolationAtOnce() {
        assertThat(of(() -> register("", "12345678901", "sem-arroba", "91991234567", "AviarioSul2026")))
                .containsKeys("fullName", "cpf", "email");
    }

    @Test
    @DisplayName("never stores the raw password")
    void neverStoresTheRawPassword() {
        Caretaker caretaker = validCaretaker();

        assertThat(caretaker.passwordHash().value()).doesNotContain("AviarioSul2026");
        assertThat(caretaker.passwordHash().value()).startsWith("{");
    }

    @Test
    @DisplayName("rejects a password outside the policy along with the other violations")
    void rejectsPasswordOutsidePolicy() {
        assertThat(of(() -> register(
                        "João Pereira de Souza", "52998224725", "joao@ovyx.com.br", "91991234567", "curta")))
                .hasEntrySatisfying("password", message -> assertThat(message).contains("ao menos 12 caracteres"));
    }

    @Test
    @DisplayName("authenticates with the correct password")
    void authenticatesWithCorrectPassword() {
        Caretaker caretaker = validCaretaker();

        assertThat(caretaker.authenticate("AviarioSul2026", hasher)).isTrue();
        assertThat(caretaker.authenticate("SenhaErrada2026", hasher)).isFalse();
    }

    @Test
    @DisplayName("an inactive caretaker never authenticates, even with the correct password")
    void inactiveCaretakerNeverAuthenticates() {
        Caretaker caretaker = validCaretaker();
        caretaker.deactivate(clock);

        assertThat(caretaker.status()).isEqualTo(CaretakerStatus.INACTIVE);
        assertThat(caretaker.authenticate("AviarioSul2026", hasher)).isFalse();
    }

    @Test
    @DisplayName("reactivation restores authentication")
    void reactivationRestoresAuthentication() {
        Caretaker caretaker = validCaretaker();
        caretaker.deactivate(clock);

        caretaker.reactivate(clock);

        assertThat(caretaker.status()).isEqualTo(CaretakerStatus.ACTIVE);
        assertThat(caretaker.authenticate("AviarioSul2026", hasher)).isTrue();
    }

    @Test
    @DisplayName("changes the own password when the current one matches")
    void changesOwnPassword() {
        Caretaker caretaker = validCaretaker();

        caretaker.changeOwnPassword("AviarioSul2026", "PosturaAviario2027", hasher, clock);

        assertThat(caretaker.authenticate("AviarioSul2026", hasher)).isFalse();
        assertThat(caretaker.authenticate("PosturaAviario2027", hasher)).isTrue();
    }

    @Test
    @DisplayName("refuses the change when the current password is wrong and keeps the password")
    void refusesChangeWhenCurrentPasswordIsWrong() {
        Caretaker caretaker = validCaretaker();

        assertThat(of(() -> caretaker.changeOwnPassword("SenhaErrada2026", "PosturaAviario2027", hasher, clock)))
                .hasEntrySatisfying(
                        "currentPassword", message -> assertThat(message).contains("senha atual está incorreta"));
        assertThat(caretaker.authenticate("AviarioSul2026", hasher)).isTrue();
    }

    @Test
    @DisplayName("refuses a new password outside the policy and returns every violation")
    void refusesNewPasswordOutsidePolicy() {
        Caretaker caretaker = validCaretaker();

        assertThat(of(() -> caretaker.changeOwnPassword("AviarioSul2026", "abc", hasher, clock)))
                .hasEntrySatisfying("newPassword", message -> assertThat(message)
                        .contains("ao menos 12 caracteres")
                        .contains("ao menos um dígito"));
        assertThat(caretaker.authenticate("AviarioSul2026", hasher)).isTrue();
    }

    @Test
    @DisplayName("requires the current password and reports it together with the new password violations")
    void requiresCurrentPasswordAlongWithOtherViolations() {
        // Antes, a borda HTTP exigia a senha atual e o dominio cuidava da nova: quem errava as duas
        // coisas recebia uma violacao, corrigia, e so entao descobria a outra (A1).
        Caretaker caretaker = validCaretaker();

        assertThat(of(() -> caretaker.changeOwnPassword("", "abc", hasher, clock)))
                .hasEntrySatisfying("currentPassword", message -> assertThat(message)
                        .contains("Informe a senha atual")
                        .doesNotContain("incorreta"))
                .hasEntrySatisfying("newPassword", message -> assertThat(message)
                        .contains("ao menos 12 caracteres")
                        .contains("ao menos um dígito"));
    }

    @Test
    @DisplayName("changing the password clears the obligation to change it")
    void changingPasswordClearsTheObligation() {
        // Cenario 7 da Historia 1: o administrador semeado troca a senha provisoria e so entao
        // pode usar o sistema.
        Caretaker seeded = Caretaker.register(
                "Administrador do Sistema",
                "52998224725",
                "admin@ovyx.com.br",
                "91991234567",
                "TrocarNoPrimeiroAcesso2026",
                Role.ADMINISTRATOR,
                true,
                hasher,
                clock);

        assertThat(seeded.mustChangePassword()).isTrue();

        seeded.changeOwnPassword("TrocarNoPrimeiroAcesso2026", "PosturaAviario2027", hasher, clock);

        assertThat(seeded.mustChangePassword()).isFalse();
    }

    @Test
    @DisplayName("requires a role and reports it together with the other violations")
    void requiresRole() {
        assertThat(of(() -> Caretaker.register(
                        "",
                        "52998224725",
                        "joao.pereira@ovyx.com.br",
                        "91991234567",
                        "AviarioSul2026",
                        null,
                        false,
                        hasher,
                        clock)))
                .containsEntry("role", "Informe o perfil.")
                .containsEntry("fullName", "Informe o nome completo.");
    }

    @Test
    @DisplayName("two loads of the same caretaker are the same aggregate")
    void identityIsByIdentifier() {
        // Igualdade por identificador, e nao por campo: e o que AggregateRoot garante.
        Caretaker caretaker = validCaretaker();
        Caretaker reloaded = Caretaker.restore(
                caretaker.id(),
                caretaker.fullName(),
                caretaker.cpf(),
                caretaker.email(),
                caretaker.mobilePhone(),
                caretaker.passwordHash(),
                caretaker.role(),
                caretaker.status(),
                caretaker.mustChangePassword(),
                caretaker.createdAt(),
                caretaker.updatedAt());

        assertThat(reloaded).isEqualTo(caretaker).hasSameHashCodeAs(caretaker);
    }
}
