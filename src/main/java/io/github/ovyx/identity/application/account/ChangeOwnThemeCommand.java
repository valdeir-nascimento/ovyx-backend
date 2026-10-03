package io.github.ovyx.identity.application.account;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.shared.application.Command;

/**
 * Troca do proprio tema (feature 011): o responsavel da sessao escolhe o tema dele.
 *
 * @param theme o tema como chegou, {@code LIGHT}, {@code DARK} ou {@code SYSTEM}; quem valida e o agregado
 */
public record ChangeOwnThemeCommand(CaretakerId caretakerId, String theme) implements Command<CaretakerId> {}
