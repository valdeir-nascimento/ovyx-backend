package io.github.ovyx.identity.integration;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ovyx.IntegrationSessions;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import jakarta.mail.internet.MimeMessage;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * A recuperação da senha (feature 012; contracts/identity-api.yaml, operações de recuperação): o pedido responde
 * sempre igual, só a conta ativa recebe o e-mail, e a causa real fica só na auditoria.
 *
 * <p>Cada teste usa uma origem própria: a contenção por origem é gravada no banco e valeria entre os testes.
 */
@AutoConfigureMockMvc
@DisplayName("Password recovery contract")
@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class PasswordRecoveryContractIT extends IntegrationTestSupport {

    private static final String RECOVERY = "/api/v1/auth/password-recovery";
    private static final Duration ASYNC = Duration.ofSeconds(10);

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

    @Autowired
    private org.springframework.boot.logging.LoggingSystem loggingSystem;

    private static final String WEB_LOGGER = "org.springframework.web";

    @org.junit.jupiter.api.AfterEach
    void restoreWebLogLevel() {
        loggingSystem.setLogLevel(WEB_LOGGER, null);
    }

    private IntegrationSessions sessions;

    private String origin;

    @BeforeEach
    void setUp() throws Exception {
        sessions = new IntegrationSessions(mockMvc, caretakerRepository, passwordHasher, clock);
        origin = "10." + ThreadLocalRandom.current().nextInt(256) + "." + ThreadLocalRandom.current().nextInt(256)
                + "." + ThreadLocalRandom.current().nextInt(256);
        MAIL.purgeEmailFromAllMailboxes();
    }

    private Caretaker saved(CaretakerTestDataBuilder builder) {
        Caretaker caretaker = builder.withHasher(passwordHasher).withRoster(caretakerRepository).build();
        caretakerRepository.save(caretaker);
        return caretaker;
    }

    private Caretaker savedInactive(CaretakerTestDataBuilder builder) {
        Caretaker caretaker = builder.withHasher(passwordHasher).withRoster(caretakerRepository).buildInactive();
        caretakerRepository.save(caretaker);
        return caretaker;
    }

    private MockHttpServletRequestBuilder fromOrigin(MockHttpServletRequestBuilder request) {
        return request.with(mock -> {
            mock.setRemoteAddr(origin);
            return mock;
        });
    }

    private ResultActions requestRecovery(String body) throws Exception {
        return mockMvc.perform(fromOrigin(post(RECOVERY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(sessions.csrf())));
    }

    private ResultActions requestRecoveryFor(String email) throws Exception {
        return requestRecovery("""
                {"email": "%s"}
                """.formatted(email));
    }

    /** Os resultados registrados para o e-mail, esperando o trabalho adiado chegar a eles. */
    private List<String> outcomesFor(String email, int expected) {
        await().atMost(ASYNC).until(() -> outcomes(email).size() >= expected);
        return outcomes(email);
    }

    private List<String> outcomes(String email) {
        return jdbc.queryForList(
                "select outcome from access_event where attempted_identifier = ? order by occurred_at",
                String.class,
                email);
    }

    /** Os e-mails recebidos pela conta, esperando chegar a quantidade dada. */
    private static List<MimeMessage> awaitMessagesTo(String email, int expected) {
        await().atMost(ASYNC).until(() -> messagesTo(email).size() >= expected);
        return messagesTo(email);
    }

    private static List<MimeMessage> messagesTo(String email) {
        return java.util.Arrays.stream(MAIL.getReceivedMessagesForDomain(email.substring(email.indexOf('@') + 1)))
                .filter(message -> {
                    try {
                        return message.getAllRecipients()[0].toString().equals(email);
                    } catch (Exception unreadable) {
                        return false;
                    }
                })
                .toList();
    }

    private static Map<String, Object> shapeOf(MockHttpServletResponse response) {
        return Map.of(
                "status", response.getStatus(),
                "body", response.getContentAsByteArray().length,
                "contentType", String.valueOf(response.getContentType()));
    }

    // ---------------------------------------------------------------- pedido (US1)

    @Test
    @DisplayName("answers 202 without a body and mails the link to the active caretaker")
    void givenActiveCaretaker_whenRequestingRecovery_thenAnswer202AndMailTheLink() throws Exception {
        // given
        Caretaker marina = saved(aUniqueCaretaker());
        String email = marina.email().value();

        // when
        ResultActions response = requestRecoveryFor(email.toUpperCase() + "  ");

        // then
        response.andExpect(status().isAccepted());
        assertThat(response.andReturn().getResponse().getContentAsByteArray()).isEmpty();
        assertThat(awaitMessagesTo(email, 1)).singleElement().satisfies(message ->
                assertThat(message.getSubject()).isEqualTo("Redefina sua senha do Ovyx"));
        assertThat(outcomesFor(email, 1)).containsExactly("RECOVERY_LINK_SENT");
    }

    @Test
    @DisplayName("answers the inactive caretaker and the unknown email exactly as the active one, mailing neither")
    void givenActiveInactiveAndUnknown_whenRequestingRecovery_thenAnswerTheSameAndMailOnlyTheActive() throws Exception {
        // given
        Caretaker active = saved(aUniqueCaretaker());
        Caretaker inactive = savedInactive(aUniqueCaretaker());
        String unknown = "ninguem." + System.nanoTime() + "@ovyx.com.br";

        // when
        MockHttpServletResponse toActive = requestRecoveryFor(active.email().value()).andReturn().getResponse();
        MockHttpServletResponse toInactive = requestRecoveryFor(inactive.email().value()).andReturn().getResponse();
        MockHttpServletResponse toUnknown = requestRecoveryFor(unknown).andReturn().getResponse();

        // then
        assertThat(shapeOf(toActive)).isEqualTo(shapeOf(toInactive)).isEqualTo(shapeOf(toUnknown));
        assertThat(toActive.getStatus()).isEqualTo(202);
        assertThat(outcomesFor(active.email().value(), 1)).containsExactly("RECOVERY_LINK_SENT");
        assertThat(outcomesFor(inactive.email().value(), 1)).containsExactly("RECOVERY_INACTIVE");
        assertThat(outcomesFor(unknown, 1)).containsExactly("RECOVERY_UNKNOWN_EMAIL");
        assertThat(messagesTo(active.email().value())).hasSize(1);
        assertThat(messagesTo(inactive.email().value())).isEmpty();
        assertThat(messagesTo(unknown)).isEmpty();
    }

    @Test
    @DisplayName("refuses a missing email in its field")
    void givenNoEmail_whenRequestingRecovery_thenRefuseInTheEmailField() throws Exception {
        // given
        String body = "{}";

        // when
        ResultActions response = requestRecovery(body);

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.email").value("Informe o e-mail."));
    }

    @Test
    @DisplayName("refuses a malformed email in its field")
    void givenMalformedEmail_whenRequestingRecovery_thenRefuseInTheEmailField() throws Exception {
        // given
        String email = "marina";

        // when
        ResultActions response = requestRecoveryFor(email);

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.email").value("Informe um e-mail em formato válido."));
    }

    @Test
    @DisplayName("refuses the request without the protection token")
    void givenNoCsrfToken_whenRequestingRecovery_thenRefuseAsCsrfTokenInvalid() throws Exception {
        // given
        Caretaker marina = saved(aUniqueCaretaker());

        // when
        ResultActions response = mockMvc.perform(fromOrigin(post(RECOVERY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s"}
                        """.formatted(marina.email().value()))));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("refuses a body that is not JSON")
    void givenPlainTextBody_whenRequestingRecovery_thenRefuseAsUnsupported() throws Exception {
        // given
        String body = "marina.costa@ovyx.com.br";

        // when
        ResultActions response = mockMvc.perform(fromOrigin(post(RECOVERY)
                .contentType(MediaType.TEXT_PLAIN)
                .content(body)
                .with(sessions.csrf())));

        // then
        response.andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("accepts the request from a session that still owes the change of the provisional password")
    void givenSessionOwingThePasswordChange_whenRequestingRecovery_thenAcceptIt() throws Exception {
        // given
        IntegrationSessions.SignedIn owing = sessions.commonUserOwingThePasswordChange();
        Caretaker marina = saved(aUniqueCaretaker());

        // when
        ResultActions response = mockMvc.perform(fromOrigin(post(RECOVERY)
                .cookie(owing.cookies())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s"}
                        """.formatted(marina.email().value()))
                .with(sessions.csrf())));

        // then
        response.andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("never records the code of the link in the audit trail")
    void givenMailedLink_whenReadingTheAuditTrail_thenFindNoCode() throws Exception {
        // given
        Caretaker marina = saved(aUniqueCaretaker());
        requestRecoveryFor(marina.email().value()).andExpect(status().isAccepted());
        String code = RecoveryMails.codeIn(awaitMessagesTo(marina.email().value(), 1).getFirst());
        outcomesFor(marina.email().value(), 1);

        // when
        Integer found = jdbc.queryForObject(
                "select count(*) from access_event where attempted_identifier like ? or origin like ?",
                Integer.class,
                "%" + code + "%",
                "%" + code + "%");
        String storedHash = jdbc.queryForObject(
                "select recovery_token_hash from caretaker where id = ?", String.class, marina.id().value());

        // then
        assertThat(found).isZero();
        assertThat(storedHash).isNotEqualTo(code).hasSize(64);
    }

    // ---------------------------------------------------------------- conferência e redefinição (US2)

    private static final String VERIFICATION = "/api/v1/auth/password-recovery/verification";
    private static final String RESET = "/api/v1/auth/password-reset";
    private static final String NEW_PASSWORD = "PosturaAviario2027";

    /** Pede o link da conta e devolve o código lido do e-mail que chegou, o mais recente. */
    private String linkCodeFor(Caretaker caretaker) throws Exception {
        String email = caretaker.email().value();
        int before = messagesTo(email).size();
        requestRecoveryFor(email).andExpect(status().isAccepted());
        List<MimeMessage> received = awaitMessagesTo(email, before + 1);
        return RecoveryMails.codeIn(received.getLast());
    }

    private ResultActions verify(String code) throws Exception {
        return mockMvc.perform(fromOrigin(post(VERIFICATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token": "%s"}
                        """.formatted(code))
                .with(sessions.csrf())));
    }

    private MockHttpServletRequestBuilder resetRequest(String code, String newPassword) {
        return fromOrigin(post(RESET)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token": "%s", "newPassword": "%s"}
                        """.formatted(code, newPassword))
                .with(sessions.csrf()));
    }

    private ResultActions reset(String code, String newPassword) throws Exception {
        return mockMvc.perform(resetRequest(code, newPassword));
    }

    private ResultActions signIn(Caretaker caretaker, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/sign-in")
                .with(sessions.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"identifier": "%s", "password": "%s"}
                        """.formatted(caretaker.email().value(), password)));
    }

    private jakarta.servlet.http.Cookie[] sessionOf(Caretaker caretaker) throws Exception {
        return signIn(caretaker, CaretakerTestDataBuilder.DEFAULT_PASSWORD)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }

    private ResultActions me(jakarta.servlet.http.Cookie[] cookies) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/me")
                .cookie(cookies));
    }

    private static void expectInvalidLink(ResultActions response) throws Exception {
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RECOVERY_LINK_INVALID"))
                .andExpect(jsonPath("$.detail")
                        .value("Este link de recuperação não vale mais. Peça um novo na tela de entrada."));
    }

    @Test
    @DisplayName("accepts the verification of a valid link, without spending it")
    void givenValidLink_whenVerifying_thenAnswer204AndKeepTheLinkValid() throws Exception {
        // given
        String code = linkCodeFor(saved(aUniqueCaretaker()));

        // when
        ResultActions response = verify(code);

        // then
        response.andExpect(status().isNoContent());
        verify(code).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("refuses the verification of an expired, a replaced, an altered and an unknown link the same way")
    void givenLinksThatNoLongerHold_whenVerifying_thenRefuseThemAllTheSameWay() throws Exception {
        // given
        Caretaker marina = saved(aUniqueCaretaker());
        String replaced = linkCodeFor(marina);
        String current = linkCodeFor(marina);
        String altered = (current.charAt(0) == 'A' ? 'B' : 'A') + current.substring(1);
        Caretaker joao = saved(aUniqueCaretaker());
        String expired = linkCodeFor(joao);
        jdbc.update(
                "update caretaker set recovery_expires_at = now() - interval '1 minute' where id = ?",
                joao.id().value());

        // when / then
        expectInvalidLink(verify(replaced));
        expectInvalidLink(verify(altered));
        expectInvalidLink(verify(expired));
        expectInvalidLink(verify("x".repeat(43)));
        verify(current).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("recovers the password: the old one stops working, the new one works, and the link is spent")
    void givenValidLink_whenResetting_thenSwapThePasswordsAndSpendTheLink() throws Exception {
        // given
        Caretaker marina = saved(aUniqueCaretaker());
        String code = linkCodeFor(marina);

        // when
        ResultActions response = reset(code, NEW_PASSWORD);

        // then
        response.andExpect(status().isNoContent());
        signIn(marina, CaretakerTestDataBuilder.DEFAULT_PASSWORD).andExpect(status().isUnauthorized());
        signIn(marina, NEW_PASSWORD).andExpect(status().isOk());
        expectInvalidLink(reset(code, "OutraSenha2028"));
        expectInvalidLink(verify(code));
        assertThat(outcomesFor(marina.email().value(), 2)).contains("PASSWORD_RECOVERED");
    }

    @Test
    @DisplayName("refuses a new password outside the policy with every violation, keeping the link valid")
    void givenPasswordOutsideThePolicy_whenResetting_thenRefuseInTheFieldAndKeepTheLink() throws Exception {
        // given
        String code = linkCodeFor(saved(aUniqueCaretaker()));

        // when
        ResultActions response = reset(code, "curta");

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.newPassword")
                        .value(org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("ao menos 12 caracteres"),
                                org.hamcrest.Matchers.containsString("ao menos um dígito"))));
        verify(code).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("releases the caretaker from the provisional password")
    void givenCaretakerOwingTheProvisionalPassword_whenResetting_thenSignInWithoutTheObligation() throws Exception {
        // given
        Caretaker carla = saved(aUniqueCaretaker().withPendingPasswordChange());
        String code = linkCodeFor(carla);

        // when
        reset(code, NEW_PASSWORD).andExpect(status().isNoContent());

        // then
        signIn(carla, NEW_PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    @DisplayName("ends every open session of the caretaker, and keeps the session of another account that did the reset")
    void givenOpenSessions_whenResetting_thenEndThoseOfTheCaretakerAndKeepTheOthers() throws Exception {
        // given
        Caretaker marina = saved(aUniqueCaretaker());
        jakarta.servlet.http.Cookie[] computer = sessionOf(marina);
        jakarta.servlet.http.Cookie[] phone = sessionOf(marina);
        IntegrationSessions.SignedIn administrator = sessions.administrator();
        String code = linkCodeFor(marina);

        // when
        mockMvc.perform(resetRequest(code, NEW_PASSWORD).cookie(administrator.cookies()))
                .andExpect(status().isNoContent());

        // then
        me(computer).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("SESSION_REVOKED"));
        me(phone).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("SESSION_REVOKED"));
        me(administrator.cookies())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value(administrator.fullName()));
    }

    @Test
    @DisplayName("accepts only one of two simultaneous resets with the same link")
    void givenTwoSimultaneousResets_whenSending_thenAcceptOnlyOne() throws Exception {
        // given
        Caretaker marina = saved(aUniqueCaretaker());
        String code = linkCodeFor(marina);

        // when
        java.util.concurrent.CompletableFuture<Integer> first = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> statusOf(resetRequest(code, NEW_PASSWORD)));
        java.util.concurrent.CompletableFuture<Integer> second = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> statusOf(resetRequest(code, "OutraSenha2028")));

        // then
        assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(204, 400);
    }

    private int statusOf(MockHttpServletRequestBuilder request) {
        try {
            return mockMvc.perform(request).andReturn().getResponse().getStatus();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    @Test
    @DisplayName("mails the notice of the reset to the caretaker")
    void givenReset_whenItConcludes_thenMailTheNotice() throws Exception {
        // given
        Caretaker marina = saved(aUniqueCaretaker());
        String code = linkCodeFor(marina);

        // when
        reset(code, NEW_PASSWORD).andExpect(status().isNoContent());

        // then
        List<MimeMessage> received = awaitMessagesTo(marina.email().value(), 2);
        assertThat(received.getLast().getSubject()).isEqualTo("Sua senha do Ovyx foi redefinida");
    }

    @Test
    @DisplayName("annuls the pending link when the caretaker changes the own password from the menu")
    void givenPendingLink_whenChangingTheOwnPassword_thenRefuseTheLink() throws Exception {
        // given
        Caretaker marina = saved(aUniqueCaretaker());
        String code = linkCodeFor(marina);
        jakarta.servlet.http.Cookie[] session = sessionOf(marina);

        // when
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/me/password")
                        .cookie(session)
                        .with(sessions.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "%s", "newPassword": "%s"}
                                """.formatted(CaretakerTestDataBuilder.DEFAULT_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        // then
        expectInvalidLink(verify(code));
    }

    @Test
    @DisplayName("annuls the pending link when the caretaker is deactivated")
    void givenPendingLink_whenDeactivatingTheCaretaker_thenRefuseTheLink() throws Exception {
        // given
        Caretaker marina = saved(aUniqueCaretaker());
        String code = linkCodeFor(marina);
        IntegrationSessions.SignedIn administrator = sessions.administrator();

        // when
        mockMvc.perform(post("/api/v1/caretakers/" + marina.id().value() + "/deactivation")
                        .cookie(administrator.cookies())
                        .with(sessions.csrf()))
                .andExpect(status().is2xxSuccessful());

        // then
        expectInvalidLink(verify(code));
    }

    // ---------------------------------------------------------------- limites e rastro (US3)

    @Test
    @DisplayName("answers five requests for the same account alike, mailing only three links, of which the last holds")
    void givenFiveRequestsForTheSameAccount_whenSending_thenAnswerAllAlikeAndMailOnlyThree() throws Exception {
        // given
        Caretaker marina = saved(aUniqueCaretaker());
        String email = marina.email().value();

        // when
        for (int request = 0; request < 5; request++) {
            requestRecoveryFor(email).andExpect(status().isAccepted());
        }

        // then
        List<String> outcomes = outcomesFor(email, 5);
        assertThat(outcomes).filteredOn("RECOVERY_LINK_SENT"::equals).hasSize(3);
        assertThat(outcomes).filteredOn("RECOVERY_LIMITED"::equals).hasSize(2);
        List<MimeMessage> received = awaitMessagesTo(email, 3);
        assertThat(received).hasSize(3);
        String last = RecoveryMails.codeIn(received.getLast());
        verify(last).andExpect(status().isNoContent());
        for (MimeMessage older : received.subList(0, 2)) {
            String code = RecoveryMails.codeIn(older);
            if (!code.equals(last)) {
                expectInvalidLink(verify(code));
            }
        }
    }

    @Test
    @DisplayName("answers the eleventh request of an origin alike, mailing nothing for it")
    void givenTenRequestsFromAnOrigin_whenSendingTheEleventh_thenAnswerAlikeAndMailNothing() throws Exception {
        // given
        List<Caretaker> accounts = new java.util.ArrayList<>();
        for (int account = 0; account < 11; account++) {
            accounts.add(saved(aUniqueCaretaker()));
        }
        for (Caretaker account : accounts.subList(0, 10)) {
            requestRecoveryFor(account.email().value()).andExpect(status().isAccepted());
        }
        String eleventh = accounts.getLast().email().value();

        // when
        ResultActions response = requestRecoveryFor(eleventh);

        // then
        response.andExpect(status().isAccepted());
        assertThat(outcomesFor(eleventh, 1)).containsExactly("RECOVERY_THROTTLED");
        awaitMessagesTo(accounts.get(9).email().value(), 1);
        assertThat(messagesTo(eleventh)).isEmpty();
    }

    @Test
    @DisplayName("refuses even a valid link once an origin tried eleven links that hold nothing")
    void givenElevenUnknownLinksFromAnOrigin_whenVerifyingAValidLink_thenRefuseIt() throws Exception {
        // given
        String valid = linkCodeFor(saved(aUniqueCaretaker()));
        for (int attempt = 0; attempt < 11; attempt++) {
            expectInvalidLink(verify(java.util.UUID.randomUUID().toString().replace("-", "") + "abcdefghijk"));
        }

        // when
        ResultActions response = verify(valid);

        // then
        expectInvalidLink(response);
        assertThat(jdbc.queryForObject(
                        "select count(*) from access_event where origin = ? and outcome = 'RECOVERY_THROTTLED'",
                        Integer.class,
                        origin))
                .isPositive();
    }

    @Test
    @DisplayName("records each request, refusal and reset with the origin and the instant")
    void givenRecoveryFromStartToEnd_whenReadingTheAuditTrail_thenFindEveryStepWithOriginAndInstant() throws Exception {
        // given
        Caretaker marina = saved(aUniqueCaretaker());
        String email = marina.email().value();
        String code = linkCodeFor(marina);
        expectInvalidLink(verify("z".repeat(43)));
        reset(code, NEW_PASSWORD).andExpect(status().isNoContent());

        // when
        List<Map<String, Object>> events = jdbc.queryForList(
                "select attempted_identifier, outcome, origin, occurred_at from access_event where origin = ?"
                        + " order by occurred_at",
                origin);

        // then
        assertThat(events).extracting(event -> event.get("outcome"))
                .contains("RECOVERY_LINK_SENT", "RECOVERY_LINK_REFUSED", "PASSWORD_RECOVERED");
        assertThat(events).allSatisfy(event -> {
            assertThat(event.get("origin")).isEqualTo(origin);
            assertThat(event.get("occurred_at")).isNotNull();
        });
        assertThat(events).extracting(event -> event.get("attempted_identifier"))
                .contains(email, "(link de recuperação)");
    }

    @Test
    @DisplayName("never writes the code of a link nor the new password in the audit trail or in the log, even in DEBUG")
    void givenWholeRecovery_whenScanningTheAuditTrailAndTheLog_thenFindNoCodeNorPassword(
            org.springframework.boot.test.system.CapturedOutput output) throws Exception {
        // given
        // O Spring MVC registra o corpo desserializado em DEBUG: sem o toString que esconde, ligar o DEBUG para
        // investigar um incidente gravava o código e a senha no log. O nível volta ao normal no restoreWebLogLevel.
        loggingSystem.setLogLevel(WEB_LOGGER, org.springframework.boot.logging.LogLevel.DEBUG);
        Caretaker marina = saved(aUniqueCaretaker());
        String code = linkCodeFor(marina);
        // O JSON quebrado: a mensagem do parser trazia o valor lido.
        mockMvc.perform(fromOrigin(post(RESET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": %s, "newPassword": %s}
                                """.formatted(code, NEW_PASSWORD))
                        .with(sessions.csrf())))
                .andExpect(status().isBadRequest());
        verify(code).andExpect(status().isNoContent());
        reset(code, "curta").andExpect(status().isBadRequest());
        reset(code, NEW_PASSWORD).andExpect(status().isNoContent());
        awaitMessagesTo(marina.email().value(), 2);

        // when
        List<String> stored = jdbc.queryForList(
                "select attempted_identifier || ' ' || outcome || ' ' || origin from access_event where origin = ?",
                String.class,
                origin);

        // then
        assertThat(String.join("\n", stored)).doesNotContain(code).doesNotContain(NEW_PASSWORD);
        assertThat(output.getAll()).doesNotContain(code).doesNotContain(NEW_PASSWORD);
    }

    // ---------------------------------------------------------------- recusas da conferência e da redefinição (rodada 1)

    @Test
    @DisplayName("refuses the verification and the reset without the protection token")
    void givenNoCsrfToken_whenVerifyingOrResetting_thenRefuseAsCsrfTokenInvalid() throws Exception {
        // given
        String code = linkCodeFor(saved(aUniqueCaretaker()));

        // when
        ResultActions verification = mockMvc.perform(fromOrigin(post(VERIFICATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token": "%s"}
                        """.formatted(code))));
        ResultActions resetting = mockMvc.perform(fromOrigin(post(RESET)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token": "%s", "newPassword": "%s"}
                        """.formatted(code, NEW_PASSWORD))));

        // then
        verification.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
        resetting.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
        verify(code).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("refuses a body that is not JSON in the verification and in the reset")
    void givenPlainTextBody_whenVerifyingOrResetting_thenRefuseAsUnsupported() throws Exception {
        // given
        String body = "3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx";

        // when
        ResultActions verification = mockMvc.perform(fromOrigin(post(VERIFICATION)
                .contentType(MediaType.TEXT_PLAIN)
                .content(body)
                .with(sessions.csrf())));
        ResultActions resetting = mockMvc.perform(fromOrigin(post(RESET)
                .contentType(MediaType.TEXT_PLAIN)
                .content(body)
                .with(sessions.csrf())));

        // then
        verification.andExpect(status().isUnsupportedMediaType());
        resetting.andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("lets a session that owes the provisional password verify a link and reset by it")
    void givenSessionOwingThePasswordChange_whenVerifyingAndResetting_thenAcceptBoth() throws Exception {
        // given
        IntegrationSessions.SignedIn owing = sessions.commonUserOwingThePasswordChange();
        String code = linkCodeFor(saved(aUniqueCaretaker()));

        // when
        ResultActions verification = mockMvc.perform(fromOrigin(post(VERIFICATION)
                .cookie(owing.cookies())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token": "%s"}
                        """.formatted(code))
                .with(sessions.csrf())));
        ResultActions resetting = mockMvc.perform(resetRequest(code, NEW_PASSWORD).cookie(owing.cookies()));

        // then
        verification.andExpect(status().isNoContent());
        resetting.andExpect(status().isNoContent());
    }
}
