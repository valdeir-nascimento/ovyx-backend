package io.github.ovyx.identity.infrastructure.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Garante que a configuracao do administrador inicial nao expoe a senha em {@code toString()}
 * (FR-021): ela e o unico portador da senha provisoria fora do caso de uso de semeadura.
 */
@DisplayName("BootstrapAdministratorProperties")
class BootstrapAdministratorPropertiesTest {

    @Test
    @DisplayName("masks the password and keeps the other fields")
    void masksThePassword() {
        String text = new BootstrapAdministratorProperties(
                        "Administrador do Sistema", "52998224725", "admin@ovyx.com.br", "11999999999",
                        "ProvisoriaGranja2026")
                .toString();

        assertThat(text)
                .doesNotContain("ProvisoriaGranja2026")
                .contains("password=****")
                .contains("admin@ovyx.com.br");
    }
}
