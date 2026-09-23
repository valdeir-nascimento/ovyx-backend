package io.github.ovyx.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.shared.domain.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes do acumulador de violacoes (FR-017).
 *
 * <p>Ele existe para que a pessoa veja todos os campos errados de uma vez; se recusasse no primeiro,
 * o requisito estaria quebrado e nenhum outro teste perceberia.
 */
@DisplayName("Violations")
class ViolationsTest {

    @Test
    @DisplayName("keeps quiet while nothing was violated")
    void keepsQuietWithoutViolations() {
        Violations violations = new Violations();

        violations.throwIfAny();

        assertThat(violations.hasAny()).isFalse();
    }

    @Test
    @DisplayName("refuses once, carrying every field that was violated")
    void refusesOnceWithEveryField() {
        Violations violations = new Violations();
        violations.add("email", "Informe o e-mail.");
        violations.add("cpf", "CPF inválido.");

        assertThatThrownBy(violations::throwIfAny)
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).details())
                        .containsEntry("email", "Informe o e-mail.")
                        .containsEntry("cpf", "CPF inválido."));
    }

    @Test
    @DisplayName("joins two violations of the same field instead of dropping one")
    void joinsViolationsOfTheSameField() {
        Violations violations = new Violations();
        violations.add("password", "A senha deve ter ao menos 12 caracteres.");
        violations.add("password", "A senha deve conter ao menos um dígito.");

        assertThat(violations.details().get("password"))
                .contains("ao menos 12 caracteres")
                .contains("ao menos um dígito");
    }

    @Test
    @DisplayName("collects what a value object refused and keeps going")
    void collectsFromValueObjectFactory() {
        Violations violations = new Violations();

        String collected = violations.collect(() -> {
            throw Violations.of("fullName", "Informe o nome completo.");
        });

        assertThat(collected).isNull();
        assertThat(violations.details()).containsEntry("fullName", "Informe o nome completo.");
    }

    @Test
    @DisplayName("uses the code that tells the client it was a validation failure")
    void usesTheValidationCode() {
        Violations violations = new Violations();
        violations.add("role", "Informe o perfil.");

        assertThatThrownBy(violations::throwIfAny)
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).errorCode())
                        .isEqualTo(IdentityErrorCode.VALIDATION_FAILED));
    }
}
