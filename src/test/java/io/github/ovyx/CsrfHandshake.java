package io.github.ovyx;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import jakarta.servlet.http.Cookie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Handshake CSRF real, como o cliente SPA faz: obtem o cookie {@code XSRF-TOKEN} numa resposta
 * qualquer da API e o devolve no cabecalho {@code X-XSRF-TOKEN}.
 *
 * <p>Substitui o {@code csrf()} do spring-security-test, que nao pode ser usado neste projeto. Na
 * primeira chamada, aquele atalho troca o repositorio do {@code CsrfFilter} por um
 * {@code HttpSessionCsrfTokenRepository} — e a troca persiste no contexto Spring, que e compartilhado
 * por todos os testes e classes. A partir dai o token passa a ser gravado na sessao, nunca mais em
 * cookie, e qualquer teste do fluxo real falha conforme a ordem de execucao. Alem disso, o atalho
 * pula exatamente o handshake que precisa ser provado.
 */
public final class CsrfHandshake {

    private static final String COOKIE = "XSRF-TOKEN";
    private static final String HEADER = "X-XSRF-TOKEN";

    private CsrfHandshake() {
    }

    /**
     * Pos-processador que acrescenta o cookie e o cabecalho obtidos da propria API.
     *
     * <p>Um {@code XSRF-TOKEN} ja presente na requisicao e substituido, e nao somado: o navegador
     * guarda um cookie por nome. Os cookies devolvidos pelo login trazem o token trocado (T168), e
     * com dois cookies de mesmo nome o servidor leria o primeiro e recusaria o cabecalho.
     */
    public static RequestPostProcessor using(MockMvc mockMvc) {
        return request -> {
            Cookie token = fetchToken(mockMvc);

            List<Cookie> cookies = new ArrayList<>();
            if (request.getCookies() != null) {
                Arrays.stream(request.getCookies())
                    .filter(cookie -> !COOKIE.equals(cookie.getName()))
                    .forEach(cookies::add);
            }
            cookies.add(token);

            request.setCookies(cookies.toArray(Cookie[]::new));
            request.addHeader(HEADER, token.getValue());
            return request;
        };
    }

    private static Cookie fetchToken(MockMvc mockMvc) {
        try {
            Cookie token = mockMvc.perform(get("/api/v1/auth/me")).andReturn().getResponse().getCookie(COOKIE);
            if (token == null) {
                throw new IllegalStateException("a API não emitiu o cookie XSRF-TOKEN numa resposta anônima");
            }
            return token;
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("falha ao obter o token CSRF da API", exception);
        }
    }
}
