package io.github.ovyx.identity.domain;

import io.github.ovyx.shared.domain.ErrorCode;

/**
 * Codigos estaveis das regras do contexto Identity.
 *
 * <p>O codigo e o contrato: o cliente distingue a regra violada por ele, nunca pelo texto da
 * mensagem, que pode mudar sem aviso.
 */
public enum IdentityErrorCode implements ErrorCode {

    /** Uma ou mais regras de formato ou de politica foram violadas; os campos vem em {@code details}. */
    VALIDATION_FAILED,

    /**
     * Identificador inexistente, senha errada, responsavel inativo ou origem contida.
     *
     * <p>Codigo unico de proposito: distinguir as causas na resposta diria a quem sonda quais contas
     * existem (FR-002).
     */
    INVALID_CREDENTIALS,

    /**
     * O responsavel da sessao nao existe mais ou foi inativado com a sessao aberta.
     *
     * <p>A apresentacao encerra a sessao em vez de usa-la.
     */
    CARETAKER_UNAVAILABLE,

    /** Nao ha administrador ativo e a senha do administrador inicial nao foi informada (FR-025). */
    ADMINISTRATOR_PASSWORD_REQUIRED;

    @Override
    public String code() {
        return name();
    }
}
