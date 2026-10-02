package io.github.ovyx.farm.domain;

import io.github.ovyx.shared.domain.ErrorCode;

/**
 * Codigos estaveis das regras do contexto farm (data-model.md, secao Codigos de recusa).
 *
 * <p>O codigo e o contrato: quem reage a recusa distingue a regra violada por ele, nunca pelo texto
 * da mensagem, que pode mudar sem aviso.
 *
 * <p>Ha dois niveis, como no identity. Os codigos de recusa identificam a operacao recusada e chegam
 * ao cliente no campo {@code code}. Os codigos de regra identificam cada violacao dentro de uma
 * recusa de validacao, um por regra.
 */
public enum FarmErrorCode implements ErrorCode {

    // ---------------------------------------------------------------- regras de validacao

    SECTOR_NAME_REQUIRED,
    SECTOR_NAME_TOO_SHORT,
    SECTOR_NAME_TOO_LONG,

    SECTOR_DESCRIPTION_TOO_LONG,

    CAGE_BATTERY_REQUIRED,
    /** Fora de 1 a 3 letras ou digitos: uma violacao so, porque a pessoa corrige a bateria inteira. */
    CAGE_BATTERY_INVALID,

    CAGE_NUMBER_REQUIRED,
    CAGE_NUMBER_NOT_INTEGER,
    CAGE_NUMBER_OUT_OF_RANGE,

    BIRD_COUNT_REQUIRED,
    BIRD_COUNT_NOT_INTEGER,
    BIRD_COUNT_OUT_OF_RANGE,

    FEED_FORMULA_NAME_REQUIRED,
    FEED_FORMULA_NAME_TOO_SHORT,
    FEED_FORMULA_NAME_TOO_LONG,

    /** Ausente no campo {@code pricePerKg}. */
    PRICE_REQUIRED,
    /** Nao e um valor em reais com ate duas casas decimais, como "2,855" ou "1.000,00". */
    PRICE_INVALID,
    PRICE_OUT_OF_RANGE,

    EXPECTED_INTAKE_REQUIRED,
    EXPECTED_INTAKE_NOT_INTEGER,
    EXPECTED_INTAKE_OUT_OF_RANGE,

    FEED_FORMULA_DESCRIPTION_TOO_LONG,

    /** So um limite da faixa de peso de referencia do setor (feature 005): os dois vao juntos. */
    REFERENCE_WEIGHT_INCOMPLETE,
    /** Limite da faixa que nao e inteiro de 1 a 10.000 gramas. */
    REFERENCE_WEIGHT_INVALID,
    /** Minimo da faixa igual ou acima do maximo. */
    REFERENCE_WEIGHT_INVERTED,

    /** Ausente no campo {@code layingRateTarget}: a meta de produtividade e obrigatoria no setor (008). */
    LAYING_RATE_TARGET_REQUIRED,
    /** Nao e uma porcentagem com ate uma casa decimal, como "82,55" ou "85%". */
    LAYING_RATE_TARGET_INVALID,
    /** Fora de 1% a 100%. */
    LAYING_RATE_TARGET_OUT_OF_RANGE,
    /** Nao e um dia da semana, de {@code MONDAY} a {@code SUNDAY}, no campo {@code weighingDay} (010). */
    WEIGHING_DAY_INVALID,

    /** Peso medio da pesagem ausente (feature 005). */
    WEIGHT_REQUIRED,
    /** Peso que nao e um numero de gramas com ate uma casa decimal. */
    WEIGHT_INVALID,
    /** Peso fora de 1 a 10.000 gramas. */
    WEIGHT_OUT_OF_RANGE,

    /** Data da pesagem ausente. */
    WEIGHED_ON_REQUIRED,
    /** Data da pesagem que nao e uma data ISO. */
    WEIGHED_ON_INVALID,
    /** Data da pesagem depois de hoje, no fuso da granja. */
    WEIGHED_ON_IN_FUTURE,

    // ---------------------------------------------------------------- recusas de operacao

    /** Uma ou mais regras de validacao foram violadas; cada uma vem, com o seu codigo, na recusa. */
    VALIDATION_FAILED,

    /** Identificador de setor inexistente ou malformado. */
    SECTOR_NOT_FOUND,

    /** Identificador de gaiola inexistente, malformado ou de outro setor. */
    CAGE_NOT_FOUND,

    /** Identificador de formula de racao inexistente ou malformado (feature 004). */
    FEED_FORMULA_NOT_FOUND,

    /** Identificador de pesagem inexistente, malformado, de outra gaiola ou de uma pesagem anulada (feature 005). */
    WEIGHING_NOT_FOUND,

    // ---------------------------------------------------------------- conflitos com outros setores

    /** Outro setor ativo ja tem o nome, comparado sem maiusculas (FR-002). */
    SECTOR_NAME_IN_USE,

    /** Outra gaiola ativa do setor ja tem a bateria e o numero (FR-007). */
    CAGE_ALREADY_EXISTS,

    /**
     * Outra formula, ativa ou inativa, ja tem o nome, comparado sem maiusculas (R-011 da 004): a formula
     * da nome aos custos do passado, e duas com o mesmo nome misturariam o historico.
     */
    FEED_FORMULA_NAME_IN_USE,

    /** A gaiola ja tem outra pesagem valida na data (FR-004 da 005). */
    WEIGHING_DATE_IN_USE,

    /** Operacao de gaiola num setor inativo: ele nao recebe gaiola nova nem reativa gaiola sozinha (FR-014, FR-015). */
    SECTOR_INACTIVE,

    /** Pesagem de uma gaiola inativa: as pesagens dela so se consultam (FR-008 da 005). */
    CAGE_INACTIVE;

    @Override
    public String code() {
        return name();
    }
}
