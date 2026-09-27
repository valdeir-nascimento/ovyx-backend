package io.github.ovyx.shared.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A configuração da documentação recusa na subida o que o CORS com credenciais não pode ter: o
 * curinga, que com credenciais derrubaria cada requisição de outra origem em 500, e o que não for
 * uma origem ({@code esquema://host[:porta]}). O servidor publicado precisa ser um endereço absoluto.
 */
@DisplayName("ApiDocsProperties")
class ApiDocsPropertiesTest {

    private static final String SERVER = "http://localhost:8080";

    @Test
    @DisplayName("accepts exact origins, with or without a port")
    void givenExactOrigins_whenBinding_thenKeepThem() {
        // given
        List<String> origins = List.of("http://localhost:9090", "https://docs.ovyx.example");

        // when
        ApiDocsProperties properties = new ApiDocsProperties(origins, SERVER, "Ambiente local");

        // then
        assertThat(properties.allowedOrigins()).containsExactlyElementsOf(origins);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"*", "http://*.ovyx.example", "http://localhost:9090/", "localhost:9090", "ftp://localhost"})
    @DisplayName("refuses anything that is not an exact origin, naming the property")
    void givenSomethingOtherThanAnOrigin_whenBinding_thenRefuseNamingTheProperty(String origin) {
        // given
        List<String> origins = List.of(origin);

        // when
        ThrowingCallable binding = () -> new ApiDocsProperties(origins, SERVER, "Ambiente local");

        // then
        assertThatThrownBy(binding)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ovyx.api-docs.allowed-origins")
                .hasMessageContaining(origin);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/", "localhost:8080", "api.ovyx.example"})
    @DisplayName("refuses a server that is not an absolute address")
    void givenRelativeServer_whenBinding_thenRefuseNamingTheProperty(String server) {
        // given
        // Um endereço relativo seria resolvido pela origem da interface, na porta de gerenciamento, e
        // o "Try it out" chamaria a porta errada.

        // when
        ThrowingCallable binding = () -> new ApiDocsProperties(List.of(), server, "Ambiente local");

        // then
        assertThatThrownBy(binding)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ovyx.api-docs.server-url");
    }
}
