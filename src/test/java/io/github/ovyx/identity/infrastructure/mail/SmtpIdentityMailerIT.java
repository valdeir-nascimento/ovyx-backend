package io.github.ovyx.identity.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.application.recovery.IdentityMailer;
import io.github.ovyx.identity.application.recovery.PasswordRecoveredMail;
import io.github.ovyx.identity.application.recovery.RecoveryLinkMail;
import io.github.ovyx.identity.domain.valueobject.RecoveryToken;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * O envio dos e-mails do identity (R-006 e R-010 da 012), contra um servidor SMTP de verdade, em processo: o e-mail
 * do link de recuperação e o aviso da redefinição, em HTML e em texto, e a falha do servidor.
 */
@DisplayName("SmtpIdentityMailer")
@ExtendWith(OutputCaptureExtension.class)
class SmtpIdentityMailerIT extends IntegrationTestSupport {

    private static final String CODE = "3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx";
    private static final String LINK = APP_URL + "/redefinir-senha#" + CODE;
    private static final String TO = "marina.costa@ovyx.com.br";

    @Autowired
    private IdentityMailer mailer;

    @BeforeEach
    void emptyTheMailbox() throws Exception {
        MAIL.purgeEmailFromAllMailboxes();
    }

    private static MimeMessage onlyMessage() {
        assertThat(MAIL.waitForIncomingEmail(5_000, 1)).isTrue();
        MimeMessage[] received = MAIL.getReceivedMessages();
        assertThat(received).hasSize(1);
        return received[0];
    }

    /** As partes de texto da mensagem, pelo tipo: o texto puro e o HTML. */
    private static List<String> partsOf(Part part, String mimeType) throws Exception {
        List<String> found = new ArrayList<>();
        if (part.isMimeType(mimeType)) {
            found.add((String) part.getContent());
        } else if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart child = multipart.getBodyPart(i);
                found.addAll(partsOf(child, mimeType));
            }
        }
        return found;
    }

    private static String html(MimeMessage message) throws Exception {
        List<String> parts = partsOf(message, "text/html");
        assertThat(parts).hasSize(1);
        return parts.getFirst();
    }

    private static String text(MimeMessage message) throws Exception {
        List<String> parts = partsOf(message, "text/plain");
        assertThat(parts).hasSize(1);
        return parts.getFirst();
    }

    private static RecoveryLinkMail recoveryLink(String fullName) {
        return new RecoveryLinkMail(TO, fullName, RecoveryToken.of(CODE), Instant.parse("2026-10-02T12:30:00Z"));
    }

    @Test
    @DisplayName("sends the recovery link to the caretaker, from the configured sender")
    void givenRecoveryLink_whenSending_thenDeliverItToTheCaretakerFromTheConfiguredSender() throws Exception {
        // given
        RecoveryLinkMail mail = recoveryLink("Marina Costa");

        // when
        boolean accepted = mailer.sendRecoveryLink(mail);

        // then
        assertThat(accepted).isTrue();
        MimeMessage message = onlyMessage();
        assertThat(message.getSubject()).isEqualTo("Redefina sua senha do Ovyx");
        assertThat(message.getFrom()).extracting(address -> ((InternetAddress) address).getAddress()).containsExactly(MAIL_FROM);
        assertThat(message.getFrom()).extracting(address -> ((InternetAddress) address).getPersonal()).containsExactly("Ovyx");
        assertThat(message.getRecipients(Message.RecipientType.TO))
                .extracting(address -> ((InternetAddress) address).getAddress())
                .containsExactly(TO);
    }

    @Test
    @DisplayName("writes the link email in HTML and in plain text, with the name, the link, the validity and the advice")
    void givenRecoveryLink_whenSending_thenWriteBothVersionsWithTheNameTheLinkTheValidityAndTheAdvice()
            throws Exception {
        // given
        RecoveryLinkMail mail = recoveryLink("Marina Costa");

        // when
        mailer.sendRecoveryLink(mail);

        // then
        MimeMessage message = onlyMessage();
        String html = html(message);
        String text = text(message);
        assertThat(html)
                .contains("Marina Costa")
                .contains("href=\"" + LINK + "\"")
                .contains("Definir nova senha")
                .contains("Este link vale por 30 minutos.")
                .contains("ignore este e-mail")
                .doesNotContain("<script");
        assertThat(text)
                .contains("Marina Costa")
                .contains(LINK)
                .contains("Este link vale por 30 minutos.")
                .contains("ignore este e-mail");
    }

    @Test
    @DisplayName("shows the link written out in the HTML too, for whoever cannot click it")
    void givenRecoveryLink_whenSending_thenWriteTheLinkOutInTheHtml() throws Exception {
        // given
        RecoveryLinkMail mail = recoveryLink("Marina Costa");

        // when
        mailer.sendRecoveryLink(mail);

        // then
        String html = html(onlyMessage());
        assertThat(html.indexOf(LINK)).isNotEqualTo(html.lastIndexOf(LINK));
    }

    @Test
    @DisplayName("escapes the name in the HTML, so it shows as text")
    void givenNameWithMarkup_whenSending_thenEscapeItInTheHtml() throws Exception {
        // given
        RecoveryLinkMail mail = recoveryLink("<b>Marina</b> & Cia");

        // when
        mailer.sendRecoveryLink(mail);

        // then
        String html = html(onlyMessage());
        assertThat(html).contains("&lt;b&gt;Marina&lt;/b&gt; &amp; Cia").doesNotContain("<b>Marina</b>");
    }

    @Test
    @DisplayName("sends the notice of the reset with the instant in the farm time zone and the advice")
    void givenPasswordRecovered_whenSendingTheNotice_thenWriteTheInstantInTheFarmTimeZoneAndTheAdvice()
            throws Exception {
        // given
        PasswordRecoveredMail mail =
                new PasswordRecoveredMail(TO, "Marina Costa", Instant.parse("2026-10-02T12:05:00Z"));

        // when
        boolean accepted = mailer.sendPasswordRecoveredNotice(mail);

        // then
        assertThat(accepted).isTrue();
        MimeMessage message = onlyMessage();
        assertThat(message.getSubject()).isEqualTo("Sua senha do Ovyx foi redefinida");
        assertThat(html(message))
                .contains("Marina Costa")
                .contains("02/10/2026 às 09:05")
                .contains("Se não foi você, procure o administrador do Ovyx.");
        assertThat(text(message))
                .contains("02/10/2026 às 09:05")
                .contains("Se não foi você, procure o administrador do Ovyx.");
    }

    @Test
    @DisplayName("logs the refusal of the server without the recipient it repeats")
    void givenServerThatRefusesTheRecipient_whenSending_thenLogWithoutTheRecipient(CapturedOutput output) {
        // given
        org.springframework.mail.javamail.JavaMailSender refusing = new JavaMailSenderImpl() {
            @Override
            public void send(MimeMessage message) {
                throw new org.springframework.mail.MailSendException(
                        "550 5.1.1 <" + TO + ">: Recipient address rejected");
            }
        };
        SmtpIdentityMailer mailerRefused =
                new SmtpIdentityMailer(refusing, new OvyxMailProperties(MAIL_FROM, APP_URL), ZoneId.of("America/Sao_Paulo"));

        // when
        boolean accepted = mailerRefused.sendRecoveryLink(recoveryLink("Marina Costa"));

        // then
        assertThat(accepted).isFalse();
        assertThat(output.getAll()).contains("MailSendException").doesNotContain(TO).doesNotContain(CODE);
    }

    @Test
    @DisplayName("gives up after two attempts when the server is down, without the code in the log")
    void givenServerDown_whenSending_thenReturnFalseWithoutTheCodeInTheLog(CapturedOutput output) throws IOException {
        // given
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        JavaMailSenderImpl down = new JavaMailSenderImpl();
        down.setHost("127.0.0.1");
        down.setPort(closedPort);
        SmtpIdentityMailer mailerToNowhere =
                new SmtpIdentityMailer(down, new OvyxMailProperties(MAIL_FROM, APP_URL), ZoneId.of("America/Sao_Paulo"));

        // when
        boolean accepted = mailerToNowhere.sendRecoveryLink(recoveryLink("Marina Costa"));

        // then
        assertThat(accepted).isFalse();
        assertThat(output.getAll()).contains("2 tentativas").contains("ConnectException").doesNotContain(CODE);
    }
}
