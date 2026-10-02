package io.github.ovyx.identity.integration;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationSessions;
import io.github.ovyx.IntegrationSessions.SignedIn;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * A troca do próprio tema (US2 da 011; contracts/identity-api.yaml, PUT /api/v1/me/theme): só o responsável da
 * sessão, para ele mesmo, sem mexer no cadastro.
 */
@AutoConfigureMockMvc
@DisplayName("Own theme contract")
class OwnThemeContractIT extends IntegrationTestSupport {

    private static final String THEME = "/api/v1/me/theme";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaretakerRepository caretakerRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private Clock clock;

    @Autowired
    private JdbcTemplate jdbc;

    private IntegrationSessions sessions;

    @BeforeEach
    void setUp() {
        sessions = new IntegrationSessions(mockMvc, caretakerRepository, passwordHasher, clock);
    }

    private MockHttpServletRequestBuilder choose(Cookie[] cookies, String body) {
        MockHttpServletRequestBuilder request = put(THEME)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body);
        return cookies.length == 0 ? request : request.cookie(cookies);
    }

    private ResultActions me(Cookie[] cookies) throws Exception {
        return mockMvc.perform(get("/api/v1/auth/me").cookie(cookies));
    }

    @Test
    @DisplayName("saves the theme of whoever is in the session, and of no one else")
    void givenTwoCaretakers_whenOneChoosesTheDarkTheme_thenOnlyHerIdentityCarriesIt() throws Exception {
        // given
        SignedIn maria = sessions.commonUser();
        SignedIn administrator = sessions.administrator();

        // when
        ResultActions response = mockMvc.perform(choose(maria.cookies(), """
                {"theme": "DARK"}
                """).with(sessions.csrf()));

        // then
        response.andExpect(status().isNoContent());
        me(maria.cookies()).andExpect(jsonPath("$.theme").value("DARK"));
        me(administrator.cookies()).andExpect(jsonPath("$.theme").value("SYSTEM"));
    }

    @Test
    @DisplayName("does not touch the instant of the last change of the record")
    void givenCaretaker_whenChoosingTheTheme_thenKeepTheUpdatedAt() throws Exception {
        // given
        SignedIn maria = sessions.commonUser();
        Timestamp before = jdbc.queryForObject(
                "select updated_at from caretaker where id = ?", Timestamp.class, maria.id());

        // when
        mockMvc.perform(choose(maria.cookies(), """
                        {"theme": "LIGHT"}
                        """).with(sessions.csrf()))
                .andExpect(status().isNoContent());

        // then
        assertThat(jdbc.queryForObject("select updated_at from caretaker where id = ?", Timestamp.class, maria.id()))
                .isEqualTo(before);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"{\"theme\": \"AZUL\"}", "{}", "{\"theme\": \"\"}"})
    @DisplayName("refuses what is not a theme, in its field")
    void givenBodyWithoutATheme_whenChoosing_thenAnswerBadRequestInTheThemeField(String body) throws Exception {
        // given
        SignedIn maria = sessions.commonUser();

        // when
        ResultActions response = mockMvc.perform(choose(maria.cookies(), body).with(sessions.csrf()));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.theme").value("Escolha o tema claro, o escuro ou o igual ao sistema."));
    }

    @Test
    @DisplayName("refuses the choice without a session, and without the protection token")
    void givenNoSessionOrNoToken_whenChoosing_thenRefuse() throws Exception {
        // given
        SignedIn maria = sessions.commonUser();

        // when
        ResultActions withoutSession = mockMvc.perform(choose(new Cookie[0], """
                {"theme": "DARK"}
                """).with(sessions.csrf()));
        ResultActions withoutToken = mockMvc.perform(choose(maria.cookies(), """
                {"theme": "DARK"}
                """));

        // then
        withoutSession.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        withoutToken.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("refuses the choice while the provisional password is not changed")
    void givenPendingPasswordChange_whenChoosing_thenRequireThePasswordChange() throws Exception {
        // given
        SignedIn pending = sessions.commonUserOwingThePasswordChange();

        // when
        ResultActions response = mockMvc.perform(choose(pending.cookies(), """
                {"theme": "DARK"}
                """).with(sessions.csrf()));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
        me(pending.cookies()).andExpect(jsonPath("$.theme").value("SYSTEM"));
    }
}
