package io.github.ovyx.identity.domain.model;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.domain.Notification;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * A preferencia de tema do responsavel (R-001 da 011): claro, escuro ou igual ao sistema, que acompanha o tema do
 * aparelho. Todo responsavel comeca em {@link #SYSTEM}, o comportamento de antes da feature.
 *
 * <p>Chega como texto cru, sem distinguir maiusculas: o valor errado e recusado no campo {@code theme}, e nao como
 * erro de leitura do corpo.
 */
public enum ThemePreference {
    LIGHT,
    DARK,
    SYSTEM;

    private static final String FIELD = "theme";
    private static final String INVALID = "Escolha o tema claro, o escuro ou o igual ao sistema.";

    /** Registra no {@link Notification} a violacao do campo, sem lancar: ausente ou fora dos tres temas. */
    public static void validate(String raw, Notification notification) {
        if (parse(raw).isEmpty()) {
            notification.add(FIELD, IdentityErrorCode.THEME_INVALID, INVALID);
        }
    }

    /**
     * O tema do texto.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando o texto falta ou nao e um dos tres temas
     */
    public static ThemePreference of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(IdentityErrorCode.VALIDATION_FAILED);
        return parse(raw).orElseThrow();
    }

    private static Optional<ThemePreference> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String name = raw.strip().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(theme -> theme.name().equals(name)).findFirst();
    }
}
