package io.github.ovyx.identity.domain.model;

import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalCodeOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.DEFAULT_PASSWORD;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.identity.fixtures.InMemoryCaretakerRepository;
import io.github.ovyx.identity.domain.FakePasswordHasher;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.domain.FixedClock;
import io.github.ovyx.shared.domain.Violation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes do agregado {@code Caretaker} — sem mock de dominio, conforme o principio VI.
 *
 * <p>As invariantes verificadas aqui sao as de data-model.md que dependem so do proprio
 * responsavel. As que consultam os demais — unicidade do CPF, do e-mail e do celular, e o ultimo
 * administrador ativo — estao em {@link CaretakerAdministrationTest}.
 */
@DisplayName("Caretaker")
class CaretakerTest {

    private static final String NEW_PASSWORD = "PosturaAviario2027";

    private final PasswordHasher hasher = new FakePasswordHasher();
    private final FixedClock clock = FixedClock.at("2026-09-19T12:00:00Z");

    private CaretakerTestDataBuilder aValidCaretaker() {
        return aCaretaker().withHasher(hasher).withClock(clock);
    }

    @Test
    @DisplayName("is born active with identity and timestamps")
    void givenValidData_whenRegistering_thenBeBornActiveWithIdentityAndTimestamps() {
        // given
        CaretakerTestDataBuilder valid = aValidCaretaker();

        // when
        Caretaker caretaker = valid.build();

        // then
        assertThat(caretaker.id()).isNotNull();
        assertThat(caretaker.status()).isEqualTo(CaretakerStatus.ACTIVE);
        assertThat(caretaker.role()).isEqualTo(Role.USER);
        assertThat(caretaker.mustChangePassword()).isFalse();
        assertThat(caretaker.createdAt()).isEqualTo(clock.instant());
        assertThat(caretaker.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("accumulates ALL violations at once instead of stopping at the first")
    void givenThreeInvalidFields_whenRegistering_thenReportAllThreeAtOnce() {
        // given
        CaretakerTestDataBuilder threeInvalidFields =
                aValidCaretaker().withFullName("").withCpf("12345678901").withEmail("sem-arroba");

        // when
        List<Violation> violations = violationsOf(threeInvalidFields::build);

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(
                        tuple("fullName", IdentityErrorCode.FULL_NAME_REQUIRED),
                        tuple("cpf", IdentityErrorCode.CPF_INVALID),
                        tuple("email", IdentityErrorCode.EMAIL_MALFORMED));
    }

    @Test
    @DisplayName("never stores the raw password")
    void givenRawPassword_whenRegistering_thenStoreOnlyAPrefixedHash() {
        // given
        CaretakerTestDataBuilder withRawPassword = aValidCaretaker().withPassword(DEFAULT_PASSWORD);

        // when
        Caretaker caretaker = withRawPassword.build();

        // then
        assertThat(caretaker.passwordHash().value()).doesNotContain(DEFAULT_PASSWORD).startsWith("{");
    }

    @Test
    @DisplayName("rejects a password outside the policy along with the other violations")
    void givenPasswordOutsidePolicy_whenRegistering_thenRejectThePassword() {
        // given
        CaretakerTestDataBuilder withShortPassword = aValidCaretaker().withPassword("curta");

        // when
        List<Violation> violations = violationsOf(withShortPassword::build);

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .contains(tuple("password", IdentityErrorCode.PASSWORD_TOO_SHORT));
    }

    @Test
    @DisplayName("authenticates with the correct password")
    void givenCorrectPassword_whenAuthenticating_thenAccept() {
        // given
        Caretaker caretaker = aValidCaretaker().build();

        // when
        boolean authenticated = caretaker.authenticate(DEFAULT_PASSWORD, hasher);

        // then
        assertThat(authenticated).isTrue();
    }

    @Test
    @DisplayName("does not authenticate with a wrong password")
    void givenWrongPassword_whenAuthenticating_thenRefuse() {
        // given
        Caretaker caretaker = aValidCaretaker().build();

        // when
        boolean authenticated = caretaker.authenticate("SenhaErrada2026", hasher);

        // then
        assertThat(authenticated).isFalse();
    }

    @Test
    @DisplayName("an inactive caretaker never authenticates, even with the correct password")
    void givenInactiveCaretaker_whenAuthenticatingWithCorrectPassword_thenRefuse() {
        // given
        Caretaker caretaker = aValidCaretaker().build();
        caretaker.deactivate(new InMemoryCaretakerRepository(), clock);

        // when
        boolean authenticated = caretaker.authenticate(DEFAULT_PASSWORD, hasher);

        // then
        assertThat(caretaker.status()).isEqualTo(CaretakerStatus.INACTIVE);
        assertThat(authenticated).isFalse();
    }

    @Test
    @DisplayName("reactivation restores authentication")
    void givenDeactivatedCaretaker_whenReactivating_thenAuthenticateAgain() {
        // given
        Caretaker caretaker = aValidCaretaker().build();
        caretaker.deactivate(new InMemoryCaretakerRepository(), clock);

        // when
        caretaker.reactivate(new InMemoryCaretakerRepository(), clock);

        // then
        assertThat(caretaker.status()).isEqualTo(CaretakerStatus.ACTIVE);
        assertThat(caretaker.authenticate(DEFAULT_PASSWORD, hasher)).isTrue();
    }

    @Test
    @DisplayName("changes the own password when the current one matches")
    void givenMatchingCurrentPassword_whenChangingOwnPassword_thenAcceptOnlyTheNewOne() {
        // given
        Caretaker caretaker = aValidCaretaker().build();

        // when
        caretaker.changeOwnPassword(DEFAULT_PASSWORD, NEW_PASSWORD, hasher, clock);

        // then
        assertThat(caretaker.authenticate(DEFAULT_PASSWORD, hasher)).isFalse();
        assertThat(caretaker.authenticate(NEW_PASSWORD, hasher)).isTrue();
    }

    @Test
    @DisplayName("refuses the change of an inactive caretaker as unavailable and keeps the password")
    void givenInactiveCaretaker_whenChangingOwnPassword_thenRefuseAsUnavailableAndKeepThePassword() {
        // given
        // A sessao pode sobreviver a inativacao. A recusa mora no agregado (principio II, T179):
        // nenhum outro chamador precisa lembrar de conferir a situacao antes.
        Caretaker caretaker = aValidCaretaker().build();
        caretaker.deactivate(new InMemoryCaretakerRepository(), clock);

        // when
        var refusal = refusalCodeOf(() -> caretaker.changeOwnPassword(DEFAULT_PASSWORD, NEW_PASSWORD, hasher, clock));

        // then
        assertThat(refusal).isEqualTo(IdentityErrorCode.CARETAKER_UNAVAILABLE);
        caretaker.reactivate(new InMemoryCaretakerRepository(), clock);
        assertThat(caretaker.authenticate(DEFAULT_PASSWORD, hasher))
                .as("a senha anterior continua valendo")
                .isTrue();
    }

    @Test
    @DisplayName("refuses an inactive caretaker before looking at what was typed")
    void givenInactiveCaretakerWithInvalidPasswords_whenChangingOwnPassword_thenRefuseAsUnavailableFirst() {
        // given
        // A recusa por situacao vem antes da de preenchimento: o inativo nao fica sabendo nem se a
        // senha atual confere, nem o que falta na nova.
        Caretaker caretaker = aValidCaretaker().build();
        caretaker.deactivate(new InMemoryCaretakerRepository(), clock);

        // when
        var refusal = refusalCodeOf(() -> caretaker.changeOwnPassword("SenhaErrada2026", "abc", hasher, clock));

        // then
        assertThat(refusal).isEqualTo(IdentityErrorCode.CARETAKER_UNAVAILABLE);
    }

    @Test
    @DisplayName("refuses the change when the current password is wrong and keeps the password")
    void givenWrongCurrentPassword_whenChangingOwnPassword_thenRefuseAndKeepTheOldPassword() {
        // given
        Caretaker caretaker = aValidCaretaker().build();

        // when
        List<Violation> violations =
                violationsOf(() -> caretaker.changeOwnPassword("SenhaErrada2026", NEW_PASSWORD, hasher, clock));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(tuple("currentPassword", IdentityErrorCode.CURRENT_PASSWORD_INCORRECT));
        assertThat(caretaker.authenticate(DEFAULT_PASSWORD, hasher)).isTrue();
    }

    @Test
    @DisplayName("refuses a new password outside the policy and returns every violation")
    void givenNewPasswordOutsidePolicy_whenChangingOwnPassword_thenRefuseWithEveryViolationAndKeepTheOldPassword() {
        // given
        Caretaker caretaker = aValidCaretaker().build();

        // when
        List<Violation> violations =
                violationsOf(() -> caretaker.changeOwnPassword(DEFAULT_PASSWORD, "abc", hasher, clock));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(
                        tuple("newPassword", IdentityErrorCode.PASSWORD_TOO_SHORT),
                        tuple("newPassword", IdentityErrorCode.PASSWORD_WITHOUT_DIGIT));
        assertThat(caretaker.authenticate(DEFAULT_PASSWORD, hasher)).isTrue();
    }

    @Test
    @DisplayName("requires the current password and reports it together with the new password violations")
    void givenMissingCurrentAndInvalidNewPassword_whenChangingOwnPassword_thenReportBothFieldsTogether() {
        // given
        // Antes, a borda HTTP exigia a senha atual e o dominio cuidava da nova: quem errava as duas
        // coisas recebia uma violacao, corrigia, e so entao descobria a outra (A1).
        Caretaker caretaker = aValidCaretaker().build();

        // when
        List<Violation> violations = violationsOf(() -> caretaker.changeOwnPassword("", "abc", hasher, clock));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(
                        tuple("currentPassword", IdentityErrorCode.CURRENT_PASSWORD_REQUIRED),
                        tuple("newPassword", IdentityErrorCode.PASSWORD_TOO_SHORT),
                        tuple("newPassword", IdentityErrorCode.PASSWORD_WITHOUT_DIGIT));
    }

    @Test
    @DisplayName("changing the password clears the obligation to change it")
    void givenSeededAdministratorWithProvisionalPassword_whenChangingOwnPassword_thenClearTheObligation() {
        // given
        // Cenario 7 da Historia 1: o administrador semeado troca a senha provisoria e so entao
        // pode usar o sistema.
        Caretaker seeded = aValidCaretaker()
                .withRole(Role.ADMINISTRATOR)
                .withPendingPasswordChange()
                .build();
        assertThat(seeded.mustChangePassword())
                .as("precondition: seeded with a provisional password")
                .isTrue();

        // when
        seeded.changeOwnPassword(DEFAULT_PASSWORD, NEW_PASSWORD, hasher, clock);

        // then
        assertThat(seeded.mustChangePassword()).isFalse();
    }

    @Test
    @DisplayName("requires a role and reports it together with the other violations")
    void givenMissingRoleAndName_whenRegistering_thenReportBothTogether() {
        // given
        CaretakerTestDataBuilder withoutRoleAndName = aValidCaretaker().withRole(null).withFullName("");

        // when
        List<Violation> violations = violationsOf(withoutRoleAndName::build);

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(
                        tuple("fullName", IdentityErrorCode.FULL_NAME_REQUIRED),
                        tuple("role", IdentityErrorCode.ROLE_REQUIRED));
    }

    @ParameterizedTest(name = "refuses \"{0}\"")
    @ValueSource(strings = {"admin", "ROOT", "administrator"})
    @DisplayName("refuses a role outside the list and reports it together with the other violations")
    void givenRoleOutsideTheListAndMissingName_whenRegistering_thenReportBothTogether(String unknownRole) {
        // given
        // Convertido em enum na borda, o perfil desconhecido tornava o corpo inteiro ilegivel e
        // escondia as demais violacoes (FR-017). So o nome exato vale, como o contrato publica.
        CaretakerTestDataBuilder withUnknownRoleAndNoName =
                aValidCaretaker().withRoleNamed(unknownRole).withFullName("");

        // when
        List<Violation> violations = violationsOf(withUnknownRoleAndNoName::build);

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(
                        tuple("fullName", IdentityErrorCode.FULL_NAME_REQUIRED),
                        tuple("role", IdentityErrorCode.ROLE_INVALID));
    }

    @Test
    @DisplayName("the refusal the API publishes carries a Portuguese message per field")
    void givenMissingRoleAndName_whenRegistering_thenPublishAPortugueseMessagePerField() {
        // given
        // Os codigos sao para quem reage a recusa; o texto e o que a pessoa le na tela (principio VII).
        CaretakerTestDataBuilder withoutRoleAndName = aValidCaretaker().withRole(null).withFullName("");

        // when
        Map<String, String> details = detailsOf(withoutRoleAndName::build);

        // then
        assertThat(details)
                .containsEntry("role", "Informe o perfil.")
                .containsEntry("fullName", "Informe o nome completo.");
    }

    @Test
    @DisplayName("two loads of the same caretaker are the same aggregate")
    void givenTheSameCaretakerLoadedTwice_whenComparing_thenTreatThemAsTheSameAggregate() {
        // given
        // Igualdade por identificador, e nao por campo: e o que AggregateRoot garante.
        Caretaker caretaker = aValidCaretaker().build();

        // when
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

        // then
        assertThat(reloaded).isEqualTo(caretaker).hasSameHashCodeAs(caretaker);
    }
}
