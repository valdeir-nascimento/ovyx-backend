package io.github.ovyx.identity.domain.model;

import static io.github.ovyx.identity.domain.DomainViolations.refusalCodeOf;
import static io.github.ovyx.identity.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.domain.ErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Violation;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes do perfil recebido como texto (FR-007, FR-017).
 *
 * <p>Convertido em enum na borda, um perfil desconhecido tornava o corpo inteiro ilegivel e escondia
 * as demais violacoes. Agora ele chega ao dominio como foi digitado, e e aqui que se decide o que e
 * ausente e o que e invalido — a diferenca muda a mensagem que a pessoa le.
 */
@DisplayName("Role")
class RoleTest {

    private static List<ErrorCode> codesOf(String raw) {
        Notification notification = new Notification();
        Role.validate(raw, notification);
        return notification.violations().stream().map(Violation::code).toList();
    }

    @ParameterizedTest(name = "[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("treats a missing or blank role as missing, and not as outside the list")
    void givenMissingOrBlankRole_whenValidating_thenReportItAsRequired(String raw) {
        // given — perfil ausente ou em branco, vindo do @NullAndEmptySource e do @ValueSource

        // when
        List<ErrorCode> codes = codesOf(raw);

        // then
        assertThat(codes).containsExactly(IdentityErrorCode.ROLE_REQUIRED);
    }

    @ParameterizedTest(name = "[{0}]")
    @ValueSource(strings = {"user", " USER", "ADMIN", "administrator", "ROOT"})
    @DisplayName("refuses anything but the exact name the contract publishes")
    void givenRoleOutsideTheList_whenValidating_thenReportItAsInvalid(String raw) {
        // given — perfil fora da lista, vindo do @ValueSource

        // when
        List<ErrorCode> codes = codesOf(raw);

        // then
        assertThat(codes).containsExactly(IdentityErrorCode.ROLE_INVALID);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Role.class)
    @DisplayName("accepts the exact name of each role")
    void givenTheExactNameOfARole_whenValidating_thenReportNothing(Role role) {
        // given — cada perfil do enum, pelo nome exato

        // when
        List<ErrorCode> codes = codesOf(role.name());

        // then
        assertThat(codes).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Role.class)
    @DisplayName("creates the role with that exact name")
    void givenTheExactNameOfARole_whenCreating_thenReturnThatRole(Role role) {
        // given — cada perfil do enum, pelo nome exato

        // when
        Role created = Role.of(role.name());

        // then
        assertThat(created).isEqualTo(role);
    }

    @Test
    @DisplayName("refuses to create a role outside the list as a field violation, and not as an unexpected error")
    void givenRoleOutsideTheList_whenCreating_thenRefuseWithTheFieldViolation() {
        // given
        // Sem a recusa, o valueOf do enum lancava IllegalArgumentException, que viraria 500 (principio
        // III): a recusa do dominio e sempre DomainException, com o codigo da regra.
        String unknown = "ROOT";

        // when
        List<Violation> violations = violationsOf(() -> Role.of(unknown));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(tuple("role", IdentityErrorCode.ROLE_INVALID));
        assertThat(refusalCodeOf(() -> Role.of(unknown))).isEqualTo(IdentityErrorCode.VALIDATION_FAILED);
    }
}
