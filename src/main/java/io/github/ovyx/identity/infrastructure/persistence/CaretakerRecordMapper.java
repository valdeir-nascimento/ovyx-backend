package io.github.ovyx.identity.infrastructure.persistence;

import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.valueobject.Cpf;
import io.github.ovyx.identity.domain.valueobject.Email;
import io.github.ovyx.identity.domain.valueobject.FullName;
import io.github.ovyx.identity.domain.valueobject.MobilePhone;
import io.github.ovyx.identity.domain.valueobject.PasswordHash;
import io.github.ovyx.identity.domain.valueobject.PasswordRecovery;
import io.github.ovyx.identity.domain.valueobject.RecoveryAllowance;

/**
 * Traducao entre o agregado e a linha da tabela.
 *
 * <p>Na volta usa {@code Caretaker.restore}, e nao a fabrica de cadastro: os dados ja foram
 * validados quando entraram. Revalidar na leitura faria o sistema recusar registros legitimos
 * depois de qualquer endurecimento de regra — um responsavel gravado sob a regra antiga viraria
 * um registro impossivel de carregar.
 */
final class CaretakerRecordMapper {

    private CaretakerRecordMapper() {}

    static CaretakerRecord toRecord(Caretaker caretaker) {
        CaretakerRecord record = new CaretakerRecord(
                caretaker.id().value(),
                caretaker.fullName().value(),
                caretaker.cpf().value(),
                caretaker.email().value(),
                caretaker.mobilePhone().value(),
                caretaker.passwordHash().value(),
                caretaker.role(),
                caretaker.status(),
                caretaker.mustChangePassword(),
                caretaker.themePreference(),
                caretaker.createdAt(),
                caretaker.updatedAt());
        applyRecoveryTo(record, caretaker);
        return record;
    }

    static void applyTo(CaretakerRecord record, Caretaker caretaker) {
        record.apply(
                caretaker.fullName().value(),
                caretaker.cpf().value(),
                caretaker.email().value(),
                caretaker.mobilePhone().value(),
                caretaker.passwordHash().value(),
                caretaker.role(),
                caretaker.status(),
                caretaker.mustChangePassword(),
                caretaker.themePreference(),
                caretaker.updatedAt());
        applyRecoveryTo(record, caretaker);
    }

    /** O link pendente, os pedidos contados e a geracao de sessao (feature 012). */
    private static void applyRecoveryTo(CaretakerRecord record, Caretaker caretaker) {
        PasswordRecovery recovery = caretaker.passwordRecovery();
        RecoveryAllowance allowance = caretaker.recoveryAllowance();
        record.applyRecovery(
                recovery == null ? null : recovery.tokenHash(),
                recovery == null ? null : recovery.expiresAt(),
                allowance.windowStartedAt(),
                allowance.requestsInWindow(),
                caretaker.sessionGeneration());
    }

    static Caretaker toDomain(CaretakerRecord record) {
        return Caretaker.restore(
                CaretakerId.of(record.getId()),
                new FullName(record.getFullName()),
                new Cpf(record.getCpf()),
                new Email(record.getEmail()),
                new MobilePhone(record.getMobilePhone()),
                PasswordHash.of(record.getPasswordHash()),
                record.getRole(),
                record.getStatus(),
                record.isMustChangePassword(),
                record.getThemePreference(),
                record.getRecoveryTokenHash() == null
                        ? null
                        : new PasswordRecovery(record.getRecoveryTokenHash(), record.getRecoveryExpiresAt()),
                new RecoveryAllowance(record.getRecoveryWindowStartedAt(), record.getRecoveryRequestsInWindow()),
                record.getSessionGeneration(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }
}
