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
    SIGNED_OUT
}
