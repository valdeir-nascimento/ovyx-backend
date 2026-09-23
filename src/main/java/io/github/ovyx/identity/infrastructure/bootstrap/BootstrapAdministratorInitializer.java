package io.github.ovyx.identity.infrastructure.bootstrap;

import io.github.ovyx.identity.application.administratorseeding.SeedInitialAdministratorCommand;
import io.github.ovyx.identity.application.administratorseeding.SeedInitialAdministratorCommandHandler;
import io.github.ovyx.identity.application.administratorseeding.SeedingOutcome;
import io.github.ovyx.shared.application.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Executa a semeadura do administrador inicial na subida da aplicacao (FR-025).
 *
 * <p>A regra — quando criar, com que dados, com a troca de senha obrigatoria — vive no caso de uso
 * {@link SeedInitialAdministratorCommandHandler}. Aqui fica so o que e da subida: ler a
 * configuracao e decidir o que fazer com o {@code Result}.
 *
 * <p>Falha na semeadura impede a subida. Um sistema sem administrador e sem como criar um nao tem
 * por onde ser acessado, e descobrir isso na tela de login e pior do que numa mensagem clara no
 * log de inicializacao.
 */
@Component
public class BootstrapAdministratorInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdministratorInitializer.class);

    private final SeedInitialAdministratorCommandHandler seedInitialAdministratorCommandHandler;
    private final BootstrapAdministratorProperties properties;

    BootstrapAdministratorInitializer(
            SeedInitialAdministratorCommandHandler seedInitialAdministratorCommandHandler,
            BootstrapAdministratorProperties properties) {
        this.seedInitialAdministratorCommandHandler = seedInitialAdministratorCommandHandler;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        Result<SeedingOutcome> result = seedInitialAdministratorCommandHandler.handle(new SeedInitialAdministratorCommand(
                properties.fullName(),
                properties.cpf(),
                properties.email(),
                properties.mobilePhone(),
                properties.password()));

        if (result.isFailure()) {
            if (SeedInitialAdministratorCommandHandler.PASSWORD_REQUIRED.equals(result.error().code())) {
                throw new IllegalStateException(
                        "Não há administrador ativo e a variável OVYX_BOOTSTRAP_ADMIN_PASSWORD não foi definida. "
                                + "Defina-a para criar o administrador inicial; a senha deverá ser trocada no primeiro acesso.");
            }
            // As violacoes nao incluem a senha: details carrega campo e mensagem fixa.
            throw new IllegalStateException(
                    "Não foi possível criar o administrador inicial. Corrija a configuração em "
                            + "ovyx.bootstrap.administrator. Violações: " + result.error().details());
        }

        if (result.value() == SeedingOutcome.CREATED) {
            log.info("Administrador inicial criado com o e-mail {}.", properties.email());
        } else {
            log.debug("Já existe administrador ativo; a semeadura não é necessária.");
        }
    }
}
