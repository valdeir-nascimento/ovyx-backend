package io.github.ovyx.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.shared.application.FarmCalendar;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Testes do relogio que a aplicacao injeta.
 *
 * <p>Sem bean de {@link Clock} nada do dominio consegue datar nada, e o contexto nem sobe. O fuso
 * vem de propriedade porque relatorio de granja e lido no horario local de quem cria as aves.
 */
@DisplayName("ClockConfig")
class ClockConfigTest {

    private final ApplicationContextRunner contexts =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ClockConfig.class));

    @Test
    @DisplayName("Uses the Brazilian timezone when the environment says nothing")
    void givenNoTimezoneProperty_whenStarting_thenUseSaoPaulo() {
        // given
        ApplicationContextRunner context = contexts;

        // when
        context.run(started -> assertThat(started)
                .getBean(Clock.class)
                // then
                .extracting(Clock::getZone)
                .isEqualTo(ZoneId.of("America/Sao_Paulo")));
    }

    @Test
    @DisplayName("Uses the timezone the environment asks for")
    void givenConfiguredTimezone_whenStarting_thenUseIt() {
        // given
        ApplicationContextRunner context = contexts.withPropertyValues("ovyx.timezone=America/Belem");

        // when
        context.run(started -> assertThat(started)
                .getBean(Clock.class)
                // then
                .extracting(Clock::getZone)
                .isEqualTo(ZoneId.of("America/Belem")));
    }

    @Test
    @DisplayName("Tells the day in the farm timezone the environment asks for")
    void givenFarmTimeZone_whenAskingToday_thenAnswerInThatZone() {
        // given
        // Kiritimati (UTC+14) e Pago Pago (UTC-11) distam 25 horas: o dia de uma sempre vem depois do da outra.
        ApplicationContextRunner context = contexts.withPropertyValues("ovyx.farm.time-zone=Pacific/Kiritimati");

        // when
        context.run(started -> assertThat(started)
                .getBean(FarmCalendar.class)
                // then
                .extracting(FarmCalendar::today)
                .matches(today -> today.isAfter(LocalDate.now(ZoneId.of("Pacific/Pago_Pago")))));
    }
}
