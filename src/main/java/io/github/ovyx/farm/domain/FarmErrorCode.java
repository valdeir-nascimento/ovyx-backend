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

    // ---------------------------------------------------------------- recusas de operacao

    /** Uma ou mais regras de validacao foram violadas; cada uma vem, com o seu codigo, na recusa. */
    VALIDATION_FAILED,

    /** Identificador de setor inexistente ou malformado. */
    SECTOR_NOT_FOUND,

    /** Identificador de gaiola inexistente, malformado ou de outro setor. */
    CAGE_NOT_FOUND,

    // ---------------------------------------------------------------- conflitos com outros setores

    /** Outro setor ativo ja tem o nome, comparado sem maiusculas (FR-002). */
    SECTOR_NAME_IN_USE,

    /** Outra gaiola ativa do setor ja tem a bateria e o numero (FR-007). */
    CAGE_ALREADY_EXISTS,

    /** Operacao de gaiola num setor inativo: ele nao recebe gaiola nova nem reativa gaiola sozinha (FR-014, FR-015). */
    SECTOR_INACTIVE;

    @Override
    public String code() {
        return name();
    }
}
