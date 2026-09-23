package io.github.ovyx.shared.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Testes da injecao da identidade nos controllers.
 *
 * <p>A identidade vem do contexto de seguranca, nunca de parametro da requisicao. Se este resolver
 * lesse o que o chamador manda, qualquer um operaria como outra pessoa.
 */
@DisplayName("AuthenticatedUserArgumentResolver")
class AuthenticatedUserArgumentResolverTest {

    private final AuthenticatedUserArgumentResolver resolver = new AuthenticatedUserArgumentResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** Assinatura de apoio: e dela que saem os parametros submetidos ao resolver. */
    @SuppressWarnings("unused")
    private void handler(AuthenticatedUser user, String other) {
        // sem corpo: o teste so usa a assinatura
    }

    private static MethodParameter parameterAt(int index) throws NoSuchMethodException {
        Method method = AuthenticatedUserArgumentResolverTest.class.getDeclaredMethod(
                "handler", AuthenticatedUser.class, String.class);
        return new MethodParameter(method, index);
    }

    private static AuthenticatedUser authenticate() {
        AuthenticatedUser user =
                new AuthenticatedUser(UUID.randomUUID(), "Maria Silva", "ADMINISTRATOR", false);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority(user.authority()))));
        return user;
    }

    @Test
    @DisplayName("Takes the identity from the security context, not from the request")
    void givenAuthenticatedSession_whenResolving_thenReturnTheIdentityOfTheSession() throws Exception {
        // given
        AuthenticatedUser authenticated = authenticate();

        // when
        Object resolved = resolver.resolveArgument(parameterAt(0), null, null, null);

        // then
        assertThat(resolved).isEqualTo(authenticated);
    }

    @Test
    @DisplayName("Resolves nothing when there is no session")
    void givenAnonymousRequest_whenResolving_thenReturnNothing() throws Exception {
        // given
        SecurityContextHolder.clearContext();

        // when
        Object resolved = resolver.resolveArgument(parameterAt(0), null, null, null);

        // then
        assertThat(resolved).isNull();
    }

    @Test
    @DisplayName("Serves only parameters of the identity type")
    void givenParameterOfAnotherType_whenCheckingSupport_thenDoNotServeIt() throws Exception {
        // given
        MethodParameter identity = parameterAt(0);
        MethodParameter other = parameterAt(1);

        // when
        boolean servesIdentity = resolver.supportsParameter(identity);

        // then
        assertThat(servesIdentity).isTrue();
        assertThat(resolver.supportsParameter(other)).isFalse();
    }
}
