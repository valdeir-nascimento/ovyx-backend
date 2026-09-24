package io.github.ovyx.identity.presentation.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.shared.presentation.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Testes do bloqueio de operacoes enquanto a troca de senha for obrigatoria (FR-025, cenario V-05).
 *
 * <p>Exercita o filtro diretamente. Por HTTP, hoje, o bloqueio nao e observavel: toda rota da
 * Historia 1 ou esta na lista de liberacao ou ja e negada pela autorizacao. Sem este teste, o filtro
 * seguiria sem nenhuma prova de que funciona ate a Historia 2 acrescentar rotas.
 */
@DisplayName("PasswordChangeRequiredFilter")
class PasswordChangeRequiredFilterTest {

    private final PasswordChangeRequiredFilter filter =
            new PasswordChangeRequiredFilter(JsonMapper.builder().build());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticate(boolean mustChangePassword) {
        AuthenticatedUser principal = new AuthenticatedUser(
                UUID.randomUUID(), "Administrador do Sistema", Role.ADMINISTRATOR.name(), mustChangePassword);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority(principal.authority()))));
    }

    private static MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        return request;
    }

    @Test
    @DisplayName("blocks any other operation while the password change is pending")
    void givenPendingPasswordChange_whenCallingAnotherOperation_thenBlockWith403() throws Exception {
        // given
        authenticate(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request("GET", "/api/v1/caretakers"), response, chain);

        // then
        assertThat(chain.getRequest()).as("a requisicao nao pode seguir adiante").isNull();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("PASSWORD_CHANGE_REQUIRED")
                .contains("\"instance\":\"/api/v1/caretakers\"");
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
        "PUT, /api/v1/me/password",
        "POST, /api/v1/auth/sign-out",
        "GET, /api/v1/auth/me",
        "POST, /api/v1/auth/sign-in"
    })
    @DisplayName("lets the allowed operations through while the password change is pending")
    void givenPendingPasswordChange_whenCallingAnAllowedOperation_thenLetItThrough(String method, String uri)
            throws Exception {
        // given
        authenticate(true);
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request(method, uri), new MockHttpServletResponse(), chain);

        // then
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("matches method and path together, so the right path with another method is blocked")
    void givenPendingPasswordChange_whenCallingTheAllowedPathWithAnotherMethod_thenBlockWith403() throws Exception {
        // given
        authenticate(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request("DELETE", "/api/v1/me/password"), response, chain);

        // then
        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("lets everything through once the password has been changed")
    void givenPasswordAlreadyChanged_whenCallingAnyOperation_thenLetItThrough() throws Exception {
        // given
        authenticate(false);
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request("GET", "/api/v1/caretakers"), new MockHttpServletResponse(), chain);

        // then
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("does not interfere with anonymous requests, which authorization already handles")
    void givenAnonymousRequest_whenFiltering_thenLeaveItToAuthorization() throws Exception {
        // given — nenhuma autenticacao no contexto de seguranca
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request("GET", "/api/v1/caretakers"), new MockHttpServletResponse(), chain);

        // then
        assertThat(chain.getRequest()).isNotNull();
    }
}
