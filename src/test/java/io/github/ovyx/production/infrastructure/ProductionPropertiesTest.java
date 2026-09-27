package io.github.ovyx.production.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * O fuso da granja (R-006): decide o que é "hoje" para a data da coleta. Sem configuração, é o horário
 * de Brasília; um fuso que não existe derruba a subida com o nome da propriedade, em vez de deixar o
 * relatório com a data errada.
 */
@DisplayName("ProductionProperties")
class ProductionPropertiesTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ProductionProperties.class)
    static class Properties {}

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Properties.class);

    @Test
    @DisplayName("uses the time of Brasília when nothing is configured")
    void givenNoConfiguration_whenBinding_thenUseSaoPaulo() {
        // given
        ApplicationContextRunner withoutConfiguration = runner;

        // when / then
        withoutConfiguration.run(context -> assertThat(context.getBean(ProductionProperties.class).zone())
                .isEqualTo(ZoneId.of("America/Sao_Paulo")));
    }

    @Test
    @DisplayName("takes the configured time zone")
    void givenConfiguredTimeZone_whenBinding_thenUseIt() {
        // given
        ApplicationContextRunner lisbon = runner.withPropertyValues("ovyx.production.time-zone=Europe/Lisbon");

        // when / then
        lisbon.run(context -> assertThat(context.getBean(ProductionProperties.class).zone())
                .isEqualTo(ZoneId.of("Europe/Lisbon")));
    }

    @Test
    @DisplayName("refuses to start with a time zone that does not exist, naming the property")
    void givenUnknownTimeZone_whenStarting_thenFailNamingTheProperty() {
        // given
        ApplicationContextRunner unknown = runner.withPropertyValues("ovyx.production.time-zone=Marte/Olimpo");

        // when / then
        unknown.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("ovyx.production.time-zone");
        });
    }
}
