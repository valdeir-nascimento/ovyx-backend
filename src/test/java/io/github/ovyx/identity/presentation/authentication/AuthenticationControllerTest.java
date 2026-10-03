package io.github.ovyx.identity.presentation.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.application.authentication.GetAuthenticatedCaretakerQuery;
import io.github.ovyx.identity.application.authentication.SignInCommand;
import io.github.ovyx.identity.application.authentication.SignedIn;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.presentation.security.SessionAuthenticator;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.Command;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Query;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.presentation.ResultHttpMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

/**
 * A entrada e a redefinição da senha ao mesmo tempo (R-022 da 012): a consulta da identidade leva a geração de sessão
 * contra a qual a senha foi conferida. Se a senha foi redefinida no intervalo, a consulta recusa, e a sessão não abre
 * com a geração nova para quem entrou com a senha antiga.
 */
@DisplayName("AuthenticationController")
class AuthenticationControllerTest {

    private static final CaretakerId MARIA = CaretakerId.generate();

    /** O despachante falso: a entrada confere a senha na geração 0, e a consulta vê a redefinição que veio depois. */
    private static final class RacingDispatcher implements Dispatcher {
        private final List<GetAuthenticatedCaretakerQuery> asked = new ArrayList<>();

        @Override
        @SuppressWarnings("unchecked")
        public <R> Result<R> dispatch(Command<R> command) {
            assertThat(command).isInstanceOf(SignInCommand.class);
            return (Result<R>) Result.success(new SignedIn(MARIA, 0));
        }

        @Override
        public <R> Result<R> ask(Query<R> query) {
            asked.add((GetAuthenticatedCaretakerQuery) query);
            return Result.failure(ApplicationError.of(
                    ErrorType.UNAUTHENTICATED,
                    "SESSION_REVOKED",
                    "Sua senha foi redefinida e esta sessão foi encerrada. Entre com a nova senha."));
        }
    }

    @Test
    @DisplayName("asks for the identity in the generation the password was checked against, opening no session if it moved on")
    void givenPasswordRecoveredBetweenTheCheckAndTheQuery_whenSigningIn_thenRefuseWithoutOpeningASession() {
        // given
        RacingDispatcher dispatcher = new RacingDispatcher();
        AuthenticationController controller = new AuthenticationController(
                dispatcher,
                new SessionAuthenticator(new HttpSessionCsrfTokenRepository()),
                new ResultHttpMapper(new SimpleMeterRegistry()));
        MockHttpServletRequest request = new MockHttpServletRequest();

        // when
        ResponseEntity<Object> response = controller.signIn(
                new SignInRequest("maria.silva@ovyx.com.br", "SenhaAntiga2026"), request, new MockHttpServletResponse());

        // then
        assertThat(dispatcher.asked).singleElement().satisfies(query -> {
            assertThat(query.caretakerId()).isEqualTo(MARIA);
            assertThat(query.sessionGeneration()).isZero();
        });
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(request.getSession(false)).isNull();
    }
}
