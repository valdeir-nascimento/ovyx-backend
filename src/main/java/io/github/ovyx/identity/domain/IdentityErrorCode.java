package io.github.ovyx.identity.domain;

import io.github.ovyx.shared.domain.ErrorCode;

/**
 * Codigos estaveis das regras do contexto Identity.
 *
 * <p>O codigo e o contrato: quem reage a recusa distingue a regra violada por ele, nunca pelo texto
 * da mensagem, que pode mudar sem aviso.
 *
 * <p>Ha dois niveis. Os codigos de recusa ({@link #VALIDATION_FAILED} e os demais do fim da lista)
 * identificam a operacao recusada e chegam ao cliente no campo {@code code}. Os codigos de regra
 * identificam cada violacao dentro de uma recusa de validacao, um por regra.
 */
public enum IdentityErrorCode implements ErrorCode {

    // ---------------------------------------------------------------- regras de validacao

    FULL_NAME_REQUIRED,
    FULL_NAME_TOO_SHORT,
    FULL_NAME_TOO_LONG,
    FULL_NAME_WITHOUT_LETTER,

    CPF_REQUIRED,
    /** Formato, sequencia repetida ou digito verificador: uma violacao so, porque a pessoa corrige o CPF inteiro. */
    CPF_INVALID,

    EMAIL_REQUIRED,
    EMAIL_TOO_LONG,
    EMAIL_MALFORMED,

    MOBILE_PHONE_REQUIRED,
    /** Quantidade de digitos, DDD ou letra: uma violacao so, pelo mesmo motivo do CPF. */
    MOBILE_PHONE_INVALID,

    PASSWORD_REQUIRED,
    PASSWORD_TOO_SHORT,
    PASSWORD_TOO_LONG,
    PASSWORD_WITHOUT_LETTER,
    PASSWORD_WITHOUT_DIGIT,
    PASSWORD_EQUALS_IDENTIFIER,

    CURRENT_PASSWORD_REQUIRED,
    CURRENT_PASSWORD_INCORRECT,

    ROLE_REQUIRED,
    ROLE_INVALID,

    // ---------------------------------------------------------------- recusas de operacao

    /** Uma ou mais regras de validacao foram violadas; cada uma vem, com o seu codigo, na recusa. */
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
    ADMINISTRATOR_PASSWORD_REQUIRED,

    /** O responsavel pedido nao existe. */
    CARETAKER_NOT_FOUND,

    // ---------------------------------------------------------------- conflitos com outros responsaveis
    // Cada um e, ao mesmo tempo, a regra violada num campo e a recusa da operacao: quando ha mais de
    // um conflito, todos vem em details, e o codigo da recusa e o do primeiro, na ordem do formulario.

    /** Outro responsavel, ativo ou nao, ja tem este CPF. */
    CPF_ALREADY_IN_USE,

    /** Outro responsavel ativo ja usa este e-mail (FR-016). */
    EMAIL_ALREADY_IN_USE,

    /** Outro responsavel ativo ja usa este celular (FR-016). */
    MOBILE_PHONE_ALREADY_IN_USE,

    /** A operacao deixaria o sistema sem administrador ativo (FR-019). */
    LAST_ADMINISTRATOR;

    @Override
    public String code() {
        return name();
    }
}
