package io.github.ovyx.identity.application.recovery;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.shared.application.Command;
import java.time.Instant;

/**
 * O aviso por e-mail de que a senha foi redefinida (FR-012 da 012), adiado pela redefinicao para depois da confirmacao.
 *
 * @param caretakerId o responsavel da senha redefinida
 * @param recoveredAt o instante da redefinicao
 */
public record NotifyPasswordRecoveredCommand(CaretakerId caretakerId, Instant recoveredAt) implements Command<Void> {}
