package io.github.ovyx.identity.infrastructure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Dados do administrador inicial (FR-025).
 *
 * <p>A senha <strong>nao</strong> tem valor padrao, de proposito: senha fixa publicada em codigo ou
 * em documentacao seria a mesma em toda instalacao, e FR-025 proibe. A variavel de ambiente
 * {@code OVYX_BOOTSTRAP_ADMIN_PASSWORD} e obrigatoria em <strong>qualquer</strong> perfil, inclusive
 * no de desenvolvimento, enquanto nao houver administrador ativo: sem ela a aplicacao recusa subir.
 *
 * <p>Os demais campos tem padrao apenas para que o ambiente de desenvolvimento suba sem
 * configuracao. Em qualquer outro ambiente, devem ser informados.
 */
@ConfigurationProperties(prefix = "ovyx.bootstrap.administrator")
public record BootstrapAdministratorProperties(
        @DefaultValue("Administrador do Sistema") String fullName,
        @DefaultValue("52998224725") String cpf,
        @DefaultValue("admin@ovyx.com.br") String email,
        @DefaultValue("11999999999") String mobilePhone,
        @DefaultValue("") String password) {

    /** Nunca inclui a senha: o toString gerado para records a exporia em qualquer log (FR-021). */
    @Override
    public String toString() {
        return "BootstrapAdministratorProperties[fullName=" + fullName + ", cpf=" + cpf + ", email=" + email
                + ", mobilePhone=" + mobilePhone + ", password=****]";
    }
}
