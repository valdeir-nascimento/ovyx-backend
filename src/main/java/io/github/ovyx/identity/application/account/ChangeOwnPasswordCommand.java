package io.github.ovyx.identity.application.account;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.shared.application.Command;

public record ChangeOwnPasswordCommand(
    CaretakerId caretakerId,
    String currentPassword,
    String newPassword
) implements Command<CaretakerId> {

    @Override
    public String toString() {
        return "ChangeOwnPasswordCommand[caretakerId=" + caretakerId + ", currentPassword=****, newPassword=****]";
    }
}
