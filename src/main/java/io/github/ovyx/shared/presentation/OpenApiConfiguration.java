package io.github.ovyx.shared.presentation;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.List;

import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * <p>Um documento so para os dois contratos, {@code identity-api.yaml} e {@code farm-api.yaml} (R-012 da
 * feature 002): quem integra entra e cadastra um setor na mesma pagina. O texto geral e o da identidade,
 * seguido do da granja sem o paragrafo que remete a identidade; o esquema de seguranca e o dos dois: o
 * cookie de sessao vale para todas as operacoes, e a entrada, que e publica, o dispensa por anotacao
 * propria.
 *
 * <p>A interface chama a API de outra origem, com o cookie da sessao e o cabecalho CSRF: o CORS fica
 * em {@code SecurityConfiguration}; as credenciais, em {@code application.yml}; e o cabecalho CSRF entre
 * portas, no {@link CrossPortCsrfSwaggerIndexTransformer}. O servidor publicado vem de
 * {@link ApiDocsProperties}, e nao do cabecalho {@code Host} de quem le o documento.
 */
@Configuration
public class OpenApiConfiguration {

    private static final String SESSION_COOKIE = "sessionCookie";

    @Bean
    OpenAPI ovyxOpenApi(ApiDocsProperties apiDocs) {
        return new OpenAPI()
            // Servidor declarado: com ele, o springdoc nunca o calcula pelo Host da requisicao.
            .servers(List.of(new Server().url(apiDocs.serverUrl()).description(apiDocs.serverDescription())))
            .info(new Info()
                .title("Ovyx — API")
                .version("1.0.0")
                .description(
                    """
                        Acesso ao sistema e gestão de responsáveis (`Caretaker`).

                        **Sessão**: a autenticação devolve um cookie de sessão `HttpOnly`. Não há token no corpo da \
                        resposta. A sessão expira após 30 minutos de inatividade e é invalidada imediatamente no \
                        encerramento.

                        **Erros**: todas as respostas de erro seguem RFC 9457 (`application/problem+json`) e trazem a \
                        extensão `code`, identificador estável da regra violada. Falhas de validação trazem também \
                        `details`, com uma mensagem por campo e **todas** as violações de uma só vez — nunca apenas a \
                        primeira.

                        **Mensagem genérica**: qualquer falha de autenticação — identificador inexistente, senha \
                        incorreta, responsável inativo ou tentativas em excesso — devolve exatamente a mesma resposta, \
                        para não revelar qual das condições ocorreu.

                        **CSRF**: a API segue o padrão do Spring Security para SPA. Toda resposta emite o cookie \
                        `XSRF-TOKEN`, legível pelo cliente, e toda requisição que altera estado (POST, PUT) precisa \
                        devolver o valor dele no cabeçalho `X-XSRF-TOKEN`. O cliente obtém o cookie com qualquer \
                        requisição anterior — por exemplo `GET /api/v1/auth/me`, que responde 401 sem sessão mas já \
                        entrega o cookie. Sem o cabeçalho, a resposta é 403 com `code` `CSRF_TOKEN_INVALID`, distinto de \
                        `FORBIDDEN`: o primeiro se resolve repetindo a requisição com o token; o segundo é falta de \
                        permissão. No login, o token é trocado: a resposta traz um `XSRF-TOKEN` novo, que substitui o \
                        anterior.

                        **Troca de senha pendente**: enquanto `mustChangePassword` for `true`, só são aceitas a troca da \
                        própria senha, a saída, a consulta da própria identidade e uma nova entrada. As demais operações \
                        respondem 403 com `code` `PASSWORD_CHANGE_REQUIRED`.

                        **Requisição não suportada**: corpo em formato diferente de JSON (415) ou formato de resposta \
                        indisponível (406) respondem com `code` `REQUEST_NOT_ACCEPTABLE`, mantendo o status real. Corpo \
                        que o parser não consegue ler responde 400 com o mesmo código, sem repetir o conteúdo enviado. \
                        Nas operações que devolvem corpo de sucesso — a entrada, `/auth/me` e as de responsáveis —, um \
                        `Accept` que aceita só `application/problem+json` também recebe 406: esse é o formato dos erros, \
                        e não serve a uma resposta de sucesso. A saída e a troca de senha respondem 204, sem corpo.

                        **Método ou rota não declarados**: não produzem 405, com uma exceção: `TRACE`, que o próprio \
                        servidor recusa com 405 antes de a requisição chegar à aplicação, num corpo fora do formato de \
                        erro deste documento. A autorização é declarada por endpoint e método, e o que não está \
                        declarado é negado antes de o roteamento avaliar o método (FR-012): sem sessão, a resposta é 401 \
                        `UNAUTHENTICATED`; com sessão, 403 `FORBIDDEN`. Uma requisição que altera estado sem o cabeçalho \
                        `X-XSRF-TOKEN` é recusada antes disso, com 403 `CSRF_TOKEN_INVALID`.

                        **Outra origem**: a API só atende chamadas de outra origem vindas da interface de documentação \
                        configurada, com credenciais. Qualquer outra origem, ou um método ou cabeçalho fora dos que a \
                        API usa, recebe 403 `FORBIDDEN` antes de qualquer outra verificação, inclusive a do token de \
                        proteção.

                        **Falha inesperada**: defeito técnico responde 500 com `code` `INTERNAL_ERROR`, sem nenhum \
                        detalhe da causa, que vai apenas para o log do servidor.

                        Setores e gaiolas da granja (`Sector`, `Cage`): a estrutura produtiva onde as features \
                        seguintes lançam produção, ração, mortalidade e peso.

                        **Perfis**: qualquer responsável autenticado consulta setores e gaiolas. Cadastrar, editar, \
                        inativar e reativar é exclusivo do perfil Administrador; o usuário comum recebe o 403 \
                        `FORBIDDEN` genérico, sem saber se o setor ou a gaiola existem.

                        **Nada é apagado**: setores e gaiolas são inativados, e continuam consultáveis. Inativar um \
                        setor inativa junto as gaiolas ativas dele; reativá-lo traz de volta exatamente essas gaiolas.
                        """))
            .tags(List.of(
                new Tag()
                    .name("Acesso")
                    .description("Entrada, saída e identificação do responsável autenticado"),
                new Tag()
                    .name("Responsáveis")
                    .description("Gestão de responsáveis — exclusiva do perfil Administrador"),
                new Tag()
                    .name("Minha conta")
                    .description("Operações do responsável autenticado sobre a própria conta"),
                new Tag()
                    .name("Setores")
                    .description("Cadastro, consulta, inativação e reativação de setores"),
                new Tag()
                    .name("Gaiolas")
                    .description("Cadastro, consulta, inativação e reativação das gaiolas de um setor")))
            .addSecurityItem(new SecurityRequirement().addList(SESSION_COOKIE))
            .components(new Components()
                .addSecuritySchemes(
                    SESSION_COOKIE,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("SESSION")
                        .description(
                            "Cookie de sessão emitido no login. `HttpOnly` e `SameSite=Lax`; `Secure` fora do"
                                + " perfil de desenvolvimento, que usa http.")));
    }

    /**
     * A pagina da interface, com o cabecalho CSRF enviado tambem para outra porta do mesmo host
     * (FR-028). Substitui a do springdoc, que so o envia para a propria origem da pagina.
     *
     * <p>Com as mesmas condicoes do springdoc: sem a interface ou sem o documento, as propriedades que
     * este bean usa nao existem, e desligar a interface nao pode impedir a aplicacao de subir.
     */
    @Bean
    @ConditionalOnProperty(
        name = {"springdoc.api-docs.enabled", "springdoc.swagger-ui.enabled"},
        matchIfMissing = true)
    SwaggerIndexTransformer crossPortCsrfSwaggerIndexTransformer(
        SwaggerUiConfigProperties swaggerUiConfig,
        SwaggerUiOAuthProperties swaggerUiOAuthProperties,
        SwaggerWelcomeCommon swaggerWelcomeCommon,
        ObjectMapperProvider objectMapperProvider) {
        return new CrossPortCsrfSwaggerIndexTransformer(
            swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
    }
}
