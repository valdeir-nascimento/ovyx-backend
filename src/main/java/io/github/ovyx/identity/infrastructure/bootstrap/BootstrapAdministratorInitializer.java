package io.github.ovyx.identity.infrastructure.bootstrap;

import io.github.ovyx.identity.application.administratorseeding.SeedInitialAdministratorCommand;
import io.github.ovyx.identity.application.administratorseeding.SeedInitialAdministratorCommandHandler;
import io.github.ovyx.identity.application.administratorseeding.SeedingReport;
import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.shared.application.Dispatcher;
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
 * <p>O comando passa pelo {@link Dispatcher}, como qualquer outro, e nao direto ao tratador: e ele
 * que abre a transacao e repete o comando numa escrita concorrente. Com duas instancias subindo
 * juntas sobre um banco sem administrador ativo, nao ha linha para a contagem travar: quem segura a
 * corrida do cadastro e o indice unico do CPF, e a segunda instancia, na nova tentativa, encontra o
 * administrador que a primeira acabou de gravar. Na restauracao, a versao da linha recusa a
 * segunda gravacao, e a nova tentativa tambem encontra o administrador ja restaurado.
 *
 * <p>Falha na semeadura impede a subida. Um sistema sem administrador e sem como criar um nao tem
 * por onde ser acessado, e descobrir isso na tela de login e pior do que numa mensagem clara no
 * log de inicializacao.
 */
@Component
public class BootstrapAdministratorInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdministratorInitializer.class);

    private final Dispatcher dispatcher;
    private final BootstrapAdministratorProperties properties;

    BootstrapAdministratorInitializer(Dispatcher dispatcher, BootstrapAdministratorProperties properties) {
        this.dispatcher = dispatcher;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        Result<SeedingReport> result = dispatcher.dispatch(new SeedInitialAdministratorCommand(
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
                    "Não foi possível criar nem restaurar o administrador inicial. Corrija a configuração em "
                            + "ovyx.bootstrap.administrator. Violações: " + result.error().details());
        }

        SeedingReport report = result.value();
        switch (report.outcome()) {
            case CREATED -> log.info("Administrador inicial criado com o e-mail {}.", report.email());
            // Sem o CPF no log: e dado pessoal, e o operador ja sabe qual configurou. O e-mail entra
            // porque o restaurado mantem o dele, que pode nao ser o configurado.
            case RESTORED -> log.warn(
                    "Não havia administrador ativo: o responsável com o CPF configurado, antes {} e {}, voltou a "
                            + "administrar. Ele entra com o e-mail {} e a senha provisória, e troca a senha no primeiro "
                            + "acesso.",
                    describe(report.previousRole()),
                    describe(report.previousStatus()),
                    report.email());
            case NOT_NEEDED -> log.debug("Já existe administrador ativo; a semeadura não é necessária.");
        }
    }

    private static String describe(Role role) {
        return role == Role.ADMINISTRATOR ? "administrador" : "usuário comum";
    }

    private static String describe(CaretakerStatus status) {
        return status == CaretakerStatus.ACTIVE ? "ativo" : "inativo";
    }
}
