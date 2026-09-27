package io.github.ovyx.identity.integration;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.randomValidCpf;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.application.caretaker.DeactivateCaretakerCommand;
import io.github.ovyx.identity.application.caretaker.RegisterCaretakerCommand;
import io.github.ovyx.identity.application.caretaker.UpdateCaretakerCommand;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Unicidade de CPF, e-mail e celular contra PostgreSQL real (FR-016, cenario V-08).
 *
 * <p>O dublê em memoria reimplementa as perguntas do quadro de responsaveis; so o banco prova que
 * as consultas do adaptador respondem o mesmo, e que o indice unico parcial libera o e-mail e o
 * celular de quem foi inativado.
 */
@DisplayName("Caretaker uniqueness")
class CaretakerUniquenessIT extends IntegrationTestSupport {

    private static final String PASSWORD = "AviarioSul2026";

    @Autowired
    private Dispatcher dispatcher;

    @Autowired
    private CaretakerRepository repository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private Clock clock;

    private Caretaker holder() {
        Caretaker caretaker =
                aUniqueCaretaker().withHasher(passwordHasher).withRoster(repository).withClock(clock).build();
        repository.save(caretaker);
        return caretaker;
    }

    private static String uniqueEmail() {
        return "unicidade." + UUID.randomUUID().toString().substring(0, 8) + "@ovyx.com.br";
    }

    private static String uniqueMobilePhone() {
        return "919" + String.format("%08d", Math.floorMod(UUID.randomUUID().hashCode(), 100_000_000));
    }

    private Result<CaretakerId> register(String cpf, String email, String mobilePhone) {
        return dispatcher.dispatch(
                new RegisterCaretakerCommand("Responsável Novo", cpf, email, mobilePhone, PASSWORD, Role.USER.name()));
    }

    @Test
    @DisplayName("refuses the email of an active caretaker")
    void givenActiveHolderOfTheEmail_whenRegisteringAnotherWithIt_thenFailAsEmailConflict() {
        // given
        Caretaker holder = holder();

        // when
        Result<CaretakerId> result = register(randomValidCpf(), holder.email().value(), uniqueMobilePhone());

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.EMAIL_ALREADY_IN_USE.code());
    }

    @Test
    @DisplayName("refuses the mobile phone of an active caretaker")
    void givenActiveHolderOfTheMobilePhone_whenRegisteringAnotherWithIt_thenFailAsMobilePhoneConflict() {
        // given
        Caretaker holder = holder();

        // when
        Result<CaretakerId> result = register(randomValidCpf(), uniqueEmail(), holder.mobilePhone().value());

        // then
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.MOBILE_PHONE_ALREADY_IN_USE.code());
    }

    @Test
    @DisplayName("accepts the email and the mobile phone once their holder is deactivated")
    void givenHolderDeactivated_whenRegisteringAnotherWithItsEmailAndMobilePhone_thenAccept() {
        // given
        // O indice unico e parcial (status = 'ACTIVE'): a linha do inativo fica, e nao prende nada.
        Caretaker holder = holder();
        dispatcher.dispatch(new DeactivateCaretakerCommand(holder.id()));

        // when
        Result<CaretakerId> result = register(randomValidCpf(), holder.email().value(), holder.mobilePhone().value());

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(repository.findById(result.value())).isPresent();
    }

    @Test
    @DisplayName("refuses the CPF of an inactive caretaker: the same person never exists twice")
    void givenHolderDeactivated_whenRegisteringAnotherWithItsCpf_thenFailAsCpfConflict() {
        // given
        Caretaker holder = holder();
        dispatcher.dispatch(new DeactivateCaretakerCommand(holder.id()));

        // when
        Result<CaretakerId> result = register(holder.cpf().value(), uniqueEmail(), uniqueMobilePhone());

        // then
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.CPF_ALREADY_IN_USE.code());
    }

    @Test
    @DisplayName("keeping its own identifiers in an update is not a conflict")
    void givenCaretakerKeepingItsIdentifiers_whenUpdating_thenAccept() {
        // given
        Caretaker caretaker = holder();
        UpdateCaretakerCommand rename = new UpdateCaretakerCommand(
                caretaker.id(),
                "Nome Corrigido",
                caretaker.cpf().value(),
                caretaker.email().value(),
                caretaker.mobilePhone().value(),
                Role.USER.name());

        // when
        Result<CaretakerId> result = dispatcher.dispatch(rename);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(repository.findById(caretaker.id()).orElseThrow().fullName().value()).isEqualTo("Nome Corrigido");
    }
}
