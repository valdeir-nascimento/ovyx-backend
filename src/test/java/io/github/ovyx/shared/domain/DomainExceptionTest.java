package io.github.ovyx.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Testes da recusa montada a mao, campo a campo, sem passar pelo {@link Notification}.
 *
 * <p>O {@code details} chega ao usuario final, na ordem do formulario; os dois caminhos de recusa
 * precisam preserva-la.
 */
@DisplayName("DomainException")
class DomainExceptionTest {

    private static final ErrorCode CODE = () -> "RULE_VIOLATED";

    @Test
    @DisplayName("keeps the field order of a refusal built from a map")
    void givenDetailsInFormOrder_whenRefusingWithAMap_thenKeepThatOrder() {
        // given
        Map<String, String> inFormOrder = new LinkedHashMap<>();
        inFormOrder.put("fullName", "Informe o nome completo.");
        inFormOrder.put("cpf", "Informe o CPF.");
        inFormOrder.put("email", "Informe o e-mail.");
        inFormOrder.put("mobilePhone", "Informe o celular.");
        inFormOrder.put("password", "Informe a senha.");
        inFormOrder.put("role", "Informe o perfil.");

        // when
        DomainException refusal = new DomainException(CODE, "Dados inválidos.", inFormOrder);

        // then
        assertThat(refusal.details()).containsExactlyEntriesOf(inFormOrder);
    }

    @ParameterizedTest(name = "field [{0}], message [{1}]")
    @CsvSource(value = {"NULL, Informe o e-mail.", "email, NULL"}, nullValues = "NULL")
    @DisplayName("refuses a null field or message in a refusal built from a map")
    void givenDetailsWithANullFieldOrMessage_whenRefusingWithAMap_thenThrowNullPointerException(
            String field, String message) {
        // given
        Map<String, String> withANull = new LinkedHashMap<>();
        withANull.put(field, message);

        // when
        ThrowingCallable refusing = () -> new DomainException(CODE, "Dados inválidos.", withANull);

        // then
        assertThatThrownBy(refusing).isInstanceOf(NullPointerException.class);
    }
}
