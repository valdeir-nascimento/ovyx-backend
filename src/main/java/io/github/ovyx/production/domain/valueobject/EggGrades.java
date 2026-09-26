package io.github.ovyx.production.domain.valueobject;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.WholeNumber;
import java.util.List;
import java.util.function.Function;

/**
 * A classificacao dos ovos que sairam fora do padrao numa gaiola (FR-007): pequenos, jumbo, sujos,
 * trincados, com sangue e anormais. Cada uma e inteiro de 0 a 1.000; em branco vale zero.
 *
 * <p>Os trincados, os com sangue e os anormais nao podem ser vendidos; os pequenos, os jumbo e os sujos
 * saem do padrao, mas sao vendidos (spec, Key Entities).
 */
public record EggGrades(int small, int jumbo, int dirty, int cracked, int bloodSpot, int abnormal) {

    private static final int MINIMUM = 0;
    private static final int MAXIMUM = 1000;

    /** Cada classificacao: o campo, o nome na mensagem e onde ela esta no que foi digitado. */
    private static final List<Grade> GRADES = List.of(
            new Grade("small", "pequenos", Raw::small),
            new Grade("jumbo", "jumbo", Raw::jumbo),
            new Grade("dirty", "sujos", Raw::dirty),
            new Grade("cracked", "trincados", Raw::cracked),
            new Grade("bloodSpot", "ovos com sangue", Raw::bloodSpot),
            new Grade("abnormal", "anormais", Raw::abnormal));

    /** As classificacoes como foram digitadas; ausente ou em branco vale zero. */
    public record Raw(String small, String jumbo, String dirty, String cracked, String bloodSpot, String abnormal) {}

    private record Grade(String field, String name, Function<Raw, String> typed) {}

    /**
     * Registra no {@link Notification} a violacao de cada classificacao, no campo dela, sem lancar: nao
     * inteira ou fora da faixa, uma por classificacao.
     *
     * @return se todas as classificacoes sao validas: so entao a soma delas pode ser comparada aos ovos
     */
    public static boolean validate(Raw raw, Notification notification) {
        boolean valid = true;
        for (Grade grade : GRADES) {
            valid &= validate(grade, grade.typed().apply(raw), notification);
        }
        return valid;
    }

    /**
     * Cria a classificacao, recusando na hora com as violacoes de todas as classificacoes.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando alguma nao e inteira ou sai da faixa
     */
    public static EggGrades of(Raw raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(ProductionErrorCode.VALIDATION_FAILED);
        return new EggGrades(
                valueOf(raw.small()),
                valueOf(raw.jumbo()),
                valueOf(raw.dirty()),
                valueOf(raw.cracked()),
                valueOf(raw.bloodSpot()),
                valueOf(raw.abnormal()));
    }

    /** Todos os ovos fora do padrao. */
    public int total() {
        return small + jumbo + dirty + cracked + bloodSpot + abnormal;
    }

    /** Os que nao podem ser vendidos: trincados, com sangue e anormais. */
    public int unsellable() {
        return cracked + bloodSpot + abnormal;
    }

    private static boolean validate(Grade grade, String typed, Notification notification) {
        if (isBlank(typed)) {
            return true;
        }
        if (!WholeNumber.isInteger(typed)) {
            notification.add(
                    grade.field(),
                    ProductionErrorCode.EGG_GRADE_NOT_INTEGER,
                    "A quantidade de " + grade.name() + " deve ser um número inteiro.");
            return false;
        }
        if (!WholeNumber.within(typed, MINIMUM, MAXIMUM)) {
            notification.add(
                    grade.field(),
                    ProductionErrorCode.EGG_GRADE_OUT_OF_RANGE,
                    "A quantidade de " + grade.name() + " deve ficar entre 0 e 1.000.");
            return false;
        }
        return true;
    }

    private static int valueOf(String typed) {
        return isBlank(typed) ? 0 : WholeNumber.valueOf(typed).orElseThrow();
    }

    private static boolean isBlank(String typed) {
        return typed == null || typed.isBlank();
    }
}
