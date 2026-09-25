package io.github.ovyx.shared.presentation;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * A interface de documentação e a API que ela chama (FR-026, FR-028).
 *
 * <p>A interface fica na porta de gerenciamento e a API, na da aplicação: são origens diferentes. O
 * "Try it out" precisa de três coisas daqui:
 *
 * <ul>
 *   <li>o servidor publicado, fixo pela configuração: calculado pelo cabeçalho {@code Host} de quem
 *       lê o documento, um {@code Host} forjado mandaria as senhas digitadas na interface para outro
 *       servidor;
 *   <li>as origens da interface, liberadas no CORS com credenciais. Vazio por padrão: liberar é
 *       decisão de cada ambiente, e o perfil {@code dev} libera {@code http://localhost:9090};
 *   <li>interface e API no mesmo host e no mesmo esquema: o cookie {@code XSRF-TOKEN}, o
 *       {@code SameSite=Lax} e o {@code Secure} do cookie da sessão valem por host, e não por porta.
 * </ul>
 *
 * @param allowedOrigins as origens exatas da interface ({@code esquema://host[:porta]}), sem curinga
 * @param serverUrl o endereço absoluto da API, o que a interface chama
 * @param serverDescription como o servidor aparece na interface
 */
@ConfigurationProperties(prefix = "ovyx.api-docs")
public record ApiDocsProperties(
        @DefaultValue List<String> allowedOrigins,
        @DefaultValue("http://localhost:8080") String serverUrl,
        @DefaultValue("Ambiente local") String serverDescription) {

    /** Origem exata: esquema http ou https, host e porta opcional, sem caminho e sem curinga. */
    private static final Pattern ORIGIN = Pattern.compile("https?://[A-Za-z0-9.-]+(:\\d{1,5})?");

    public ApiDocsProperties {
        allowedOrigins = List.copyOf(allowedOrigins);
        allowedOrigins.forEach(origin -> {
            if (!ORIGIN.matcher(origin).matches()) {
                throw new IllegalArgumentException(
                        "ovyx.api-docs.allowed-origins aceita só origens exatas (esquema://host[:porta]), sem"
                                + " curinga e sem caminho: " + origin);
            }
        });
        if (!isAbsoluteHttp(serverUrl)) {
            throw new IllegalArgumentException(
                    "ovyx.api-docs.server-url precisa ser um endereço absoluto http ou https: " + serverUrl);
        }
    }

    private static boolean isAbsoluteHttp(String url) {
        try {
            URI uri = URI.create(url);
            return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) && uri.getHost() != null;
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }
}
