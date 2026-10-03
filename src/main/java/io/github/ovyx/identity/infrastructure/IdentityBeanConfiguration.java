package io.github.ovyx.identity.infrastructure;

import io.github.ovyx.identity.application.account.ChangeOwnThemeCommandHandler;
import io.github.ovyx.identity.application.account.ChangeOwnPasswordCommandHandler;
import io.github.ovyx.identity.application.caretaker.CaretakerDirectory;
import io.github.ovyx.identity.application.caretaker.DeactivateCaretakerCommandHandler;
import io.github.ovyx.identity.application.caretaker.FindCaretakerByIdQueryHandler;
import io.github.ovyx.identity.application.caretaker.RegisterCaretakerCommandHandler;
import io.github.ovyx.identity.application.caretaker.SearchCaretakersQueryHandler;
import io.github.ovyx.identity.application.caretaker.UpdateCaretakerCommandHandler;
import io.github.ovyx.identity.application.administratorseeding.SeedInitialAdministratorCommandHandler;
import io.github.ovyx.identity.application.authentication.SignInCommandHandler;
import io.github.ovyx.identity.application.authentication.SignOutCommandHandler;
import io.github.ovyx.identity.application.authentication.CaretakerReadModels;
import io.github.ovyx.identity.application.authentication.GetAuthenticatedCaretakerQueryHandler;
import io.github.ovyx.identity.application.recovery.IdentityMailer;
import io.github.ovyx.identity.application.recovery.IssuePasswordRecoveryCommandHandler;
import io.github.ovyx.identity.application.recovery.NotifyPasswordRecoveredCommandHandler;
import io.github.ovyx.identity.application.recovery.RecoverPasswordCommandHandler;
import io.github.ovyx.identity.application.recovery.VerifyRecoveryLinkCommandHandler;
import io.github.ovyx.identity.application.recovery.RequestPasswordRecoveryCommandHandler;
import io.github.ovyx.identity.application.recovery.SendRecoveryLinkCommandHandler;
import io.github.ovyx.identity.domain.port.AccessEventRecorder;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.identity.domain.port.RecoveryThrottle;
import io.github.ovyx.identity.domain.port.RecoveryTokens;
import io.github.ovyx.identity.domain.port.SignInThrottle;
import io.github.ovyx.shared.application.DeferredCommands;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fiacao dos tratadores do contexto Identity.
 *
 * <p>Os tratadores sao classes comuns, sem {@code @Component} e sem qualquer anotacao: a camada
 * {@code application} e livre de framework (principio I), e por isso quem os instancia e esta
 * configuracao, que vive em {@code infrastructure}.
 *
 * <p>O efeito colateral util e que cada tratador pode ser construido em teste com um {@code new},
 * sem subir contexto do Spring — e o que torna os testes de caso de uso rapidos.
 */
@Configuration
public class IdentityBeanConfiguration {

    @Bean
    SignInCommandHandler signInCommandHandler(
            CaretakerRepository caretakerRepository,
            PasswordHasher passwordHasher,
            AccessEventRecorder accessEventRecorder,
            SignInThrottle signInThrottle,
            Clock clock) {
        return new SignInCommandHandler(caretakerRepository, passwordHasher, accessEventRecorder, signInThrottle, clock);
    }

    @Bean
    SignOutCommandHandler signOutCommandHandler(
            CaretakerRepository caretakerRepository, AccessEventRecorder accessEventRecorder, Clock clock) {
        return new SignOutCommandHandler(caretakerRepository, accessEventRecorder, clock);
    }

    @Bean
    ChangeOwnPasswordCommandHandler changeOwnPasswordCommandHandler(
            CaretakerRepository caretakerRepository, PasswordHasher passwordHasher, Clock clock) {
        return new ChangeOwnPasswordCommandHandler(caretakerRepository, passwordHasher, clock);
    }

    @Bean
    ChangeOwnThemeCommandHandler changeOwnThemeCommandHandler(CaretakerRepository caretakerRepository) {
        return new ChangeOwnThemeCommandHandler(caretakerRepository);
    }

    // ---------------------------------------------------------------- recuperacao de senha (feature 012)

    @Bean
    RequestPasswordRecoveryCommandHandler requestPasswordRecoveryCommandHandler(
            RecoveryThrottle recoveryThrottle,
            AccessEventRecorder accessEventRecorder,
            DeferredCommands deferredCommands,
            Clock clock) {
        return new RequestPasswordRecoveryCommandHandler(
                recoveryThrottle, accessEventRecorder, deferredCommands, clock);
    }

    @Bean
    IssuePasswordRecoveryCommandHandler issuePasswordRecoveryCommandHandler(
            CaretakerRepository caretakerRepository,
            RecoveryTokens recoveryTokens,
            AccessEventRecorder accessEventRecorder,
            DeferredCommands deferredCommands,
            Clock clock) {
        return new IssuePasswordRecoveryCommandHandler(
                caretakerRepository, recoveryTokens, accessEventRecorder, deferredCommands, clock);
    }

    @Bean
    SendRecoveryLinkCommandHandler sendRecoveryLinkCommandHandler(
            CaretakerRepository caretakerRepository,
            IdentityMailer identityMailer,
            AccessEventRecorder accessEventRecorder,
            Clock clock) {
        return new SendRecoveryLinkCommandHandler(caretakerRepository, identityMailer, accessEventRecorder, clock);
    }

    @Bean
    VerifyRecoveryLinkCommandHandler verifyRecoveryLinkCommandHandler(
            CaretakerRepository caretakerRepository,
            RecoveryThrottle recoveryThrottle,
            AccessEventRecorder accessEventRecorder,
            Clock clock) {
        return new VerifyRecoveryLinkCommandHandler(
                caretakerRepository, recoveryThrottle, accessEventRecorder, clock);
    }

    @Bean
    RecoverPasswordCommandHandler recoverPasswordCommandHandler(
            CaretakerRepository caretakerRepository,
            PasswordHasher passwordHasher,
            RecoveryThrottle recoveryThrottle,
            AccessEventRecorder accessEventRecorder,
            DeferredCommands deferredCommands,
            Clock clock) {
        return new RecoverPasswordCommandHandler(
                caretakerRepository, passwordHasher, recoveryThrottle, accessEventRecorder, deferredCommands, clock);
    }

    @Bean
    NotifyPasswordRecoveredCommandHandler notifyPasswordRecoveredCommandHandler(
            CaretakerRepository caretakerRepository, IdentityMailer identityMailer) {
        return new NotifyPasswordRecoveredCommandHandler(caretakerRepository, identityMailer);
    }

    @Bean
    SeedInitialAdministratorCommandHandler seedInitialAdministratorCommandHandler(
            CaretakerRepository caretakerRepository, PasswordHasher passwordHasher, Clock clock) {
        return new SeedInitialAdministratorCommandHandler(caretakerRepository, passwordHasher, clock);
    }

    @Bean
    RegisterCaretakerCommandHandler registerCaretakerCommandHandler(
            CaretakerRepository caretakerRepository, PasswordHasher passwordHasher, Clock clock) {
        return new RegisterCaretakerCommandHandler(caretakerRepository, passwordHasher, clock);
    }

    @Bean
    UpdateCaretakerCommandHandler updateCaretakerCommandHandler(CaretakerRepository caretakerRepository, Clock clock) {
        return new UpdateCaretakerCommandHandler(caretakerRepository, clock);
    }

    @Bean
    DeactivateCaretakerCommandHandler deactivateCaretakerCommandHandler(
            CaretakerRepository caretakerRepository, Clock clock) {
        return new DeactivateCaretakerCommandHandler(caretakerRepository, clock);
    }

    @Bean
    SearchCaretakersQueryHandler searchCaretakersQueryHandler(CaretakerDirectory caretakerDirectory) {
        return new SearchCaretakersQueryHandler(caretakerDirectory);
    }

    @Bean
    FindCaretakerByIdQueryHandler findCaretakerByIdQueryHandler(CaretakerDirectory caretakerDirectory) {
        return new FindCaretakerByIdQueryHandler(caretakerDirectory);
    }

    @Bean
    GetAuthenticatedCaretakerQueryHandler getAuthenticatedCaretakerQueryHandler(
            CaretakerReadModels caretakerReadModels) {
        return new GetAuthenticatedCaretakerQueryHandler(caretakerReadModels);
    }
}
