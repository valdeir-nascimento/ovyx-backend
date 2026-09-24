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
import io.github.ovyx.shared.domain.AggregateRoot;
import io.github.ovyx.shared.domain.DomainException;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Violation;

import java.time.Clock;
import java.time.Instant;

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
            mustChangePassword,
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
     * @throws io.github.ovyx.shared.domain.DomainException quando a senha atual nao confere ou a
     *                                                      nova viola a politica
     */
    public void changeOwnPassword(String currentPassword, String newPassword, PasswordHasher hasher, Clock clock) {

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
     * Edita com o perfil ja escolhido, para quem nao recebe texto digitado.
     *
     * @throws io.github.ovyx.shared.domain.DomainException nos mesmos casos da edicao por texto
     */
    public void update(
        String rawFullName,
        String rawCpf,
        String rawEmail,
        String rawMobilePhone,
        Role newRole,
        CaretakerRoster roster,
        Clock clock
    ) {
        update(rawFullName, rawCpf, rawEmail, rawMobilePhone, newRole == null ? null : newRole.name(), roster, clock);
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
        touch(clock);
    }

    private boolean isTheLastActiveAdministrator(CaretakerRoster roster) {
        return isActive() && role == Role.ADMINISTRATOR && roster.countActiveAdministrators() <= 1;
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

    public boolean mustChangePassword() {
        return mustChangePassword;
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
