package io.github.ovyx.shared.domain;

/**
 * Codigo estavel de erro de dominio.
 *
 * <p>
 * O codigo e o contrato testavel: causas diferentes precisam produzir codigos
 * diferentes, para que o consumidor distinga a regra violada sem depender do
 * texto da mensagem.
 * </p>
 */
public interface ErrorCode {

    /**
     * @return identificador estavel da regra violada, em SCREAMING_SNAKE_CASE.
     */
    String code();
}
