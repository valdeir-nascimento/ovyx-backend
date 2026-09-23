package io.github.ovyx.shared.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import tools.jackson.databind.json.JsonMapper;

/**
 * Testes da leitura de JSON sem eco do conteudo nas mensagens de erro (FR-021).
 *
 * <p>O parser do Jackson poe na mensagem o trecho que nao conseguiu ler, e o Spring MVC registra essa
 * mensagem em DEBUG. Com a senha digitada sem aspas, o trecho era a propria senha.
 */
@DisplayName("RedactingJsonHttpMessageConverter")
class RedactingJsonHttpMessageConverterTest {

    private final RedactingJsonHttpMessageConverter converter =
            new RedactingJsonHttpMessageConverter(JsonMapper.builder().build());

    private static MockHttpInputMessage body(String json) {
        MockHttpInputMessage message = new MockHttpInputMessage(json.getBytes(StandardCharsets.UTF_8));
        message.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return message;
    }

    @Test
    @DisplayName("never echoes the unreadable content in the error message or its cause")
    void neverEchoesUnreadableContent() {
        HttpMessageNotReadableException failure = catchThrowableOfType(
                HttpMessageNotReadableException.class,
                () -> converter.read(
                        ResolvableType.forClass(Map.class),
                        body("{\"identifier\": \"maria@ovyx.com.br\", \"password\": SegredoReal2026}"),
                        null));

        assertThat(failure).isNotNull();
        assertThat(failure.getMessage()).doesNotContain("Segredo").contains("linha 1");
        assertThat(failure.getCause()).as("a causa do parser carrega o trecho lido").isNull();
    }

    @Test
    @DisplayName("still reads a well-formed body")
    void stillReadsWellFormedBody() throws Exception {
        Object read = converter.read(
                ResolvableType.forClass(Map.class), body("{\"identifier\": \"maria@ovyx.com.br\"}"), null);

        assertThat(read).isEqualTo(Map.of("identifier", "maria@ovyx.com.br"));
    }
}
