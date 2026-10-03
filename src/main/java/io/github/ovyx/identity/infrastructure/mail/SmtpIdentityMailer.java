package io.github.ovyx.identity.infrastructure.mail;

import io.github.ovyx.identity.application.recovery.IdentityMailer;
import io.github.ovyx.identity.application.recovery.PasswordRecoveredMail;
import io.github.ovyx.identity.application.recovery.RecoveryLinkMail;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.util.HtmlUtils;

/**
 * Os e-mails do identity por SMTP (R-006 e R-010 da 012).
 *
 * <p>Cada mensagem vai em HTML e em texto puro, montada a partir dos modelos de {@code resources/mail/}. Os valores
 * que vem do cadastro (o nome) sao escapados no HTML: um nome com marcacao aparece como texto.
 *
 * <p>O envio tenta duas vezes. Falhando, devolve {@code false} e registra no log o assunto e a causa de fundo. A
 * mensagem da causa so entra quando e de rede (conexao recusada, tempo esgotado): a recusa do servidor de envio repete o
 * destinatario. Nunca entram o link nem o corpo da mensagem: o codigo do link e o segredo da conta (FR-018).
 */
public class SmtpIdentityMailer implements IdentityMailer {

    private static final Logger LOG = LoggerFactory.getLogger(SmtpIdentityMailer.class);

    static final int ATTEMPTS = 2;
    private static final long PAUSE_BETWEEN_ATTEMPTS_MILLIS = 1_000;

    private static final String RECOVERY_SUBJECT = "Redefina sua senha do Ovyx";
    private static final String RECOVERED_SUBJECT = "Sua senha do Ovyx foi redefinida";
    private static final String RESET_PATH = "/redefinir-senha#";

    /** O nome que a caixa de entrada mostra no remetente. */
    private static final String SENDER_NAME = "Ovyx";

    private final JavaMailSender sender;
    private final OvyxMailProperties properties;
    private final DateTimeFormatter instantFormat;

    private final String recoveryHtml = template("password-recovery.html");
    private final String recoveryText = template("password-recovery.txt");
    private final String recoveredHtml = template("password-recovered.html");
    private final String recoveredText = template("password-recovered.txt");

    public SmtpIdentityMailer(JavaMailSender sender, OvyxMailProperties properties, ZoneId farmZone) {
        this.sender = sender;
        this.properties = properties;
        this.instantFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").withZone(farmZone);
    }

    @Override
    public boolean sendRecoveryLink(RecoveryLinkMail mail) {
        String link = properties.appUrl() + RESET_PATH + mail.token().value();
        Map<String, String> values = Map.of("name", mail.fullName(), "link", link);
        return send(mail.to(), RECOVERY_SUBJECT, fill(recoveryText, values, false), fill(recoveryHtml, values, true));
    }

    @Override
    public boolean sendPasswordRecoveredNotice(PasswordRecoveredMail mail) {
        Map<String, String> values =
                Map.of("name", mail.fullName(), "when", instantFormat.format(mail.recoveredAt()));
        return send(mail.to(), RECOVERED_SUBJECT, fill(recoveredText, values, false), fill(recoveredHtml, values, true));
    }

    private boolean send(String to, String subject, String text, String html) {
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                MimeMessage message = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
                helper.setFrom(properties.from(), SENDER_NAME);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(text, html);
                sender.send(message);
                return true;
            } catch (MailException | MessagingException | UnsupportedEncodingException failure) {
                if (attempt == ATTEMPTS) {
                    // A causa de fundo diz o que corrigir. So a de rede leva a mensagem: a recusa do servidor de
                    // envio ("550 ... <endereco>") repete o destinatario.
                    Throwable cause = NestedExceptionUtils.getMostSpecificCause(failure);
                    LOG.warn(
                            "E-mail \"{}\" nao enviado depois de {} tentativas ({}: {})",
                            subject,
                            ATTEMPTS,
                            cause.getClass().getSimpleName(),
                            cause instanceof IOException ? cause.getMessage() : "mensagem do servidor omitida");
                    return false;
                }
                pause();
            }
        }
        return false;
    }

    private static void pause() {
        try {
            Thread.sleep(PAUSE_BETWEEN_ATTEMPTS_MILLIS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /** Troca cada {@code {{chave}}} do modelo pelo valor, escapado no HTML. */
    private static String fill(String template, Map<String, String> values, boolean html) {
        String filled = template;
        for (Map.Entry<String, String> value : values.entrySet()) {
            String text = html ? HtmlUtils.htmlEscape(value.getValue(), StandardCharsets.UTF_8.name()) : value.getValue();
            filled = filled.replace("{{" + value.getKey() + "}}", text);
        }
        return filled;
    }

    private static String template(String name) {
        try (InputStream content = new ClassPathResource("mail/" + name).getInputStream()) {
            return new String(content.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException missing) {
            throw new UncheckedIOException("Modelo de e-mail ausente: mail/" + name, missing);
        }
    }
}
