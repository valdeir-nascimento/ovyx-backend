package io.github.ovyx.shared.presentation;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentacao da API, publicada em interface propria.
 *
 * <p>A interface fica na porta de gerenciamento ({@code http://localhost:9090/actuator/swagger-ui}), separada da
 * API e da aplicacao Angular, atendendo FR-026. A configuracao de porta esta em
 * {@code application.yml} ({@code springdoc.use-management-port: true}).
 *
 * <p>Descricoes em portugues por exigencia de FR-029 e do principio VII: quem le a documentacao e
 * pessoa, nao compilador.
 *
 * <p>O texto geral e o esquema de seguranca espelham {@code contracts/identity-api.yaml}: o cookie de
 * sessao vale para todas as operacoes, e a entrada, que e publica, o dispensa por anotacao propria.
 * Tags e descricoes cobrem so o que esta publicado; as operacoes de gestao de responsaveis entram
 * com a entrega delas.
 */
@Configuration
public class OpenApiConfiguration {

    private static final String SESSION_COOKIE = "sessionCookie";

    @Bean
    OpenAPI ovyxOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Ovyx — API de Identidade")
                .version("1.0.0")
                .description(
                    """
                        Acesso ao sistema e à própria conta do responsável (`Caretaker`).

                        **Sessão**: a autenticação devolve um cookie de sessão `HttpOnly`. \
                        Não há token no corpo da resposta. A sessão expira após 30 minutos \
                        de inatividade e é invalidada imediatamente no encerramento.

                        **Erros**: todas as respostas de erro seguem RFC 9457 \
                        (`application/problem+json`) e trazem a extensão `code`, identificador \
                        estável da regra violada. Falhas de validação trazem também `details`, \
                        com uma mensagem por campo e **todas** as violações de uma só vez — \
                        nunca apenas a primeira.

                        **Mensagem genérica**: qualquer falha de autenticação — \
                        identificador inexistente, senha incorreta, responsável inativo ou \
                        tentativas em excesso — devolve exatamente a mesma resposta, para \
                        não revelar qual das condições ocorreu.

                        **CSRF**: toda resposta emite o cookie `XSRF-TOKEN`, legível pelo \
                        cliente, e toda requisição que altera estado (POST, PUT) precisa \
                        devolver o valor dele no cabeçalho `X-XSRF-TOKEN`. O cookie vem em \
                        qualquer resposta anterior — por exemplo `GET /api/v1/auth/me`, que \
                        responde 401 sem sessão mas já o entrega. Sem o cabeçalho, a resposta \
                        é 403 com `code` `CSRF_TOKEN_INVALID`, distinto de `FORBIDDEN`. No login, \
                        o token é trocado, e a resposta traz um `XSRF-TOKEN` novo.

                        **Troca de senha pendente**: enquanto `mustChangePassword` for `true`, \
                        só são aceitas a troca da própria senha, a saída, a consulta da própria \
                        identidade e uma nova entrada. As demais operações respondem 403 com \
                        `code` `PASSWORD_CHANGE_REQUIRED`.

                        **Requisição não suportada**: corpo em formato diferente de JSON (415) \
                        ou formato de resposta indisponível (406) respondem com `code` \
                        `REQUEST_NOT_ACCEPTABLE`, mantendo o status real. Corpo que o parser não \
                        consegue ler responde 400 com o mesmo código, sem repetir o conteúdo \
                        enviado. Nas operações que devolvem corpo de sucesso — a entrada e \
                        `/auth/me` —, um `Accept` que aceita só `application/problem+json` \
                        também recebe 406: esse é o formato dos erros, e não serve a uma \
                        resposta de sucesso.

                        **Método ou rota não declarados**: não produzem 405, com uma exceção: \
                        `TRACE`, que o próprio servidor recusa com 405 antes de a requisição \
                        chegar à aplicação, num corpo fora do formato de erro deste documento. \
                        A autorização é declarada por endpoint e método, e o que não está \
                        declarado é negado antes de o roteamento avaliar o método: sem sessão, \
                        a resposta é 401 `UNAUTHENTICATED`; com sessão, 403 `FORBIDDEN`. Uma \
                        requisição que altera estado sem o cabeçalho `X-XSRF-TOKEN` é recusada \
                        antes disso, com 403 `CSRF_TOKEN_INVALID`.

                        **Falha inesperada**: defeito técnico responde 500 com `code` \
                        `INTERNAL_ERROR`, sem nenhum detalhe da causa.
                        """))
            .tags(List.of(
                new Tag()
                    .name("Acesso")
                    .description("Entrada, saída e identificação do responsável autenticado"),
                new Tag()
                    .name("Minha conta")
                    .description("Operações do responsável autenticado sobre a própria conta")))
            .addSecurityItem(new SecurityRequirement().addList(SESSION_COOKIE))
            .components(new Components()
                .addSecuritySchemes(
                    SESSION_COOKIE,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("SESSION")
                        .description(
                            "Cookie de sessão emitido no login. `HttpOnly`, `Secure`, `SameSite=Lax`.")));
    }
}
