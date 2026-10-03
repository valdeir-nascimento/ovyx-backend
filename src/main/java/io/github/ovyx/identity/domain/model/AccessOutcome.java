package io.github.ovyx.identity.domain.model;

/**
 * Resultado real de uma tentativa de acesso.
 *
 * <p>Estes valores existem <strong>apenas na trilha de auditoria</strong>. A resposta ao cliente e
 * sempre a mesma, qualquer que seja a causa (FR-002): distinguir "identificador inexistente" de
 * "senha incorreta" entregaria a quem sonda o sistema a confirmacao de que a conta existe.
 *
 * <p>Sem esta distincao registrada em algum lugar, porem, nao haveria como investigar um incidente
 * — dai a separacao entre o que se responde e o que se registra.
 */
public enum AccessOutcome {
    GRANTED,
    INVALID_CREDENTIALS,
    INACTIVE_CARETAKER,
    THROTTLED,
    SIGNED_OUT,
    // ---------------------------------------------------------------- recuperacao de senha (feature 012, R-007)
    // A resposta ao pedido e sempre a mesma; a causa real so existe aqui.
    /** Link emitido e e-mail aceito pelo servidor de envio. */
    RECOVERY_LINK_SENT,
    /** Nenhum responsavel com o e-mail informado. */
    RECOVERY_UNKNOWN_EMAIL,
    /** O responsavel do e-mail esta inativo: nao recebe link. */
    RECOVERY_INACTIVE,
    /** A conta ja recebeu 3 links na hora contada. */
    RECOVERY_LIMITED,
    /** A origem passou do limite de tentativas de recuperacao. */
    RECOVERY_THROTTLED,
    /** O servidor de envio recusou ou nao respondeu; o link ficou emitido. */
    RECOVERY_DELIVERY_FAILED,
    /** Senha redefinida pelo link. */
    PASSWORD_RECOVERED,
    /** Link que nao vale, na conferencia ou na redefinicao. */
    RECOVERY_LINK_REFUSED
}
