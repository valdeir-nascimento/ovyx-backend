package io.github.ovyx.identity.domain.valueobject;

import java.util.Locale;


public record AccessIdentifier(String value) {

    public static AccessIdentifier of(String typed) {
        if (typed == null || typed.isBlank()) {
            return new AccessIdentifier("");
        }

        String trimmed = typed.trim();

        if (trimmed.indexOf('@') >= 0) {
            return new AccessIdentifier(trimmed.toLowerCase(Locale.ROOT));
        }

        String withoutFormatting = trimmed.replaceAll(MobilePhone.FORMATTING_CHARACTERS, "");
        if (!withoutFormatting.isEmpty() && withoutFormatting.chars().allMatch(Character::isDigit)) {
            return new AccessIdentifier(withoutFormatting);
        }

        return new AccessIdentifier(trimmed.toLowerCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return value;
    }
}
