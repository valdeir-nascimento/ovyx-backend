package io.github.ovyx.identity.domain.model;

import io.github.ovyx.identity.domain.PasswordPolicy;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.port.CaretakerRoster;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.identity.domain.valueobject.Cpf;
import io.github.ovyx.identity.domain.valueobject.Email;
import io.github.ovyx.identity.domain.valueobject.FullName;
import io.github.ovyx.identity.domain.valueobject.MobilePhone;
import io.github.ovyx.identity.domain.valueobject.PasswordHash;
import io.github.ovyx.identity.domain.valueobject.PasswordRecovery;
import io.github.ovyx.identity.domain.valueobject.RecoveryAllowance;
import io.github.ovyx.identity.domain.valueobject.RecoveryToken;
import io.github.ovyx.shared.domain.AggregateRoot;
import io.github.ovyx.shared.domain.DomainException;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Violation;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

/**
 * Raiz de agregado do contexto Identity: a pessoa autorizada a usar o sistema.
 *
 * <p>"Responsável" no legado nao e apenas uma conta: e quem responde por setores e gaiolas, e e
 * referenciado por relatorios. Dai a traducao para {@code Caretaker} e nao para {@code User} — o
 * registro em docs/glossary.md.
 *
 * <p>Invariantes garantidas aqui (data-model.md):
 *
 * <ol>
 *   <li>Nome, CPF, e-mail, celular, senha e perfil sao obrigatorios, e <strong>todas</strong> as
 *       violacoes voltam de uma vez, em {@code details} (FR-017)
 *   <li>CPF invalido recusa a operacao (FR-015)
 *   <li>A senha nunca entra em texto claro: so o valor produzido pelo {@link PasswordHasher}
 *   <li>Responsavel inativo nao autentica (FR-005)
 *   <li>Trocar a propria senha exige a senha atual e a politica minima atendida (FR-020, FR-022)
 *   <li>O CPF nao se repete entre responsaveis, e e-mail e celular nao se repetem entre ativos (FR-016)
 *   <li>O sistema nunca fica sem administrador ativo (FR-019)
 * </ol>
 *
 * <p>As duas ultimas dependem dos demais responsaveis. Mesmo assim sao decididas aqui, e nao no
 * tratador (principio II): o agregado pergunta ao {@link CaretakerRoster} e recusa. O tratador so
 * entrega a porta, como entrega o {@link PasswordHasher}.
 *
 * <p>A pergunta e a gravacao so valem juntas dentro de uma transacao, e duas requisicoes simultaneas
 * podem fazer a mesma pergunta antes de qualquer uma gravar. Por isso o despachante abre uma
 * transacao por comando; a contagem de administradores ativos trava as linhas contadas ate a
 * confirmacao; e, quando um indice unico recusa a segunda gravacao, o comando roda de novo e recebe
 * daqui o conflito com o codigo da regra.
 */
public final class Caretaker extends AggregateRoot<CaretakerId> {

    private static final String KEEP_AN_ADMINISTRATOR = "O sistema precisa de ao menos um administrador ativo.";
    private static final String LAST_ADMINISTRATOR_REFUSAL = "Este é o último administrador ativo do sistema.";

    private final Instant createdAt;

    private FullName fullName;
    private Cpf cpf;
    private Email email;
    private MobilePhone mobilePhone;
    private PasswordHash passwordHash;
    private Role role;
    private CaretakerStatus status;
    private boolean mustChangePassword;
    private ThemePreference themePreference;
    private PasswordRecovery passwordRecovery;
    private RecoveryAllowance recoveryAllowance;
    private int sessionGeneration;
    private Instant updatedAt;

    private Caretaker(
        CaretakerId id,
        FullName fullName,
        Cpf cpf,
        Email email,
        MobilePhone mobilePhone,
        PasswordHash passwordHash,
        Role role,
        CaretakerStatus status,
        boolean mustChangePassword,
        ThemePreference themePreference,
        PasswordRecovery passwordRecovery,
        RecoveryAllowance recoveryAllowance,
        int sessionGeneration,
        Instant createdAt,
        Instant updatedAt) {
        super(id);
        this.fullName = fullName;
        this.cpf = cpf;
        this.email = email;
        this.mobilePhone = mobilePhone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.mustChangePassword = mustChangePassword;
        this.themePreference = themePreference;
        this.passwordRecovery = passwordRecovery;
        this.recoveryAllowance = recoveryAllowance;
        this.sessionGeneration = sessionGeneration;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Cadastra um responsavel.
     *
     * <p>Cada campo e validado de forma independente, de proposito: se a validacao parasse na
     * primeira falha, quem preenche o formulario veria um erro por vez. As violacoes de cada campo
     * sao reunidas no {@link Notification} e saem juntas, numa recusa so (FR-017).
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando algum campo viola uma regra
     */
    public static Caretaker register(
        String rawFullName,
        String rawCpf,
        String rawEmail,
        String rawMobilePhone,
        String rawPassword,
        String rawRole,
        boolean mustChangePassword,
        PasswordHasher hasher,
        CaretakerRoster roster,
        Clock clock
    ) {

        Notification notification = new Notification();

        FullName.validate(rawFullName, notification);
        Cpf.validate(rawCpf, notification);
        Email.validate(rawEmail, notification);
        MobilePhone.validate(rawMobilePhone, notification);
        PasswordPolicy.validate(rawPassword, "password", rawEmail, rawCpf, notification);
        Role.validate(rawRole, notification);
        notification.throwIfAny(IdentityErrorCode.VALIDATION_FAILED);

        // Daqui em diante cada campo ja passou pelas proprias regras: construir os objetos de valor
        // nao recusa mais, e nenhum deles precisa existir pela metade.
        CaretakerId id = CaretakerId.generate();
        Cpf cpf = Cpf.of(rawCpf);
        Email email = Email.of(rawEmail);
        MobilePhone mobilePhone = MobilePhone.of(rawMobilePhone);

        // So com os identificadores na forma canonica da para perguntar quem mais os usa. E so depois
        // deles o hash: o Argon2 custa caro demais para um cadastro que vai ser recusado.
        refuse(identifierConflicts(id, cpf, email, mobilePhone, true, roster));

        Instant now = clock.instant();
        return new Caretaker(
            id,
            FullName.of(rawFullName),
            cpf,
            email,
            mobilePhone,
            hasher.hash(rawPassword),
            Role.of(rawRole),
            CaretakerStatus.ACTIVE,
            mustChangePassword, ThemePreference.SYSTEM,
            null,
            RecoveryAllowance.NONE,
            0,
            now,
            now
        );
    }

    /**
     * Cadastra com o perfil ja escolhido, para quem nao recebe texto digitado, como a semeadura.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando algum campo viola uma regra
     */
    public static Caretaker register(
        String rawFullName,
        String rawCpf,
        String rawEmail,
        String rawMobilePhone,
        String rawPassword,
        Role role,
        boolean mustChangePassword,
        PasswordHasher hasher,
        CaretakerRoster roster,
        Clock clock
    ) {
        return register(
            rawFullName,
            rawCpf,
            rawEmail,
            rawMobilePhone,
            rawPassword,
            role == null ? null : role.name(),
            mustChangePassword,
            hasher,
            roster,
            clock);
    }

    /**
     * Reconstroi o agregado a partir do que foi gravado.
     *
     * <p>Usada apenas pelo adaptador de persistencia: os dados ja foram validados quando entraram,
     * e revalidar na leitura faria o sistema recusar registros legitimos apos uma mudanca de regra.
     */
    public static Caretaker restore(
        CaretakerId id,
        FullName fullName,
        Cpf cpf,
        Email email,
        MobilePhone mobilePhone,
        PasswordHash passwordHash,
        Role role,
        CaretakerStatus status,
        boolean mustChangePassword,
        ThemePreference themePreference,
        PasswordRecovery passwordRecovery,
        RecoveryAllowance recoveryAllowance,
        int sessionGeneration,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new Caretaker(
            id,
            fullName,
            cpf,
            email,
            mobilePhone,
            passwordHash,
            role,
            status,
            mustChangePassword,
            themePreference,
            passwordRecovery,
            recoveryAllowance,
            sessionGeneration,
            createdAt,
            updatedAt
        );
    }

    /**
     * Confere a credencial.
     *
     * <p>Responsavel inativo nunca autentica, mesmo com a senha correta (invariante 4). A
     * verificacao da senha acontece de qualquer forma, para que o custo da operacao nao denuncie a
     * situacao do responsavel a quem observa o tempo de resposta.
     */
    public boolean authenticate(String rawPassword, PasswordHasher hasher) {
        boolean passwordMatches = hasher.matches(rawPassword, passwordHash);
        return isActive() && passwordMatches;
    }

    /**
     * Troca a propria senha (FR-020).
     *
     * <p>A senha atual ausente e verificada aqui, junto com a politica da nova, e nao na borda HTTP:
     * assim as violacoes dos dois campos voltam de uma vez so.
     *
     * <p>O inativo e recusado antes de tudo, com {@code CARETAKER_UNAVAILABLE}: a sessao pode
     * sobreviver a inativacao, e trocar a senha por ela seria continuar agindo sobre a conta
     * (invariante 4). A recusa mora aqui, e nao em quem chama, para nenhum chamador precisar lembrar
     * dela (T179).
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando o responsavel esta inativo, quando a
     *                                                      senha atual nao confere ou quando a nova
     *                                                      viola a politica
     */
    public void changeOwnPassword(String currentPassword, String newPassword, PasswordHasher hasher, Clock clock) {
        if (!isActive()) {
            throw new DomainException(
                    IdentityErrorCode.CARETAKER_UNAVAILABLE, IdentityErrorCode.CARETAKER_UNAVAILABLE_MESSAGE);
        }

        Notification notification = new Notification();

        boolean currentInformed = notification.requirePresent(
                "currentPassword", currentPassword, IdentityErrorCode.CURRENT_PASSWORD_REQUIRED, "Informe a senha atual.");
        if (currentInformed && !hasher.matches(currentPassword, passwordHash)) {
            notification.add(
                    "currentPassword", IdentityErrorCode.CURRENT_PASSWORD_INCORRECT, "A senha atual está incorreta.");
        }
        PasswordPolicy.validate(newPassword, "newPassword", email.value(), cpf.value(), notification);
        notification.throwIfAny(IdentityErrorCode.VALIDATION_FAILED);

        this.passwordHash = hasher.hash(newPassword);
        this.mustChangePassword = false;
        // Toda troca de senha anula o link de recuperacao pendente (FR-007 da 012).
        this.passwordRecovery = null;
        touch(clock);
    }

    /**
     * Edita os dados cadastrais e o perfil (FR-014). A senha nao muda por aqui.
     *
     * <p>Mesma ordem do cadastro: primeiro todas as violacoes de campo, de uma vez (FR-017); depois
     * todos os conflitos com os demais responsaveis, tambem de uma vez. Recusada, a edicao nao altera
     * nada.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando algum campo viola uma regra, quando
     *                                                      um identificador ja tem dono ou quando o
     *                                                      rebaixamento deixaria o sistema sem
     *                                                      administrador ativo
     */
    public void update(
        String rawFullName,
        String rawCpf,
        String rawEmail,
        String rawMobilePhone,
        String rawNewRole,
        CaretakerRoster roster,
        Clock clock
    ) {
        Notification notification = new Notification();
        FullName.validate(rawFullName, notification);
        Cpf.validate(rawCpf, notification);
        Email.validate(rawEmail, notification);
        MobilePhone.validate(rawMobilePhone, notification);
        Role.validate(rawNewRole, notification);
        notification.throwIfAny(IdentityErrorCode.VALIDATION_FAILED);

        Role newRole = Role.of(rawNewRole);
        Cpf newCpf = Cpf.of(rawCpf);
        Email newEmail = Email.of(rawEmail);
        MobilePhone newMobilePhone = MobilePhone.of(rawMobilePhone);
        Notification conflicts = identifierConflicts(id(), newCpf, newEmail, newMobilePhone, isActive(), roster);
        if (newRole != Role.ADMINISTRATOR && isTheLastActiveAdministrator(roster)) {
            conflicts.add("role", IdentityErrorCode.LAST_ADMINISTRATOR, KEEP_AN_ADMINISTRATOR);
        }
        refuse(conflicts);

        this.fullName = FullName.of(rawFullName);
        this.cpf = newCpf;
        this.email = newEmail;
        this.mobilePhone = newMobilePhone;
        this.role = newRole;
        touch(clock);
    }

    /**
     * Inativa o responsavel. Nao remove nada: o historico permanece consultavel (FR-018).
     *
     * <p>Inativar quem ja esta inativo nao muda nada, nem o instante da ultima alteracao.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando o responsavel e o ultimo
     *                                                      administrador ativo (FR-019)
     */
    public void deactivate(CaretakerRoster roster, Clock clock) {
        if (!isActive()) {
            return;
        }
        if (isTheLastActiveAdministrator(roster)) {
            refuse(new Notification().add("status", IdentityErrorCode.LAST_ADMINISTRATOR, KEEP_AN_ADMINISTRATOR));
        }
        this.status = CaretakerStatus.INACTIVE;
        // O inativo nao recupera a senha: o link pendente deixa de valer (FR-007 da 012).
        this.passwordRecovery = null;
        touch(clock);
    }

    /**
     * Reativa o responsavel.
     *
     * <p>Enquanto ele esteve inativo, o e-mail e o celular dele ficaram livres, e outro ativo pode
     * te-los recebido: nesse caso a reativacao e recusada (FR-016).
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando outro ativo ja usa o e-mail ou o
     *                                                      celular
     */
    public void reactivate(CaretakerRoster roster, Clock clock) {
        if (isActive()) {
            return;
        }
        refuse(identifierConflicts(id(), cpf, email, mobilePhone, true, roster));
        this.status = CaretakerStatus.ACTIVE;
        touch(clock);
    }

    /**
     * Devolve a administracao a quem tem o CPF configurado para o administrador inicial, quando o
     * sistema ficou sem nenhum administrador ativo (FR-025).
     *
     * <p>E a saida da instalacao que perdeu o ultimo administrador: sem ela, a semeadura tentava
     * cadastrar de novo a mesma pessoa, esbarrava no proprio CPF, e a aplicacao recusava subir. O
     * responsavel volta ativo e administrador, com a senha provisoria configurada e a troca
     * obrigatoria no primeiro acesso, como o administrador criado pela semeadura.
     *
     * <p>Enquanto ele esteve inativo, o e-mail e o celular dele ficaram livres; se outro ativo os
     * recebeu, a restauracao e recusada, como a reativacao (FR-016).
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando a senha provisoria viola a politica
     *                                                      ou quando outro ativo ja usa o e-mail ou o
     *                                                      celular
     */
    public void restoreAsInitialAdministrator(
        String rawPassword, PasswordHasher hasher, CaretakerRoster roster, Clock clock) {
        Notification notification = new Notification();
        PasswordPolicy.validate(rawPassword, "password", email.value(), cpf.value(), notification);
        notification.throwIfAny(IdentityErrorCode.VALIDATION_FAILED);
        refuse(identifierConflicts(id(), cpf, email, mobilePhone, true, roster));

        this.passwordHash = hasher.hash(rawPassword);
        this.role = Role.ADMINISTRATOR;
        this.status = CaretakerStatus.ACTIVE;
        this.mustChangePassword = true;
        this.passwordRecovery = null;
        touch(clock);
    }

    /**
     * Se o responsavel e, agora, o unico administrador ativo.
     *
     * <p>Pergunta pelo proprio identificador no conjunto dos ativos, e nao so pela quantidade: a copia
     * em maos pode estar vencida. Se outra operacao ja o inativou ou rebaixou, ele nao esta no
     * conjunto e nao e "o ultimo"; a gravacao segue, a versao da linha a recusa, e a nova tentativa
     * decide sobre o estado atual. Contando so os ativos, a recusa vinha sobre um estado que ja nao
     * existia, e recusa nao grava nada, entao a versao nunca era conferida.
     *
     * <p>A copia que diz que ele nao e administrador ativo nem pergunta: assim a edicao de um usuario
     * comum nao trava as linhas dos administradores.
     */
    private boolean isTheLastActiveAdministrator(CaretakerRoster roster) {
        if (!isActive() || role != Role.ADMINISTRATOR) {
            return false;
        }
        Set<CaretakerId> active = roster.activeAdministrators();
        return active.contains(id()) && active.size() <= 1;
    }

    /**
     * Os identificadores que outro responsavel ja tem.
     *
     * <p>O CPF conflita com qualquer outro, ativo ou nao: a mesma pessoa nao existe duas vezes. E-mail
     * e celular so conflitam entre ativos (FR-016), e so para quem esta ativo ou vai ficar: um
     * inativo nao entra no sistema, e por isso nao disputa identificador de acesso.
     */
    private static Notification identifierConflicts(
        CaretakerId self, Cpf cpf, Email email, MobilePhone mobilePhone, boolean active, CaretakerRoster roster) {
        Notification conflicts = new Notification();
        if (roster.isCpfTakenByAnother(cpf, self)) {
            conflicts.add("cpf", IdentityErrorCode.CPF_ALREADY_IN_USE, "Já existe um responsável com este CPF.");
        }
        if (active && roster.isEmailTakenByAnotherActive(email, self)) {
            conflicts.add("email", IdentityErrorCode.EMAIL_ALREADY_IN_USE, "Já existe um responsável ativo com este e-mail.");
        }
        if (active && roster.isMobilePhoneTakenByAnotherActive(mobilePhone, self)) {
            conflicts.add(
                "mobilePhone",
                IdentityErrorCode.MOBILE_PHONE_ALREADY_IN_USE,
                "Já existe um responsável ativo com este celular.");
        }
        return conflicts;
    }

    /**
     * Recusa com todos os conflitos de uma vez.
     *
     * <p>Cada conflito e uma recusa por si, com o proprio codigo. O codigo da operacao e o do primeiro,
     * na ordem do formulario, e a mensagem geral tambem.
     */
    private static void refuse(Notification conflicts) {
        if (!conflicts.hasErrors()) {
            return;
        }
        Violation first = conflicts.violations().getFirst();
        String message = first.code() == IdentityErrorCode.LAST_ADMINISTRATOR ? LAST_ADMINISTRATOR_REFUSAL : first.message();
        throw new DomainException(first.code(), message, conflicts.violations());
    }

    private void touch(Clock clock) {
        this.updatedAt = clock.instant();
    }

    public boolean isActive() {
        return status == CaretakerStatus.ACTIVE;
    }

    public FullName fullName() {
        return fullName;
    }

    public Cpf cpf() {
        return cpf;
    }

    public Email email() {
        return email;
    }

    public MobilePhone mobilePhone() {
        return mobilePhone;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    public CaretakerStatus status() {
        return status;
    }

    /** O tema em que o responsavel ve o Ovyx (feature 011). */
    public ThemePreference themePreference() {
        return themePreference;
    }

    /**
     * Escolhe o proprio tema (FR-005 da 011). E uma preferencia de exibicao, e nao um dado de cadastro: nao muda o
     * instante da ultima alteracao, que a lista de responsaveis mostra.
     *
     * <p>O inativo e recusado com {@code CARETAKER_UNAVAILABLE}, como na troca da propria senha: a sessao pode
     * sobreviver a inativacao.
     *
     * @throws DomainException quando o responsavel esta inativo ou quando o texto nao e um dos tres temas
     */
    public void chooseTheme(String rawTheme) {
        if (!isActive()) {
            throw new DomainException(
                    IdentityErrorCode.CARETAKER_UNAVAILABLE, IdentityErrorCode.CARETAKER_UNAVAILABLE_MESSAGE);
        }
        this.themePreference = ThemePreference.of(rawTheme);
    }

    /**
     * Emite o link de recuperacao da senha (US1 da 012): guarda o resumo do codigo e o fim da validade, 30 minutos
     * depois do pedido. O link anterior, se havia, deixa de valer: so o mais recente vale (FR-006).
     *
     * <p>E um pedido de quem esqueceu a senha, e nao uma alteracao de cadastro: nao muda o instante da ultima
     * alteracao.
     *
     * <p>A conta recebe no maximo 3 links por hora (FR-014): o quarto e recusado, e o link anterior continua valendo.
     *
     * @throws DomainException com {@code CARETAKER_UNAVAILABLE}, quando o responsavel esta inativo, ou com
     *     {@code RECOVERY_LIMIT_REACHED}, quando a conta ja recebeu 3 links na hora
     */
    public void issuePasswordRecovery(RecoveryToken token, Clock clock) {
        if (!isActive()) {
            throw new DomainException(
                    IdentityErrorCode.CARETAKER_UNAVAILABLE, IdentityErrorCode.CARETAKER_UNAVAILABLE_MESSAGE);
        }
        Instant now = clock.instant();
        this.recoveryAllowance = recoveryAllowance.count(now);
        this.passwordRecovery = PasswordRecovery.issue(token, now);
    }

    /**
     * Redefine a senha pelo link de recuperacao (US2 da 012).
     *
     * <p>O link precisa ser o pendente, ainda valido, de um responsavel ativo; qualquer outro caso e a mesma recusa,
     * {@code RECOVERY_LINK_INVALID}, sem dizer o motivo. A nova senha segue a politica da troca da propria senha, com
     * todas as violacoes de uma vez em {@code newPassword}, e a recusa dela nao gasta o link.
     *
     * <p>Aceita, a redefinicao:
     *
     * <ul>
     *   <li>troca a senha e anula o link, que serve uma unica vez;
     *   <li>retira a obrigacao da senha provisoria: quem recuperou a senha ja escolheu a sua;
     *   <li>soma 1 na geracao de sessao, o que encerra as sessoes abertas antes dela (R-005).
     * </ul>
     *
     * @throws DomainException com {@code RECOVERY_LINK_INVALID}, quando o link nao vale, ou com
     *     {@code VALIDATION_FAILED}, quando a nova senha viola a politica
     */
    public void recoverPassword(String rawToken, String newPassword, PasswordHasher hasher, Clock clock) {
        refuseUnlessTheLinkHolds(rawToken, clock);

        Notification notification = new Notification();
        PasswordPolicy.validate(newPassword, "newPassword", email.value(), cpf.value(), notification);
        notification.throwIfAny(IdentityErrorCode.VALIDATION_FAILED);

        this.passwordHash = hasher.hash(newPassword);
        this.mustChangePassword = false;
        this.passwordRecovery = null;
        this.sessionGeneration = sessionGeneration + 1;
        touch(clock);
    }

    /**
     * Confere o link de recuperacao, sem muda-lo (US2 da 012): a tela de redefinicao diz logo, ao abrir, se ele nao
     * vale mais.
     *
     * @throws DomainException com {@code RECOVERY_LINK_INVALID}, quando o link nao vale
     */
    public void checkRecovery(String rawToken, Clock clock) {
        refuseUnlessTheLinkHolds(rawToken, clock);
    }

    /**
     * Se o link deste codigo ainda e o pendente: nao foi anulado nem substituido por um mais novo (FR-006). Nao olha o
     * vencimento, porque serve a quem ja tem o codigo emitido em maos, como o envio do e-mail.
     */
    public boolean holdsPendingRecovery(RecoveryToken token) {
        return passwordRecovery != null && passwordRecovery.tokenHash().equals(token.hash());
    }

    private void refuseUnlessTheLinkHolds(String rawToken, Clock clock) {
        RecoveryToken token = RecoveryToken.of(rawToken);
        if (!isActive() || passwordRecovery == null || !passwordRecovery.isValidFor(token.hash(), clock.instant())) {
            throw new DomainException(
                    IdentityErrorCode.RECOVERY_LINK_INVALID, IdentityErrorCode.RECOVERY_LINK_INVALID_MESSAGE);
        }
    }

    public boolean mustChangePassword() {
        return mustChangePassword;
    }

    /** O link de recuperacao pendente, ou {@code null} quando nao ha nenhum (feature 012). */
    public PasswordRecovery passwordRecovery() {
        return passwordRecovery;
    }

    /** Os links de recuperacao contados na hora, para o limite de 3 (feature 012). */
    public RecoveryAllowance recoveryAllowance() {
        return recoveryAllowance;
    }

    /** A geracao de sessao: a sessao aberta com outra e encerrada (R-005 da 012). */
    public int sessionGeneration() {
        return sessionGeneration;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    /**
     * Nunca inclui a senha nem o hash.
     */
    @Override
    public String toString() {
        return "Caretaker[id=" + id() + ", email=" + email + ", role=" + role + ", status=" + status + "]";
    }
}
