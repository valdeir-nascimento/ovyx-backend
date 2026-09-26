package io.github.ovyx.production.domain;

import io.github.ovyx.shared.domain.ErrorCode;

/**
 * Codigos estaveis das regras do contexto production (data-model.md, secao Codigos de recusa).
 *
 * <p>O codigo e o contrato: quem reage a recusa distingue a regra violada por ele, nunca pelo texto da
 * mensagem. Os codigos que o farm tambem usa ({@code SECTOR_NOT_FOUND}, {@code SECTOR_INACTIVE}) tem o
 * mesmo nome e o mesmo sentido, para o cliente tratar do mesmo jeito (R-009).
 *
 * <p>Ha dois niveis, como no farm. Os codigos de recusa identificam a operacao recusada e chegam ao
 * cliente no campo {@code code}. Os codigos de regra identificam cada violacao dentro de uma recusa de
 * validacao, um por regra.
 */
public enum ProductionErrorCode implements ErrorCode {

    // ---------------------------------------------------------------- regras de validacao
    COLLECTION_DATE_REQUIRED,
    COLLECTION_DATE_INVALID,
    COLLECTION_DATE_IN_FUTURE,
    COLLECTION_TIME_REQUIRED,
    COLLECTION_TIME_INVALID,
    OPENING_BIRD_COUNT_REQUIRED,
    OPENING_BIRD_COUNT_NOT_INTEGER,
    OPENING_BIRD_COUNT_OUT_OF_RANGE,
    FLOCK_AGE_REQUIRED,
    FLOCK_AGE_NOT_INTEGER,
    FLOCK_AGE_OUT_OF_RANGE,
    REPORT_NOTE_TOO_LONG,
    EGGS_REQUIRED,
    EGGS_NOT_INTEGER,
    EGGS_OUT_OF_RANGE,
    EGG_GRADE_NOT_INTEGER,
    EGG_GRADE_OUT_OF_RANGE,

    /** A classificacao soma mais que os ovos coletados (invariante 4); a violacao vai no campo dos ovos. */
    EGG_GRADES_EXCEED_EGGS,
    DEATHS_NOT_INTEGER,
    DEATHS_OUT_OF_RANGE,
    CULLS_NOT_INTEGER,
    CULLS_OUT_OF_RANGE,
    MORTALITY_NOTE_TOO_LONG,

    /** Mortes e descartes de uma gaiola passam das aves dela (invariante 5); no campo das mortes. */
    REMOVALS_EXCEED_CAGE_BIRDS,

    /**
     * Mortes e descartes do dia passariam das aves do inicio do dia (invariante 5): no campo das mortes no
     * lancamento, e no das aves na correcao.
     */
    REMOVALS_EXCEED_OPENING_BIRDS,

    // ---------------------------------------------------------------- recusas de operacao
    /** Uma ou mais regras de validacao foram violadas; cada uma vem, com o seu codigo, na recusa. */
    VALIDATION_FAILED,

    /** Outro relatorio do setor ja tem a data da coleta (FR-002). */
    DAILY_REPORT_ALREADY_EXISTS,

    /** Escrita num relatorio de setor inativo (FR-020). */
    SECTOR_INACTIVE,

    /** Abertura de relatorio num setor sem gaiola ativa (R-015). */
    SECTOR_WITHOUT_ACTIVE_CAGES,

    /** Confirmacao de dia sem ocorrencia num relatorio com morte ou descarte lancado (invariante 6). */
    MORTALITY_ALREADY_RECORDED,

    /** Identificador de setor inexistente ou malformado. */
    SECTOR_NOT_FOUND,

    /** Identificador de relatorio inexistente, malformado ou de outro setor. */
    DAILY_REPORT_NOT_FOUND,

    /** Identificador de gaiola inexistente, malformado ou fora do relatorio. */
    CAGE_NOT_FOUND;

    @Override
    public String code() {
        return name();
    }
}
