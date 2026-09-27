package io.github.ovyx.shared.presentation;

import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;

/**
 * Página do Swagger UI que manda o token CSRF para a API em outra porta do mesmo host (FR-028, V-16).
 *
 * <p>O interceptador do springdoc só põe o cabeçalho {@code X-XSRF-TOKEN} quando a chamada vai para
 * a mesma origem da página, comparando host e porta. A interface fica na porta de gerenciamento e a
 * API na da aplicação: com a porta na comparação, o cabeçalho nunca saía, e toda escrita pelo "Try it
 * out" voltava 403. Aqui a comparação é por esquema e hostname, sem a porta.
 *
 * <p>Isso não expõe o token a ninguém novo: o cookie {@code XSRF-TOKEN} vale para o host inteiro,
 * sem distinção de porta (RFC 6265), e a página da documentação já consegue lê-lo. Chamada para outro
 * host continua sem o cabeçalho.
 */
class CrossPortCsrfSwaggerIndexTransformer extends SwaggerIndexPageTransformer {

    /** O ponto do inicializador do Swagger UI antes do qual o interceptador entra, como no springdoc. */
    private static final String PRESETS = "presets: [";

    CrossPortCsrfSwaggerIndexTransformer(
            SwaggerUiConfigProperties swaggerUiConfig,
            SwaggerUiOAuthProperties swaggerUiOAuthProperties,
            SwaggerWelcomeCommon swaggerWelcomeCommon,
            ObjectMapperProvider objectMapperProvider) {
        super(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
    }

    @Override
    protected String addCSRF(String html) {
        String cookie = swaggerUiConfig.getCsrf().getCookieName();
        String header = swaggerUiConfig.getCsrf().getHeaderName();
        String interceptor = """
                requestInterceptor: (request) => {
                            const value = `; ${document.cookie}`;
                            const parts = value.split(`; %s=`);
                            const currentURL = new URL(document.URL);
                            const requestURL = new URL(request.url, document.location.origin);
                            const isSameHost = (currentURL.protocol === requestURL.protocol && currentURL.hostname === requestURL.hostname);
                            if (isSameHost && parts.length === 2) request.headers['%s'] = parts.pop().split(';').shift();
                            return request;
                        },
                        %s""".formatted(cookie, header, PRESETS);
        return html.replace(PRESETS, interceptor);
    }
}
