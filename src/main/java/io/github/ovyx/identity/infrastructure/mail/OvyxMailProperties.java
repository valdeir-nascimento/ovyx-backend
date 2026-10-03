package io.github.ovyx.identity.infrastructure.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * O remetente dos e-mails do Ovyx e o endereco da aplicacao que os links abrem (R-006 da 012).
 *
 * <p>Os dois sao obrigatorios: sem eles a aplicacao recusa subir, e a mensagem diz qual falta. O perfil {@code dev}
 * os preenche para o Mailpit; nenhum outro ambiente tem valor no codigo (FR-019).
 *
 * @param from o remetente, como {@code ovyx@granja.com.br}
 * @param appUrl o endereco da aplicacao, sem a barra final, como {@code https://ovyx.granja.com.br}
 */
@ConfigurationProperties(prefix = "ovyx.mail")
public record OvyxMailProperties(String from, String appUrl) {

    public OvyxMailProperties {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException(
                    "ovyx.mail.from precisa ser informado (OVYX_MAIL_FROM): o remetente dos e-mails do Ovyx");
        }
        if (appUrl == null || appUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "ovyx.mail.app-url precisa ser informado (OVYX_APP_URL): o endereço que os links dos e-mails abrem");
        }
        appUrl = appUrl.strip().replaceAll("/+$", "");
    }
}
