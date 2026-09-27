package io.github.ovyx.shared.infrastructure;

import io.github.ovyx.shared.application.FarmCalendar;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FarmTimeZoneProperties.class)
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${ovyx.timezone:America/Sao_Paulo}") final String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }

    /** "Hoje" no fuso da granja, para o relatorio diario e a pesagem (R-004 da 005). */
    @Bean
    public FarmCalendar farmCalendar(Clock clock, FarmTimeZoneProperties properties) {
        return new FarmCalendar(clock, properties.zone());
    }
}
