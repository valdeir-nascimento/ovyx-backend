package io.github.ovyx.shared.presentation;

import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import tools.jackson.databind.json.JsonMapper;

/**
 * Poe o {@link RedactingJsonHttpMessageConverter} no lugar do conversor JSON padrao.
 *
 * <p>O conversor nao e bean de proposito. No Spring Boot 4, todo bean {@code HttpMessageConverter}
 * entra como conversor adicional, a frente dos padroes — inclusive dos de {@code byte[]} e de
 * {@code String}. O springdoc devolve o documento OpenAPI como {@code byte[]}, e com o JSON na frente
 * o documento saia como uma string base64, que o Swagger UI nao consegue ler. Pelo customizador, o
 * conversor ocupa a posicao do JSON, e a ordem dos demais se mantem.
 *
 * <p>A ordem mais baixa faz este customizador rodar depois do que o Spring Boot registra para o
 * Jackson (ordem 0): as duas chamadas substituem o mesmo conversor, e vale a ultima.
 */
@Configuration(proxyBeanMethods = false)
public class JsonConverterConfiguration {

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    ServerHttpMessageConvertersCustomizer redactingJsonConverterCustomizer(JsonMapper jsonMapper) {
        return builder -> builder.withJsonConverter(new RedactingJsonHttpMessageConverter(jsonMapper));
    }
}
