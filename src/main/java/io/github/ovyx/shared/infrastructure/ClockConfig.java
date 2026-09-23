package io.github.ovyx.shared.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${ovyx.timezone:America/Sao_Paulo}") final String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
