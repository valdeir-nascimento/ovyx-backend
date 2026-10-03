package io.github.ovyx.identity.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * O remetente e o endereço da aplicação dos e-mails (R-006 da 012): sem eles a aplicação não sobe, e a mensagem diz
 * qual falta (FR-019).
 */
@DisplayName("OvyxMailProperties")
class OvyxMailPropertiesTest {

    @ParameterizedTest(name = "[{0}]")
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("refuses to start without the sender, naming the property")
    void givenMissingSender_whenBinding_thenRefuseNamingTheProperty(String from) {
        // given — o remetente ausente, do @NullSource e do @ValueSource

        // when / then
        assertThatThrownBy(() -> new OvyxMailProperties(from, "https://ovyx.granja.com.br"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ovyx.mail.from")
                .hasMessageContaining("OVYX_MAIL_FROM");
    }

    @ParameterizedTest(name = "[{0}]")
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("refuses to start without the address of the application, naming the property")
    void givenMissingAppUrl_whenBinding_thenRefuseNamingTheProperty(String appUrl) {
        // given — o endereço ausente, do @NullSource e do @ValueSource

        // when / then
        assertThatThrownBy(() -> new OvyxMailProperties("ovyx@granja.com.br", appUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ovyx.mail.app-url")
                .hasMessageContaining("OVYX_APP_URL");
    }

    @Test
    @DisplayName("drops the trailing slashes and spaces of the address, for the link to have a single slash")
    void givenAppUrlWithTrailingSlash_whenBinding_thenDropIt() {
        // given
        String appUrl = " https://ovyx.granja.com.br// ";

        // when
        OvyxMailProperties properties = new OvyxMailProperties("ovyx@granja.com.br", appUrl);

        // then
        assertThat(properties.appUrl()).isEqualTo("https://ovyx.granja.com.br");
    }
}
