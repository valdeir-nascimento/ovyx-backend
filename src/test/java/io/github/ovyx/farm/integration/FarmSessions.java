package io.github.ovyx.farm.integration;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ovyx.CsrfHandshake;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Sessões para os testes do contexto farm: cada uma com um responsável novo, do perfil pedido.
 *
 * <p>O farm não conhece o identity no código de produção; nos testes de ponta a ponta, alguém precisa
 * entrar no sistema, e só o identity sabe fazer isso.
 */
final class FarmSessions {

    private static final String PASSWORD = "GranjaNorte2026";

    private final MockMvc mockMvc;
    private final CaretakerRepository caretakerRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    FarmSessions(
            MockMvc mockMvc, CaretakerRepository caretakerRepository, PasswordHasher passwordHasher, Clock clock) {
        this.mockMvc = mockMvc;
        this.caretakerRepository = caretakerRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    /** Entra como um administrador novo. */
    Cookie[] administrator() throws Exception {
        return signInAs(aUniqueCaretaker().withRole(Role.ADMINISTRATOR));
    }

    /** Entra como um usuário comum novo. */
    Cookie[] commonUser() throws Exception {
        return signInAs(aUniqueCaretaker().withRole(Role.USER));
    }

    /** Entra como um usuário comum que ainda deve a troca da senha provisória. */
    Cookie[] commonUserOwingThePasswordChange() throws Exception {
        return signInAs(aUniqueCaretaker().withRole(Role.USER).withPendingPasswordChange());
    }

    /** O token CSRF de verdade, obtido do próprio servidor. */
    RequestPostProcessor csrf() {
        return CsrfHandshake.using(mockMvc);
    }

    private Cookie[] signInAs(CaretakerTestDataBuilder builder) throws Exception {
        Caretaker caretaker = builder.withPassword(PASSWORD)
                .withHasher(passwordHasher)
                .withRoster(caretakerRepository)
                .withClock(clock)
                .build();
        caretakerRepository.save(caretaker);
        return mockMvc.perform(post("/api/v1/auth/sign-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"identifier": "%s", "password": "%s"}
                                 """.formatted(caretaker.email().value(), PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }
}
