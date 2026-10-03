package io.github.ovyx.identity.integration;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lê o código do link de recuperação do e-mail recebido pelo servidor de testes, como a pessoa o leria. */
final class RecoveryMails {

    private static final Pattern LINK = Pattern.compile("/redefinir-senha#([A-Za-z0-9_-]{43})");

    private RecoveryMails() {}

    /** O código do link, tirado da versão em texto do e-mail. */
    static String codeIn(Part message) throws Exception {
        Matcher matcher = LINK.matcher(textOf(message));
        if (!matcher.find()) {
            throw new AssertionError("o e-mail não traz o link de recuperação");
        }
        return matcher.group(1);
    }

    private static String textOf(Part part) throws Exception {
        if (part.isMimeType("text/plain")) {
            return (String) part.getContent();
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart child = multipart.getBodyPart(i);
                text.append(textOf(child));
            }
            return text.toString();
        }
        return "";
    }
}
