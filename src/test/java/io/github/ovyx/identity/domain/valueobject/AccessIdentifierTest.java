package io.github.ovyx.identity.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes da forma canonica do identificador de acesso.
 *
 * <p>A forma canonica e a mesma chave usada para localizar a conta e para contar tentativas. Se as
 * duas divergirem, cada grafia diferente do mesmo identificador vira uma chave de contencao nova, e
 * as tentativas contra uma unica conta ficam ilimitadas (FR-023).
 */
@DisplayName("AccessIdentifier")
class AccessIdentifierTest {

    @Test
    @DisplayName("normalizes an email to trimmed lowercase")
    void normalizesEmail() {
        assertThat(AccessIdentifier.of("  Maria.Silva@OVYX.com.br ").value()).isEqualTo("maria.silva@ovyx.com.br");
    }

    @ParameterizedTest
    @ValueSource(strings = {"91988887777", "(91) 98888-7777", " 91 98888 7777 ", "91.98888.7777"})
    @DisplayName("reduces every formatting of the same mobile phone to the same digits")
    void reducesMobilePhoneFormattingToDigits(String typed) {
        assertThat(AccessIdentifier.of(typed).value()).isEqualTo("91988887777");
    }

    @Test
    @DisplayName("keeps letters instead of discarding them, so a mistyped identifier matches nobody")
    void keepsLetters() {
        // Descartar letras transformaria x91988887777 no celular de alguem. Mantidas, a chave nao
        // corresponde a conta nenhuma, que e o resultado correto para um identificador digitado errado.
        assertThat(AccessIdentifier.of("x91988887777").value()).isEqualTo("x91988887777");
    }

    @Test
    @DisplayName("treats anything containing an at sign as an email")
    void treatsAtSignAsEmail() {
        assertThat(AccessIdentifier.of("91988887777@qualquer.com").value()).isEqualTo("91988887777@qualquer.com");
    }

    @Test
    @DisplayName("produces an empty key for a missing identifier")
    void producesEmptyKeyForMissingIdentifier() {
        assertThat(AccessIdentifier.of(null).value()).isEmpty();
        assertThat(AccessIdentifier.of("   ").value()).isEmpty();
    }

    @Test
    @DisplayName("has value equality between different spellings of the same account")
    void hasValueEquality() {
        assertThat(AccessIdentifier.of("(91) 98888-7777")).isEqualTo(AccessIdentifier.of("91988887777"));
        assertThat(AccessIdentifier.of("MARIA@ovyx.com.br")).isEqualTo(AccessIdentifier.of("maria@ovyx.com.br"));
    }
}
