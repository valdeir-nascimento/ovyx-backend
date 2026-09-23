package io.github.ovyx.identity.domain.model;

import io.github.ovyx.identity.domain.PasswordPolicy;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.identity.domain.valueobject.Cpf;
import io.github.ovyx.identity.domain.valueobject.Email;
import io.github.ovyx.identity.domain.valueobject.FullName;
import io.github.ovyx.identity.domain.valueobject.MobilePhone;
import io.github.ovyx.identity.domain.valueobject.PasswordHash;
import io.github.ovyx.shared.domain.AggregateRoot;
import io.github.ovyx.shared.domain.Notification;

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
 * </ol>
 *
 * <p>Unicidade de e-mail e celular (FR-016) e a regra do ultimo administrador (FR-019) sao
 * invariantes <em>entre</em> agregados: dependem de consulta ao repositorio e por isso vivem nos
 * tratadores, nao aqui.
 */
public final class Caretaker extends AggregateRoot<CaretakerId> {

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
        Role role,
        boolean mustChangePassword,
        PasswordHasher hasher,
        Clock clock
    ) {

        Notification notification = new Notification();

        FullName.validate(rawFullName, notification);
        Cpf.validate(rawCpf, notification);
        Email.validate(rawEmail, notification);
        MobilePhone.validate(rawMobilePhone, notification);
        PasswordPolicy.validate(rawPassword, "password", rawEmail, rawCpf, notification);
        if (role == null) {
            notification.add("role", IdentityErrorCode.ROLE_REQUIRED, "Informe o perfil.");
        }
        notification.throwIfAny(IdentityErrorCode.VALIDATION_FAILED);

        // Daqui em diante cada campo ja passou pelas proprias regras: construir os objetos de valor
        // nao recusa mais, e nenhum deles precisa existir pela metade.
        Instant now = clock.instant();
        return new Caretaker(
            CaretakerId.generate(),
            FullName.of(rawFullName),
            Cpf.of(rawCpf),
            Email.of(rawEmail),
            MobilePhone.of(rawMobilePhone),
            hasher.hash(rawPassword),
            role,
            CaretakerStatus.ACTIVE,
            mustChangePassword,
            now,
            now
        );
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
     * Inativa o responsavel. Nao remove nada: o historico permanece consultavel (FR-018).
     */
    public void deactivate(Clock clock) {
        this.status = CaretakerStatus.INACTIVE;
        touch(clock);
    }

    public void reactivate(Clock clock) {
        this.status = CaretakerStatus.ACTIVE;
        touch(clock);
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
