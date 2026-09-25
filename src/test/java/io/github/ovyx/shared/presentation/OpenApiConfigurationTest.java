package io.github.ovyx.shared.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * A configuração da documentação não pode impedir a aplicação de subir quando a interface ou o
 * documento são desligados: é um ajuste comum de operação, e desligar a interface deve desligar só a
 * interface. A página que manda o cabeçalho CSRF entre portas depende de propriedades que o springdoc
 * só cria com os dois ligados.
 */
@DisplayName("OpenApiConfiguration")
class OpenApiConfigurationTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ApiDocsProperties.class)
    static class Properties {}

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(OpenApiConfiguration.class, Properties.class);

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"springdoc.swagger-ui.enabled=false", "springdoc.api-docs.enabled=false"})
    @DisplayName("starts without the interface page when the documentation is switched off")
    void givenDocumentationSwitchedOff_whenStarting_thenStartWithoutTheInterfacePage(String switchedOff) {
        // given
        ApplicationContextRunner withTheDocumentationOff = runner.withPropertyValues(switchedOff);

        // when / then
        withTheDocumentationOff.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(SwaggerIndexTransformer.class);
        });
    }
}
