package io.github.ovyx.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.identity.domain.valueobject.Email;
import io.github.ovyx.shared.domain.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes do Notification do dominio (FR-017).
 *
 * <p>Ele existe para que a pessoa veja todos os campos errados de uma vez; se recusasse no primeiro,
 * o requisito estaria quebrado e nenhum outro teste perceberia.
 */
@DisplayName("Notification")
class NotificationTest {

    @Test
    @DisplayName("keeps quiet while nothing was violated")
    void keepsQuietWithoutErrors() {
        Notification notification = new Notification();

        notification.throwIfAny();

        assertThat(notification.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("refuses once, carrying every field that was violated")
    void refusesOnceWithEveryField() {
        Notification notification = new Notification();
        notification.add("email", "Informe o e-mail.");
        notification.add("cpf", "CPF inválido.");

        assertThatThrownBy(notification::throwIfAny)
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).details())
                        .containsEntry("email", "Informe o e-mail.")
                        .containsEntry("cpf", "CPF inválido."));
    }

    @Test
    @DisplayName("joins two violations of the same field instead of dropping one")
    void joinsErrorsOfTheSameField() {
        Notification notification = new Notification();
        notification.add("password", "A senha deve ter ao menos 12 caracteres.");
        notification.add("password", "A senha deve conter ao menos um dígito.");

        assertThat(notification.errors())
                .containsEntry(
                        "password",
                        "A senha deve ter ao menos 12 caracteres. A senha deve conter ao menos um dígito.");
    }

    @Test
    @DisplayName("uses the code that tells the client it was a validation failure")
    void usesTheValidationCode() {
        Notification notification = new Notification();
        notification.add("email", "Informe o e-mail.");

        assertThatThrownBy(notification::throwIfAny)
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).errorCode())
                        .isEqualTo(IdentityErrorCode.VALIDATION_FAILED));
    }

    @Test
    @DisplayName("receives what a value object refused, without exception as control flow")
    void receivesWhatAValueObjectRefused() {
        // O agregado nao precisa capturar excecao campo a campo: o objeto de valor escreve aqui e
        // devolve nulo, e a recusa acontece uma vez so, no fim.
        Notification notification = new Notification();

        Email email = Email.of("   ", notification);

        assertThat(email).isNull();
        assertThat(notification.errors()).containsEntry("email", "Informe o e-mail.");
    }

    @Test
    @DisplayName("refuses a single field on the spot, for whoever has nothing to accumulate")
    void refusesASingleFieldOnTheSpot() {
        DomainException refusal = Notification.rejecting("role", "Informe o perfil.");

        assertThat(refusal.details()).containsEntry("role", "Informe o perfil.");
        assertThat(refusal.errorCode()).isEqualTo(IdentityErrorCode.VALIDATION_FAILED);
    }
}
