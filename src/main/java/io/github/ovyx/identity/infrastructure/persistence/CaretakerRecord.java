package io.github.ovyx.identity.infrastructure.persistence;

import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.identity.domain.model.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Representacao do responsavel na tabela {@code caretaker}.
 *
 * <p>Existe separada do agregado {@code Caretaker} de proposito. Anotar o agregado com JPA violaria
 * o principio I, e faria o ORM ditar o modelo de dominio: identidade publica, construtor sem
 * argumentos, campos mutaveis e ausencia de invariante na construcao. O custo e este par de classes
 * mais um mapeador; o ganho e um dominio que nao responde ao Hibernate.
 *
 * <p>Esta classe nao tem comportamento de negocio. Se alguma regra aparecer aqui, ela esta no lugar
 * errado.
 */
@Entity
@Table(name = "caretaker")
public class CaretakerRecord {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "cpf", nullable = false, length = 11)
    private String cpf;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "mobile_phone", nullable = false, length = 11)
    private String mobilePhone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private CaretakerStatus status;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Exigido pelo Hibernate. */
    protected CaretakerRecord() {}

    CaretakerRecord(
            UUID id,
            String fullName,
            String cpf,
            String email,
            String mobilePhone,
            String passwordHash,
            Role role,
            CaretakerStatus status,
            boolean mustChangePassword,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
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

    UUID getId() {
        return id;
    }

    String getFullName() {
        return fullName;
    }

    String getCpf() {
        return cpf;
    }

    String getEmail() {
        return email;
    }

    String getMobilePhone() {
        return mobilePhone;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    Role getRole() {
        return role;
    }

    CaretakerStatus getStatus() {
        return status;
    }

    boolean isMustChangePassword() {
        return mustChangePassword;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Atualiza os campos mutaveis a partir de um agregado ja gravado. */
    void apply(
            String fullName,
            String cpf,
            String email,
            String mobilePhone,
            String passwordHash,
            Role role,
            CaretakerStatus status,
            boolean mustChangePassword,
            Instant updatedAt) {
        this.fullName = fullName;
        this.cpf = cpf;
        this.email = email;
        this.mobilePhone = mobilePhone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.mustChangePassword = mustChangePassword;
        this.updatedAt = updatedAt;
    }
}
