package io.github.ovyx.shared.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Testes do registro do conversor JSON com redacao.
 *
 * <p>Registrado como bean, o conversor passava a frente do de {@code byte[]}, e o documento OpenAPI
 * saia em base64. A prova com a aplicacao inteira esta em {@code ManagementPortIT} e, para a
 * redacao em si, em {@code AuthenticationContractIT}.
 */
@DisplayName("JsonConverterConfiguration")
class JsonConverterConfigurationTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private List<HttpMessageConverter<?>> converters() {
        HttpMessageConverters.ServerBuilder builder = HttpMessageConverters.forServer().registerDefaults();
        // O que o Spring Boot faz antes, na ordem 0.
        builder.withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper));
        new JsonConverterConfiguration().redactingJsonConverterCustomizer(jsonMapper).customize(builder);

        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        builder.build().forEach(converters::add);
        return converters;
    }

    @Test
    @DisplayName("replaces the default JSON converter instead of adding a second one")
    void givenBootDefaultJsonConverter_whenCustomizing_thenReplaceItInsteadOfAddingASecondOne() {
        // given — converters() registra o padrao do Boot antes de personalizar

        // when
        List<HttpMessageConverter<?>> converters = converters();

        // then
        assertThat(converters)
                .filteredOn(JacksonJsonHttpMessageConverter.class::isInstance)
                .singleElement()
                .isInstanceOf(RedactingJsonHttpMessageConverter.class);
    }

    @Test
    @DisplayName("keeps the byte array converter ahead of the JSON converter")
    void givenByteArrayAndJsonConverters_whenCustomizing_thenKeepByteArraysAheadOfJson() {
        // given — converters() registra os padroes do Boot, inclusive o de byte array

        // when
        List<HttpMessageConverter<?>> converters = converters();

        // then
        int byteArray = indexOf(converters, ByteArrayHttpMessageConverter.class);
        int json = indexOf(converters, RedactingJsonHttpMessageConverter.class);
        assertThat(byteArray).isNotNegative().isLessThan(json);
    }

    private static int indexOf(List<HttpMessageConverter<?>> converters, Class<?> type) {
        return IntStream.range(0, converters.size())
                .filter(position -> type.isInstance(converters.get(position)))
                .findFirst()
                .orElse(-1);
    }
}
