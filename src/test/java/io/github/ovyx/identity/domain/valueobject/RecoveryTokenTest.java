package io.github.ovyx.identity.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.domain.DomainException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * O código do link de recuperação (R-002 da 012): 43 caracteres de Base64 URL, guardado só pelo resumo SHA-256.
 */
@DisplayName("RecoveryToken")
class RecoveryTokenTest {

    private static final String RAW = "3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx";

    @Test
    @DisplayName("keeps a code of 43 Base64 URL characters")
    void givenCodeOf43Base64UrlCharacters_whenReading_thenKeepIt() {
        // given
        String raw = RAW;

        // when
        RecoveryToken token = RecoveryToken.of(raw);

        // then
        assertThat(token.value()).isEqualTo(RAW);
    }

    @ParameterizedTest(name = "[{0}]")
    @NullSource
    @ValueSource(strings = {
        "",
        "3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iK",
        "3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKxy",
        "3q2+7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx",
        "3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iK=",
        " 3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iK"
    })
    @DisplayName("treats a missing, short, long or foreign code as an invalid link")
    void givenCodeOutsideTheFormat_whenReading_thenRefuseAsInvalidLink(String raw) {
        // given — o código fora do formato, do @NullSource e do @ValueSource

        // when / then
        assertThatThrownBy(() -> RecoveryToken.of(raw))
                .isInstanceOfSatisfying(DomainException.class, refusal ->
                        assertThat(refusal.errorCode()).isEqualTo(IdentityErrorCode.RECOVERY_LINK_INVALID));
    }

    @Test
    @DisplayName("digests the code with SHA-256, in 64 lowercase hexadecimal characters")
    void givenCode_whenDigesting_thenGiveItsSha256InLowercaseHex() throws NoSuchAlgorithmException {
        // given
        RecoveryToken token = RecoveryToken.of(RAW);
        String expected = HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(RAW.getBytes(StandardCharsets.US_ASCII)));

        // when
        String hash = token.hash();

        // then
        assertThat(hash).isEqualTo(expected).hasSize(64).isLowerCase();
    }

    @Test
    @DisplayName("never shows the code in its text form")
    void givenToken_whenPrinting_thenHideTheCode() {
        // given
        RecoveryToken token = RecoveryToken.of(RAW);

        // when
        String printed = token.toString();

        // then
        assertThat(printed).doesNotContain(RAW).doesNotContain(RAW.substring(0, 6));
    }
}
