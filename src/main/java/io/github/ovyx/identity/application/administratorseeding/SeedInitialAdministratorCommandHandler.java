package io.github.ovyx.identity.application.administratorseeding;

import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.domain.DomainException;

import java.time.Clock;

/**
 * Cria o administrador inicial quando o sistema nao tem nenhum administrador ativo.
 *
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

        Caretaker administrator;
        try {
            administrator = Caretaker.register(
                command.fullName(),
                command.cpf(),
                command.email(),
                command.mobilePhone(),
                command.password(),
                Role.ADMINISTRATOR,
                true,
                passwordHasher,
                clock
            );
        } catch (DomainException violation) {
            // As violacoes nao incluem a senha: details carrega campo e mensagem fixa.
            return Result.failure(ApplicationError.from(violation, ErrorType.VALIDATION));
        }

        caretakerRepository.save(administrator);
        return Result.success(SeedingOutcome.CREATED);
    }
}
