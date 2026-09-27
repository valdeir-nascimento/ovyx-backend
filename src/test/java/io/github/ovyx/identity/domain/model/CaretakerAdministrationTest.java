package io.github.ovyx.identity.domain.model;

import static io.github.ovyx.shared.domain.DomainViolations.refusalCodeOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalMessageOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.identity.fixtures.InMemoryCaretakerRepository;
import io.github.ovyx.identity.domain.CountingPasswordHasher;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.domain.FixedClock;
import io.github.ovyx.shared.domain.Violation;
import java.time.Duration;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Testes das regras do agregado que dependem dos demais responsaveis: unicidade do CPF, do e-mail e
 * do celular (FR-016) e o ultimo administrador ativo (FR-019).
 *
 * <p>Os demais responsaveis vem de um repositorio em memoria fiel ao adaptador, e nao de um mock: a
 * regra consulta de verdade quem esta cadastrado. Maria e os dados padrao do
 * {@link CaretakerTestDataBuilder}; Joao e quem chega depois.
 */
@DisplayName("Caretaker administration rules")
class CaretakerAdministrationTest {

    private static final String MARIA_CPF = "52998224725";
    private static final String MARIA_EMAIL = "maria.silva@ovyx.com.br";
    private static final String MARIA_MOBILE_PHONE = "91988887777";

    private final CountingPasswordHasher hasher = new CountingPasswordHasher();
    private final FixedClock clock = FixedClock.at("2026-09-19T12:00:00Z");
    private final InMemoryCaretakerRepository roster = new InMemoryCaretakerRepository();

    private CaretakerTestDataBuilder maria() {
        return aCaretaker().withHasher(hasher).withRoster(roster).withClock(clock);
    }

    private CaretakerTestDataBuilder joao() {
        return aCaretaker()
                .withFullName("João Pereira de Souza")
                .withCpf("11144477735")
                .withEmail("joao.pereira@ovyx.com.br")
                .withMobilePhone("91991234567")
                .withHasher(hasher)
                .withRoster(roster)
                .withClock(clock);
    }

    private Caretaker registered(CaretakerTestDataBuilder builder) {
        Caretaker caretaker = builder.build();
        roster.save(caretaker);
        return caretaker;
    }

    /** Gravado ja inativo, sem passar pela inativacao, que o ultimo administrador nao aceita. */
    private Caretaker registeredInactive(CaretakerTestDataBuilder builder) {
        Caretaker caretaker = builder.buildInactive();
        roster.save(caretaker);
        return caretaker;
    }

    /** Uma copia do responsavel como estava agora, para quando outra operacao o mudar depois. */
    private static Caretaker copyOf(Caretaker caretaker) {
        return Caretaker.restore(
                caretaker.id(),
                caretaker.fullName(),
                caretaker.cpf(),
                caretaker.email(),
                caretaker.mobilePhone(),
                caretaker.passwordHash(),
                caretaker.role(),
                caretaker.status(),
                caretaker.mustChangePassword(),
                caretaker.createdAt(),
                caretaker.updatedAt());
    }

    private Caretaker deactivated(Caretaker caretaker) {
        caretaker.deactivate(roster, clock);
        roster.save(caretaker);
        return caretaker;
    }

    // ------------------------------------------------------------------------------ cadastro

    private static Stream<Arguments> identifiersOfAnActiveCaretaker() {
        return Stream.of(
                Arguments.of(
                        "the same CPF",
                        (UnaryOperator<CaretakerTestDataBuilder>) builder -> builder.withCpf(MARIA_CPF),
                        "cpf",
                        IdentityErrorCode.CPF_ALREADY_IN_USE),
                Arguments.of(
                        "the same email",
                        (UnaryOperator<CaretakerTestDataBuilder>) builder -> builder.withEmail(MARIA_EMAIL),
                        "email",
                        IdentityErrorCode.EMAIL_ALREADY_IN_USE),
                Arguments.of(
                        "the same email in another spelling",
                        (UnaryOperator<CaretakerTestDataBuilder>) builder -> builder.withEmail(" Maria.Silva@OVYX.com.br "),
                        "email",
                        IdentityErrorCode.EMAIL_ALREADY_IN_USE),
                Arguments.of(
                        "the same mobile phone",
                        (UnaryOperator<CaretakerTestDataBuilder>) builder -> builder.withMobilePhone(MARIA_MOBILE_PHONE),
                        "mobilePhone",
                        IdentityErrorCode.MOBILE_PHONE_ALREADY_IN_USE));
    }

    @ParameterizedTest(name = "refuses {0}")
    @MethodSource("identifiersOfAnActiveCaretaker")
    @DisplayName("refuses an identifier that an active caretaker already holds")
    void givenIdentifierHeldByAnActiveCaretaker_whenRegistering_thenRefuseWithThatConflict(
            String situation,
            UnaryOperator<CaretakerTestDataBuilder> sameAsMaria,
            String field,
            IdentityErrorCode conflict) {
        // given
        registered(maria());
        CaretakerTestDataBuilder joao = sameAsMaria.apply(joao());

        // when
        List<Violation> violations = violationsOf(joao::build);

        // then
        assertThat(violations).extracting(Violation::field, Violation::code).containsExactly(tuple(field, conflict));
        assertThat(refusalCodeOf(joao::build)).isEqualTo(conflict);
    }

    @Test
    @DisplayName("accepts the email and the mobile phone of an inactive caretaker")
    void givenIdentifiersOfAnInactiveCaretaker_whenRegistering_thenAccept() {
        // given
        // FR-016 vale so entre ativos: quem foi inativado preserva o historico, mas nao prende o
        // identificador de acesso.
        deactivated(registered(maria()));

        // when
        Caretaker joao = joao().withEmail(MARIA_EMAIL).withMobilePhone(MARIA_MOBILE_PHONE).build();

        // then
        assertThat(joao.email().value()).isEqualTo(MARIA_EMAIL);
        assertThat(joao.mobilePhone().value()).isEqualTo(MARIA_MOBILE_PHONE);
    }

    @Test
    @DisplayName("refuses the CPF of an inactive caretaker: the same person never exists twice")
    void givenCpfOfAnInactiveCaretaker_whenRegistering_thenRefuseAsCpfAlreadyInUse() {
        // given
        deactivated(registered(maria()));
        CaretakerTestDataBuilder joao = joao().withCpf(MARIA_CPF);

        // when
        List<Violation> violations = violationsOf(joao::build);

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(IdentityErrorCode.CPF_ALREADY_IN_USE);
    }

    @Test
    @DisplayName("reports every conflict at once, and the refusal carries the first in form order")
    void givenCpfEmailAndMobilePhoneAllTaken_whenRegistering_thenReportTheThreeConflictsTogether() {
        // given
        registered(maria());
        CaretakerTestDataBuilder joao =
                joao().withCpf(MARIA_CPF).withEmail(MARIA_EMAIL).withMobilePhone(MARIA_MOBILE_PHONE);

        // when
        List<Violation> violations = violationsOf(joao::build);

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(
                        tuple("cpf", IdentityErrorCode.CPF_ALREADY_IN_USE),
                        tuple("email", IdentityErrorCode.EMAIL_ALREADY_IN_USE),
                        tuple("mobilePhone", IdentityErrorCode.MOBILE_PHONE_ALREADY_IN_USE));
        assertThat(refusalCodeOf(joao::build)).isEqualTo(IdentityErrorCode.CPF_ALREADY_IN_USE);
    }

    @Test
    @DisplayName("validates the fields before looking for conflicts")
    void givenMalformedCpfAndTakenEmail_whenRegistering_thenRefuseOnlyAsInvalid() {
        // given
        // Um CPF invalido nao tem dono para conflitar; primeiro a pessoa corrige o formulario.
        registered(maria());
        CaretakerTestDataBuilder joao = joao().withCpf("12345678900").withEmail(MARIA_EMAIL);

        // when
        List<Violation> violations = violationsOf(joao::build);

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(IdentityErrorCode.CPF_INVALID);
        assertThat(refusalCodeOf(joao::build)).isEqualTo(IdentityErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("does not pay for the password hash when the registration conflicts")
    void givenTakenEmail_whenRegistering_thenNeverHashThePassword() {
        // given
        // O Argon2 custa centenas de milissegundos; so faz sentido pagar quando o cadastro vai valer.
        registered(maria());
        hasher.reset();
        CaretakerTestDataBuilder joao = joao().withEmail(MARIA_EMAIL);

        // when
        violationsOf(joao::build);

        // then
        assertThat(hasher.hashCalls()).isZero();
    }

    // ------------------------------------------------------------------------------- edicao

    @Test
    @DisplayName("updates every field and records when")
    void givenValidData_whenUpdating_thenApplyEveryFieldAndTouchTheUpdateTime() {
        // given
        Caretaker maria = registered(maria());
        clock.advance(Duration.ofHours(2));

        // when
        maria.update("Maria Silva Souza", "11144477735", "maria.souza@ovyx.com.br", "91977776666", Role.ADMINISTRATOR.name(), roster, clock);

        // then
        assertThat(maria.fullName().value()).isEqualTo("Maria Silva Souza");
        assertThat(maria.cpf().value()).isEqualTo("11144477735");
        assertThat(maria.email().value()).isEqualTo("maria.souza@ovyx.com.br");
        assertThat(maria.mobilePhone().value()).isEqualTo("91977776666");
        assertThat(maria.role()).isEqualTo(Role.ADMINISTRATOR);
        assertThat(maria.updatedAt()).isEqualTo(clock.instant()).isAfter(maria.createdAt());
    }

    @Test
    @DisplayName("reports ALL invalid fields of an update at once and changes nothing")
    void givenEveryFieldInvalid_whenUpdating_thenReportEveryFieldTogetherAndKeepTheData() {
        // given
        Caretaker maria = registered(maria());

        // when
        List<Violation> violations = violationsOf(() -> maria.update("", "123", "sem-arroba", "12", null, roster, clock));

        // then
        assertThat(violations)
                .extracting(Violation::field)
                .containsExactly("fullName", "cpf", "email", "mobilePhone", "role");
        assertThat(maria.email().value()).isEqualTo(MARIA_EMAIL);
    }

    @Test
    @DisplayName("refuses a role outside the list on update, together with the other violations")
    void givenRoleOutsideTheListAndEmptyName_whenUpdating_thenReportBothTogetherAndKeepTheRole() {
        // given
        Caretaker maria = registered(maria());

        // when
        List<Violation> violations = violationsOf(
                () -> maria.update("", MARIA_CPF, MARIA_EMAIL, MARIA_MOBILE_PHONE, "ROOT", roster, clock));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(
                        tuple("fullName", IdentityErrorCode.FULL_NAME_REQUIRED),
                        tuple("role", IdentityErrorCode.ROLE_INVALID));
        assertThat(maria.role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("keeping its own identifiers is not a conflict")
    void givenTheCaretakerOwnIdentifiers_whenUpdating_thenAccept() {
        // given
        Caretaker maria = registered(maria());

        // when
        maria.update("Maria Silva Souza", MARIA_CPF, MARIA_EMAIL, MARIA_MOBILE_PHONE, Role.USER.name(), roster, clock);

        // then
        assertThat(maria.fullName().value()).isEqualTo("Maria Silva Souza");
    }

    @Test
    @DisplayName("refuses an email that another active caretaker holds, and changes nothing")
    void givenEmailOfAnotherActiveCaretaker_whenUpdating_thenRefuseAndKeepTheData() {
        // given
        Caretaker maria = registered(maria());
        Caretaker joao = registered(joao());

        // when
        List<Violation> violations = violationsOf(() -> maria.update(
                "Maria Silva", MARIA_CPF, joao.email().value(), MARIA_MOBILE_PHONE, Role.USER.name(), roster, clock));

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(IdentityErrorCode.EMAIL_ALREADY_IN_USE);
        assertThat(maria.email().value()).isEqualTo(MARIA_EMAIL);
    }

    @Test
    @DisplayName("an inactive caretaker does not hold identifiers against active ones")
    void givenInactiveCaretakerTakingTheEmailOfAnActiveOne_whenUpdating_thenAccept() {
        // given
        // A unicidade vale entre ativos: o inativo nao entra no sistema com o e-mail repetido.
        Caretaker maria = deactivated(registered(maria()));
        Caretaker joao = registered(joao());

        // when
        maria.update("Maria Silva", MARIA_CPF, joao.email().value(), MARIA_MOBILE_PHONE, Role.USER.name(), roster, clock);

        // then
        assertThat(maria.email()).isEqualTo(joao.email());
    }

    @Test
    @DisplayName("refuses demoting the last active administrator")
    void givenTheLastActiveAdministrator_whenDemotingIt_thenRefuseAsLastAdministratorAndKeepTheRole() {
        // given
        Caretaker administrator = registered(maria().withRole(Role.ADMINISTRATOR));

        // when
        List<Violation> violations = violationsOf(() ->
                administrator.update("Maria Silva", MARIA_CPF, MARIA_EMAIL, MARIA_MOBILE_PHONE, Role.USER.name(), roster, clock));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(tuple("role", IdentityErrorCode.LAST_ADMINISTRATOR));
        assertThat(administrator.role()).isEqualTo(Role.ADMINISTRATOR);
    }

    @Test
    @DisplayName("demotes an administrator while another one stays active")
    void givenAnotherActiveAdministrator_whenDemotingOne_thenAccept() {
        // given
        Caretaker maria = registered(maria().withRole(Role.ADMINISTRATOR));
        registered(joao().withRole(Role.ADMINISTRATOR));

        // when
        maria.update("Maria Silva", MARIA_CPF, MARIA_EMAIL, MARIA_MOBILE_PHONE, Role.USER.name(), roster, clock);

        // then
        assertThat(maria.role()).isEqualTo(Role.USER);
    }

    // ---------------------------------------------------------------------------- inativacao

    @Test
    @DisplayName("deactivates a common user and records when")
    void givenActiveUser_whenDeactivating_thenBecomeInactiveAndTouchTheUpdateTime() {
        // given
        Caretaker maria = registered(maria());
        clock.advance(Duration.ofHours(1));

        // when
        maria.deactivate(roster, clock);

        // then
        assertThat(maria.status()).isEqualTo(CaretakerStatus.INACTIVE);
        assertThat(maria.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("refuses deactivating the last active administrator")
    void givenTheLastActiveAdministrator_whenDeactivating_thenRefuseAsLastAdministratorAndStayActive() {
        // given
        Caretaker administrator = registered(maria().withRole(Role.ADMINISTRATOR));

        // when
        List<Violation> violations = violationsOf(() -> administrator.deactivate(roster, clock));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(tuple("status", IdentityErrorCode.LAST_ADMINISTRATOR));
        assertThat(administrator.isActive()).isTrue();
    }

    @Test
    @DisplayName("an inactive administrator does not count as the other administrator")
    void givenOnlyAnInactiveOtherAdministrator_whenDeactivating_thenRefuseAsLastAdministrator() {
        // given
        Caretaker administrator = registered(maria().withRole(Role.ADMINISTRATOR));
        registered(joao().withRole(Role.ADMINISTRATOR)).deactivate(roster, clock);

        // when
        List<Violation> violations = violationsOf(() -> administrator.deactivate(roster, clock));

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(IdentityErrorCode.LAST_ADMINISTRATOR);
    }

    @Test
    @DisplayName("deactivates an administrator while another one stays active")
    void givenAnotherActiveAdministrator_whenDeactivatingOne_thenBecomeInactive() {
        // given
        Caretaker maria = registered(maria().withRole(Role.ADMINISTRATOR));
        registered(joao().withRole(Role.ADMINISTRATOR));

        // when
        maria.deactivate(roster, clock);

        // then
        assertThat(maria.isActive()).isFalse();
    }

    @Test
    @DisplayName("refuses reactivating while another active caretaker holds the email")
    void givenEmailTakenMeanwhileByAnotherActiveCaretaker_whenReactivating_thenRefuseAndStayInactive() {
        // given
        // Enquanto Maria estava inativa, o e-mail dela foi liberado e Joao o recebeu (FR-016).
        Caretaker maria = deactivated(registered(maria()));
        registered(joao().withEmail(MARIA_EMAIL));

        // when
        List<Violation> violations = violationsOf(() -> maria.reactivate(roster, clock));

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(IdentityErrorCode.EMAIL_ALREADY_IN_USE);
        assertThat(maria.isActive()).isFalse();
    }

    // ----------------------------------------------------- restauracao do administrador inicial

    @Test
    @DisplayName("restores an inactive former administrator as the initial administrator (FR-025)")
    void givenInactiveFormerAdministrator_whenRestoringAsInitialAdministrator_thenActiveAdministratorOwingTheChange() {
        // given
        // Sem administrador ativo, a instalacao precisa de uma saida: a mesma pessoa, pelo CPF
        // configurado, volta como administradora com a senha provisoria e a troca obrigatoria.
        Caretaker maria = registeredInactive(maria().withRole(Role.ADMINISTRATOR));

        // when
        maria.restoreAsInitialAdministrator("RecuperarAcesso2026", hasher, roster, clock);

        // then
        assertThat(maria.isActive()).isTrue();
        assertThat(maria.role()).isEqualTo(Role.ADMINISTRATOR);
        assertThat(maria.mustChangePassword()).isTrue();
        assertThat(maria.authenticate("RecuperarAcesso2026", hasher)).isTrue();
    }

    @Test
    @DisplayName("refuses the restoration when another active caretaker took the email meanwhile")
    void givenEmailTakenByAnotherActiveCaretaker_whenRestoringAsInitialAdministrator_thenRefuseAndStayInactive() {
        // given
        Caretaker maria = registeredInactive(maria().withRole(Role.ADMINISTRATOR));
        registered(joao().withEmail(MARIA_EMAIL));

        // when
        List<Violation> violations =
                violationsOf(() -> maria.restoreAsInitialAdministrator("RecuperarAcesso2026", hasher, roster, clock));

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(IdentityErrorCode.EMAIL_ALREADY_IN_USE);
        assertThat(maria.isActive()).isFalse();
    }

    @Test
    @DisplayName("refuses a provisional password outside the policy, and changes nothing")
    void givenPasswordOutsidePolicy_whenRestoringAsInitialAdministrator_thenRefuseAsInvalid() {
        // given
        Caretaker maria = registeredInactive(maria().withRole(Role.ADMINISTRATOR));

        // when
        List<Violation> violations =
                violationsOf(() -> maria.restoreAsInitialAdministrator("curta1", hasher, roster, clock));

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(IdentityErrorCode.PASSWORD_TOO_SHORT);
        assertThat(maria.isActive()).isFalse();
    }

    @Test
    @DisplayName("demotes an inactive administrator even when a single active one remains")
    void givenInactiveAdministratorAndOneActive_whenDemotingTheInactiveOne_thenAccept() {
        // given
        // So o administrador ativo conta para o FR-019: rebaixar quem ja esta inativo nao tira
        // ninguem da administracao.
        Caretaker inactive = registeredInactive(maria().withRole(Role.ADMINISTRATOR));
        registered(joao().withRole(Role.ADMINISTRATOR));

        // when
        inactive.update("Maria Silva", MARIA_CPF, MARIA_EMAIL, MARIA_MOBILE_PHONE, Role.USER.name(), roster, clock);

        // then
        assertThat(inactive.role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("does not refuse, as the last administrator, a stale copy of an administrator already deactivated")
    void givenStaleCopyOfAnAdministratorAlreadyDeactivated_whenDemotingIt_thenDoNotRefuseAsTheLastAdministrator() {
        // given
        // A edicao carregou Maria como administradora ativa, e uma inativacao simultanea confirmou antes
        // de ela decidir. Contando so os ativos, Joao era "o ultimo", e a edicao era recusada com 409
        // sobre um estado que ja nao existia (revisao da T290). Quem esta fora do conjunto nao e o
        // ultimo: a gravacao segue, e a versao da linha manda decidir de novo sobre o estado atual.
        Caretaker maria = registered(maria().withRole(Role.ADMINISTRATOR));
        registered(joao().withRole(Role.ADMINISTRATOR));
        Caretaker staleMaria = copyOf(maria);
        deactivated(maria);

        // when
        staleMaria.update("Maria Silva", MARIA_CPF, MARIA_EMAIL, MARIA_MOBILE_PHONE, "USER", roster, clock);

        // then
        assertThat(staleMaria.role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("says, in the refusal itself, that this is the last active administrator")
    void givenTheLastActiveAdministrator_whenDeactivating_thenRefuseWithTheMessageThePublishedExampleShows() {
        // given
        Caretaker administrator = registered(maria().withRole(Role.ADMINISTRATOR));

        // when
        String message = refusalMessageOf(() -> administrator.deactivate(roster, clock));

        // then
        assertThat(message).isEqualTo("Este é o último administrador ativo do sistema.");
    }

    @Test
    @DisplayName("deactivating an inactive caretaker changes nothing")
    void givenInactiveCaretaker_whenDeactivatingAgain_thenKeepTheUpdateTime() {
        // given
        Caretaker maria = deactivated(registered(maria()));
        clock.advance(Duration.ofHours(1));

        // when
        maria.deactivate(roster, clock);

        // then
        assertThat(maria.status()).isEqualTo(CaretakerStatus.INACTIVE);
        assertThat(maria.updatedAt()).isBefore(clock.instant());
    }
}
