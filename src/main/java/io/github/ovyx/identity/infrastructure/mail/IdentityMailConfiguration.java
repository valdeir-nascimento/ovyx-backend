package io.github.ovyx.identity.infrastructure.mail;

import io.github.ovyx.identity.application.recovery.IdentityMailer;
import io.github.ovyx.shared.infrastructure.FarmTimeZoneProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * O envio dos e-mails do identity (R-006 da 012). O {@link JavaMailSender} vem da configuracao {@code spring.mail.*}
 * do Spring Boot; o instante do aviso sai no fuso da granja.
 */
@Configuration(proxyBeanMethods = false)
public class IdentityMailConfiguration {

    @Bean
    IdentityMailer identityMailer(
            JavaMailSender sender, OvyxMailProperties properties, FarmTimeZoneProperties farmTimeZone) {
        return new SmtpIdentityMailer(sender, properties, farmTimeZone.zone());
    }
}
