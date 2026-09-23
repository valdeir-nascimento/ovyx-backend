package io.github.ovyx.identity.application.authentication;

import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.port.AccessEventRecorder;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.identity.domain.port.SignInThrottle;
import io.github.ovyx.identity.domain.valueobject.AccessIdentifier;
import io.github.ovyx.identity.domain.valueobject.PasswordHash;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.ErrorType;

import java.time.Clock;
import java.util.Optional;

public class SignInCommandHandler implements CommandHandler<SignInCommand, CaretakerId> {

    /**
     * Senha usada so para gerar o hash de equalizacao de custo. O valor e irrelevante: o resultado
     * da comparacao contra ele e sempre descartado.
     */
    private static final String COST_EQUALIZER = "ovyx-cost-equalizer-2026";

    /**
     * Registrado na auditoria quando nada foi digitado, ja que o evento exige um identificador.
     */
    private static final String BLANK_IDENTIFIER = "(vazio)";

    private final CaretakerRepository caretakerRepository;
    private final PasswordHasher passwordHasher;
    private final AccessEventRecorder accessEventRecorder;
    private final SignInThrottle signInThrottle;
    private final Clock clock;

    /**
     * Hash com os mesmos parametros dos reais, calculado uma vez, para igualar o custo das falhas.
     */
    private final PasswordHash costEqualizerHash;

    public SignInCommandHandler(
        CaretakerRepository caretakerRepository,
        PasswordHasher passwordHasher,
        AccessEventRecorder accessEventRecorder,
        SignInThrottle signInThrottle,
        Clock clock
    ) {
        this.caretakerRepository = caretakerRepository;
        this.passwordHasher = passwordHasher;
        this.accessEventRecorder = accessEventRecorder;
        this.signInThrottle = signInThrottle;
        this.clock = clock;
        this.costEqualizerHash = passwordHasher.hash(COST_EQUALIZER);
    }

    @Override
    public Result<CaretakerId> handle(SignInCommand command) {
        String typed = command.identifier();
        String key = AccessIdentifier.of(typed).value();
        String origin = command.origin();

        // Sem identificador nao ha chave de contencao nem conta a buscar. A borda HTTP ja exige o
        // campo, mas o tratador nao pode depender disso: sem este desvio, o evento de auditoria
        // lancaria excecao, e excecao nao e canal de negocio (principio IV).
        if (key.isEmpty()) {
            payHashCost(command.password());
            accessEventRecorder.record(AccessEvent.invalidCredentials(BLANK_IDENTIFIER, null, origin, clock));
            return invalidCredentials();
        }

        // A busca vem antes da contencao so para que a auditoria saiba de quem e a conta atacada.
        // Nada disso chega ao cliente: a resposta e o custo continuam identicos em todo caminho.
        Optional<Caretaker> found = caretakerRepository.findByEmailOrMobilePhone(key);
        CaretakerId knownCaretaker = found.map(Caretaker::id).orElse(null);

        if (signInThrottle.isBlocked(key, origin)) {
            payHashCost(command.password());
            accessEventRecorder.record(AccessEvent.throttled(typed, knownCaretaker, origin, clock));
            return invalidCredentials();
        }

        if (found.isEmpty()) {
            payHashCost(command.password());
            signInThrottle.registerFailure(key, origin);
            accessEventRecorder.record(AccessEvent.invalidCredentials(typed, null, origin, clock));
            return invalidCredentials();
        }

        Caretaker caretaker = found.get();

        // authenticate calcula o hash sempre, inclusive para inativo, e so depois considera a
        // situacao. Por isso a checagem de inativo vem DEPOIS desta linha, e nao antes.
        boolean authenticated = caretaker.authenticate(command.password(), passwordHasher);

        if (!caretaker.isActive()) {
            signInThrottle.registerFailure(key, origin);
            accessEventRecorder.record(AccessEvent.inactiveCaretaker(typed, caretaker.id(), origin, clock));
            return invalidCredentials();
        }

        if (!authenticated) {
            signInThrottle.registerFailure(key, origin);
            accessEventRecorder.record(AccessEvent.invalidCredentials(typed, caretaker.id(), origin, clock));
            return invalidCredentials();
        }

        signInThrottle.clear(key, origin);
        accessEventRecorder.record(AccessEvent.granted(typed, caretaker.id(), origin, clock));
        return Result.success(caretaker.id());
    }

    /**
     * Executa uma verificacao de hash cujo resultado e descartado, so pelo custo.
     */
    private void payHashCost(String password) {
        passwordHasher.matches(password, costEqualizerHash);
    }

    /**
     * A unica falha que este caso de uso produz.
     *
     * <p>Um unico ponto de construcao garante que os caminhos sejam indistinguiveis — inclusive
     * depois de alguem mexer em um deles sem lembrar dos outros.
     */
    private Result<CaretakerId> invalidCredentials() {
        return Result.failure(ApplicationError.of(
            ErrorType.UNAUTHENTICATED,
            IdentityErrorCode.INVALID_CREDENTIALS.code(),
            "E-mail, celular ou senha inválidos."));
    }
}
