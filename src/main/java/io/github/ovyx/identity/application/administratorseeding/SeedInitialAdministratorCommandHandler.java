package io.github.ovyx.identity.application.administratorseeding;

import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.identity.domain.valueobject.Cpf;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.domain.DomainException;
import io.github.ovyx.shared.domain.Notification;

import java.time.Clock;
import java.util.Optional;

/**
 * Garante um administrador ativo na subida da aplicacao (FR-025).
 *
 * <p>Quando nao ha nenhum, o CPF configurado decide: se alguem ja o tem — em geral o proprio
 * administrador inicial, inativado —, essa pessoa volta a administrar; senao, o administrador
 * inicial e cadastrado. Nos dois casos, com a senha provisoria configurada e a troca obrigatoria
 * no primeiro acesso.
 */
public class SeedInitialAdministratorCommandHandler implements CommandHandler<SeedInitialAdministratorCommand, SeedingOutcome> {

    /**
     * Codigo da recusa quando a senha obrigatoria nao foi informada.
     */
    public static final String PASSWORD_REQUIRED = IdentityErrorCode.ADMINISTRATOR_PASSWORD_REQUIRED.code();

    private final CaretakerRepository caretakerRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public SeedInitialAdministratorCommandHandler(CaretakerRepository caretakerRepository, PasswordHasher passwordHasher, Clock clock) {
        this.caretakerRepository = caretakerRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    public Result<SeedingOutcome> handle(SeedInitialAdministratorCommand command) {
        if (caretakerRepository.countActiveAdministrators() > 0) {
            return Result.success(SeedingOutcome.NOT_NEEDED);
        }

        if (command.password() == null || command.password().isBlank()) {
            return Result.failure(ApplicationError.of(
                ErrorType.VALIDATION,
                PASSWORD_REQUIRED,
                "Não há administrador ativo e a senha do administrador inicial não foi informada."));
        }

        Optional<Caretaker> holderOfTheCpf;
        Caretaker administrator;
        try {
            holderOfTheCpf = holderOf(command.cpf());
            if (holderOfTheCpf.isPresent()) {
                administrator = holderOfTheCpf.get();
                administrator.restoreAsInitialAdministrator(command.password(), passwordHasher, caretakerRepository, clock);
            } else {
                administrator = Caretaker.register(
                    command.fullName(),
                    command.cpf(),
                    command.email(),
                    command.mobilePhone(),
                    command.password(),
                    Role.ADMINISTRATOR,
                    true,
                    passwordHasher,
                    caretakerRepository,
                    clock
                );
            }
        } catch (DomainException violation) {
            // As violacoes nao incluem a senha: details carrega campo e mensagem fixa. Um e-mail ou
            // celular ja usado por um responsavel comum ativo e conflito, e nao dado invalido.
            ErrorType type = IdentityErrorCode.VALIDATION_FAILED.equals(violation.errorCode())
                ? ErrorType.VALIDATION
                : ErrorType.CONFLICT;
            return Result.failure(ApplicationError.from(violation, type));
        }

        caretakerRepository.save(administrator);
        return Result.success(holderOfTheCpf.isPresent() ? SeedingOutcome.RESTORED : SeedingOutcome.CREATED);
    }

    /**
     * Quem ja tem o CPF configurado. Com o CPF invalido, ninguem: o cadastro e que recusa, com todas
     * as violacoes da configuracao de uma vez.
     */
    private Optional<Caretaker> holderOf(String rawCpf) {
        Notification cpfCheck = new Notification();
        Cpf.validate(rawCpf, cpfCheck);
        return cpfCheck.hasErrors() ? Optional.empty() : caretakerRepository.findByCpf(Cpf.of(rawCpf));
    }
}
