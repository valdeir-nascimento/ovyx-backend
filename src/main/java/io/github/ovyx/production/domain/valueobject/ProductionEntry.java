package io.github.ovyx.production.domain.valueobject;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.WholeNumber;
import java.util.Locale;
import java.util.Objects;

/**
 * A producao de uma gaiola num dia (FR-007, FR-008): os ovos coletados, obrigatorios, de 0 a 1.000, e a
 * classificacao dos que sairam fora do padrao, que nao soma mais que os ovos (invariante 4).
 *
 * <p>Zero ovos e lancamento, diferente de gaiola sem lancamento: a gaiola que nao botou foi contada.
 *
 * @param eggs os ovos coletados, de 0 a 1.000
 * @param grades a classificacao dos que sairam fora do padrao
 */
public record ProductionEntry(int eggs, EggGrades grades) {

    private static final String FIELD = "eggs";
    private static final int MINIMUM = 0;
    private static final int MAXIMUM = 1000;
    private static final Locale PORTUGUESE = Locale.forLanguageTag("pt-BR");

    public ProductionEntry {
        Objects.requireNonNull(grades, "grades");
    }

    /**
     * Registra no {@link Notification} as violacoes da producao, sem lancar: a dos ovos, a de cada
     * classificacao e, quando os campos sao validos, a da soma, no campo dos ovos.
     */
    public static void validate(String rawEggs, EggGrades.Raw rawGrades, Notification notification) {
        boolean eggsValid = validateEggs(rawEggs, notification);
        boolean gradesValid = EggGrades.validate(rawGrades, notification);
        if (!eggsValid || !gradesValid) {
            return;
        }
        int eggs = WholeNumber.valueOf(rawEggs).orElseThrow();
        int graded = EggGrades.of(rawGrades).total();
        if (graded > eggs) {
            notification.add(FIELD, ProductionErrorCode.EGG_GRADES_EXCEED_EGGS, exceedingMessage(graded, eggs));
        }
    }

    /**
     * Cria a producao, recusando na hora com todas as violacoes.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando os ovos faltam, nao sao inteiros ou saem
     *     da faixa, quando alguma classificacao e invalida, ou quando a classificacao passa dos ovos
     */
    public static ProductionEntry of(String rawEggs, EggGrades.Raw rawGrades) {
        Notification notification = new Notification();
        validate(rawEggs, rawGrades, notification);
        notification.throwIfAny(ProductionErrorCode.VALIDATION_FAILED);
        return new ProductionEntry(WholeNumber.valueOf(rawEggs).orElseThrow(), EggGrades.of(rawGrades));
    }

    /** Os ovos dentro do padrao: os coletados menos os classificados. */
    public int standardEggs() {
        return eggs - grades.total();
    }

    /** Os ovos que nao podem ser vendidos: trincados, com sangue e anormais. */
    public int unsellableEggs() {
        return grades.unsellable();
    }

    private static boolean validateEggs(String raw, Notification notification) {
        if (!notification.requirePresent(FIELD, raw, ProductionErrorCode.EGGS_REQUIRED, "Informe os ovos coletados.")) {
            return false;
        }
        if (!WholeNumber.isInteger(raw)) {
            notification.add(
                    FIELD, ProductionErrorCode.EGGS_NOT_INTEGER, "Os ovos coletados devem ser um número inteiro.");
            return false;
        }
        if (!WholeNumber.within(raw, MINIMUM, MAXIMUM)) {
            notification.add(
                    FIELD, ProductionErrorCode.EGGS_OUT_OF_RANGE, "Os ovos coletados devem ficar entre 0 e 1.000.");
            return false;
        }
        return true;
    }

    /** A mensagem nomeia os dois numeros, como o prototipo (R-013). */
    private static String exceedingMessage(int graded, int eggs) {
        String collected = eggs == 1 ? "o 1 ovo coletado" : "os " + countOf(eggs) + " ovos coletados";
        return "As classificações somam " + countOf(graded) + ", mais que " + collected + ".";
    }

    private static String countOf(int value) {
        return String.format(PORTUGUESE, "%,d", value);
    }
}
