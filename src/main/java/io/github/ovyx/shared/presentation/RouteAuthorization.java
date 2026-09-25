package io.github.ovyx.shared.presentation;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * As rotas de um contexto, cada uma com o perfil que ela exige (R-008 da feature 002).
 *
 * <p>Cada contexto declara as suas num bean que implementa esta interface, e a configuração de
 * segurança aplica todas antes da negação por omissão. Assim nenhum contexto conhece as rotas de
 * outro, e o {@code denyAll()} final continua num lugar só: a rota que ninguém declarou nasce negada,
 * até para quem está autenticado.
 *
 * <p>Uma rota por vez, com o método, como na feature 001: declarar um caminho inteiro liberaria junto
 * os métodos que ainda não existem.
 *
 * <p>A ordem entre os contextos não é garantida, e por isso as rotas de contextos diferentes nunca se
 * sobrepõem: cada contexto declara só os caminhos dele ({@code /api/v1/sectors} no farm;
 * {@code /api/v1/auth}, {@code /api/v1/me} e {@code /api/v1/caretakers} no identity). Um padrão amplo,
 * como {@code /api/v1/**}, sombrearia as regras de outro contexto, e não entra aqui.
 */
@FunctionalInterface
public interface RouteAuthorization {

    /** Declara as rotas do contexto no registro de regras da cadeia de segurança. */
    void authorize(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry routes);
}
