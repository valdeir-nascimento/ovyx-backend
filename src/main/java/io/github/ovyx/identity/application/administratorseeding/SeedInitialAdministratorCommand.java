package io.github.ovyx.identity.application.administratorseeding;

import io.github.ovyx.shared.application.Command;

public record SeedInitialAdministratorCommand(
    String fullName,
    String cpf,
    String email,
    String mobilePhone,
    String password
) implements Command<SeedingOutcome> {

    @Override
    public String toString() {
        return "SeedInitialAdministratorCommand[fullName=" + fullName + ", cpf=" + cpf + ", email=" + email
            + ", mobilePhone=" + mobilePhone + ", password=****]";
    }
}
