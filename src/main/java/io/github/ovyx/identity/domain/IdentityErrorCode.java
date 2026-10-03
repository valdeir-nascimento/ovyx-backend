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

    /** Nao e {@code LIGHT}, {@code DARK} nem {@code SYSTEM}, no campo {@code theme} (feature 011). */
    THEME_INVALID,

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
    /**
     * O link de recuperacao da senha nao vale: usado, vencido, substituido, anulado, alterado ou inexistente, ou a
     * origem passou do limite de tentativas (feature 012).
     *
     * <p>Codigo unico de proposito, como {@link #INVALID_CREDENTIALS}: distinguir as causas diria a quem sonda se o
     * link existe e de quem e.
     */
    RECOVERY_LINK_INVALID,
    /** A conta ja recebeu 3 links de recuperacao na hora contada (FR-014 da 012); so a auditoria ve esta recusa. */
    RECOVERY_LIMIT_REACHED,
    /** A sessao foi aberta antes de a senha ser redefinida pelo link; a apresentacao a encerra (R-005 da 012). */
    SESSION_REVOKED,

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

    /**
     * A mensagem de {@link #CARETAKER_UNAVAILABLE}. E a mesma frase para quem nao existe mais e para
     * quem foi inativado, em todo chamador, de proposito: a diferenca diria a quem sonda quais contas
     * existem.
     */
    public static final String CARETAKER_UNAVAILABLE_MESSAGE = "Responsável não encontrado ou inativo.";

    /** A mensagem de {@link #RECOVERY_LINK_INVALID}, a mesma para toda causa. */
    public static final String RECOVERY_LINK_INVALID_MESSAGE =
            "Este link de recuperação não vale mais. Peça um novo na tela de entrada.";

    /** A mensagem de {@link #RECOVERY_LIMIT_REACHED}, que so a auditoria e o log veem. */
    public static final String RECOVERY_LIMIT_REACHED_MESSAGE = "Limite de pedidos de recuperação atingido.";

    /** A mensagem de {@link #SESSION_REVOKED}. */
    public static final String SESSION_REVOKED_MESSAGE =
            "Sua senha foi redefinida e esta sessão foi encerrada. Entre com a nova senha.";

    @Override
    public String code() {
        return name();
    }
}
