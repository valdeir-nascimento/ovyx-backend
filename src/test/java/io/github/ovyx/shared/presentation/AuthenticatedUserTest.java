package io.github.ovyx.shared.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes da identidade guardada na sessao.
 *
 * <p>Ela e serializada pelo Spring Session e aparece em log: o que ela carrega, e o que ela deixa de
 * carregar, e contrato.
 */
@DisplayName("AuthenticatedUser")
class AuthenticatedUserTest {

    private static final UUID ID = UUID.fromString("6f1b4c7e-9a2d-4f3b-8c1e-2d5a7b9c0e11");

    private static AuthenticatedUser administrator(boolean mustChangePassword) {
        return new AuthenticatedUser(ID, "Maria Silva", "ADMINISTRATOR", mustChangePassword);
    }

    @Test
    @DisplayName("Exposes the role as the authority the security chain expects")
    void givenAdministrator_whenReadingTheAuthority_thenPrefixItWithRole() {
        // given
        AuthenticatedUser user = administrator(false);

        // when
        String authority = user.authority();

        // then
        assertThat(authority).isEqualTo("ROLE_ADMINISTRATOR");
    }

    @Test
    @DisplayName("Clears the pending password change without touching the rest of the identity")
    void givenPendingPasswordChange_whenClearingIt_thenKeepIdentifierNameAndRole() {
        // given
        AuthenticatedUser pending = administrator(true);

        // when
        AuthenticatedUser changed = pending.withPasswordChanged();

        // then
        assertThat(changed.mustChangePassword()).isFalse();
        assertThat(changed).isEqualTo(new AuthenticatedUser(ID, "Maria Silva", "ADMINISTRATOR", false));
    }

    @Test
    @DisplayName("Never writes the person's name in the log")
    void givenIdentityOfAPerson_whenWritingItToTheLog_thenOmitTheName() {
        // given
        AuthenticatedUser user = administrator(false);

        // when
        String text = user.toString();

        // then
        assertThat(text).doesNotContain("Maria Silva").contains(ID.toString()).contains("ADMINISTRATOR");
    }
}
