package io.github.ovyx.identity.infrastructure;

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
import io.github.ovyx.identity.domain.port.AccessEventRecorder;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.identity.domain.port.SignInThrottle;
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
