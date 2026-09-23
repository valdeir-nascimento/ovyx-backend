package io.github.ovyx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * Ponto de entrada do Ovyx.
 *
 * <p>Esta classe vive na raiz do pacote porque o Spring Boot varre os subpacotes a partir daqui.
 * Ela e o unico ponto em que o framework "ve" o projeto inteiro; as camadas {@code domain} e
 * {@code application} de cada contexto permanecem livres de qualquer dependencia de framework,
 * conforme o principio I da constituicao.
 *
 * <p>O usuario em memoria do Spring Boot fica desligado. Como a autenticacao do Ovyx nao usa
 * {@code UserDetailsService}, o Spring Boot criava por conta propria o usuario {@code user}, com
 * senha gerada impressa no log a cada subida: uma credencial fora do cadastro, em texto claro no
 * log (FR-021).
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class OvyxApplication {

    public static void main(String[] args) {
        SpringApplication.run(OvyxApplication.class, args);
    }
}
