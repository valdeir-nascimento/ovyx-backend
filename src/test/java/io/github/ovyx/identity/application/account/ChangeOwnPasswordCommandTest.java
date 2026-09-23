package io.github.ovyx.identity.application.account;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.domain.model.CaretakerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Garante que o comando de troca de senha nao expoe nenhuma das duas senhas em {@code toString()}
 * (FR-021), como o {@code toString()} gerado para records faria.
 */
@DisplayName("ChangeOwnPasswordCommand")
class ChangeOwnPasswordCommandTest {

    @Test
    @DisplayName("masks both passwords")
    void masksBothPasswords() {
        String text = new ChangeOwnPasswordCommand(CaretakerId.generate(), "GranjaNorte2026", "PosturaAviario2027")
                .toString();

        assertThat(text).doesNotContain("GranjaNorte2026").doesNotContain("PosturaAviario2027");
    }
}
