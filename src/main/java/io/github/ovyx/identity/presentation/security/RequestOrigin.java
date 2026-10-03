package io.github.ovyx.identity.presentation.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * A origem da requisicao, usada pela contencao de tentativas e pela auditoria: a da entrada (FR-023 da 001) e a da
 * recuperacao de senha (FR-015 da 012).
 *
 * <p>Vem so de {@code getRemoteAddr()}, nunca de {@code X-Forwarded-For} lido a mao. Esse cabecalho e escrito pelo
 * cliente: aceita-lo deixava cada tentativa inventar a propria origem, ganhar uma chave de contencao nova e registrar
 * uma origem falsa na auditoria.
 *
 * <p>Atras de um proxy confiavel, a origem real e obtida por configuracao ({@code server.forward-headers-strategy}),
 * que o proprio container valida — e nao por esta classe.
 */
public final class RequestOrigin {

    /** Largura da coluna {@code origin} nas tabelas de auditoria e de contencao. */
    private static final int MAX_LENGTH = 60;

    private RequestOrigin() {}

    public static String of(HttpServletRequest request) {
        String address = request.getRemoteAddr();
        if (address == null || address.isBlank()) {
            return "desconhecida";
        }
        return address.length() <= MAX_LENGTH ? address : address.substring(0, MAX_LENGTH);
    }
}
