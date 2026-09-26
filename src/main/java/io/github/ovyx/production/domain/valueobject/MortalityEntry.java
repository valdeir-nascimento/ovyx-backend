package io.github.ovyx.production.domain.valueobject;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.shared.domain.ErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.WholeNumber;

/**
 * A mortalidade de uma gaiola num dia (FR-011): as aves encontradas mortas, as descartadas — retiradas
 * do plantel — e uma observacao. Mortes e descartes sao inteiros de 0 a 1.000; em branco vale zero.
 *
 * <p>O limite pelas aves da gaiola e pelas do inicio do dia (invariante 5) depende do relatorio, e e o
 * {@code DailyReport} que o checa. A mortalidade nao muda as aves da gaiola (FR-015).
 *
 * @param deaths as aves encontradas mortas
 * @param culls as aves descartadas
 * @param note a observacao, ou {@code null}
 */
public record MortalityEntry(int deaths, int culls, MortalityNote note) {

    private static final int MINIMUM = 0;
    private static final int MAXIMUM = 1000;

    /**
     * Registra no {@link Notification} as violacoes da mortalidade, sem lancar: a das mortes, a dos
     * descartes e a da observacao, uma por campo.
     *
     * @return se os tres campos sao validos: so entao as somas podem ser comparadas as aves
     */
    public static boolean validate(String rawDeaths, String rawCulls, String rawNote, Notification notification) {
        boolean deaths = validateQuantity(
                "deaths",
                rawDeaths,
                notification,
                ProductionErrorCode.DEATHS_NOT_INTEGER,
                "As mortes devem ser um número inteiro.",
                ProductionErrorCode.DEATHS_OUT_OF_RANGE,
                "As mortes devem ficar entre 0 e 1.000.");
        boolean culls = validateQuantity(
                "culls",
                rawCulls,
                notification,
                ProductionErrorCode.CULLS_NOT_INTEGER,
                "Os descartes devem ser um número inteiro.",
                ProductionErrorCode.CULLS_OUT_OF_RANGE,
                "Os descartes devem ficar entre 0 e 1.000.");
        int before = notification.violations().size();
        MortalityNote.validate(rawNote, notification);
        return deaths && culls && notification.violations().size() == before;
    }

    /**
     * Cria a mortalidade, recusando na hora com todas as violacoes de campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando mortes ou descartes nao sao inteiros ou
     *     saem da faixa, ou quando a observacao passa de 500 caracteres
     */
    public static MortalityEntry of(String rawDeaths, String rawCulls, String rawNote) {
        Notification notification = new Notification();
        validate(rawDeaths, rawCulls, rawNote, notification);
        notification.throwIfAny(ProductionErrorCode.VALIDATION_FAILED);
        return new MortalityEntry(
                valueOf(rawDeaths), valueOf(rawCulls), MortalityNote.optionalOf(rawNote).orElse(null));
    }

    /** As aves que saem do plantel: mortes e descartes. */
    public int removals() {
        return deaths + culls;
    }

    /** Se houve morte ou descarte; zero e zero e lancamento sem ocorrencia. */
    public boolean hasOccurrence() {
        return removals() > 0;
    }

    private static boolean validateQuantity(
            String field,
            String raw,
            Notification notification,
            ErrorCode notInteger,
            String notIntegerMessage,
            ErrorCode outOfRange,
            String outOfRangeMessage) {
        if (isBlank(raw)) {
            return true;
        }
        if (!WholeNumber.isInteger(raw)) {
            notification.add(field, notInteger, notIntegerMessage);
            return false;
        }
        if (!WholeNumber.within(raw, MINIMUM, MAXIMUM)) {
            notification.add(field, outOfRange, outOfRangeMessage);
            return false;
        }
        return true;
    }

    private static int valueOf(String raw) {
        return isBlank(raw) ? 0 : WholeNumber.valueOf(raw).orElseThrow();
    }

    private static boolean isBlank(String raw) {
        return raw == null || raw.isBlank();
    }
}
