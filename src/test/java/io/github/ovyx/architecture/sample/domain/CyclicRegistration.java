package io.github.ovyx.architecture.sample.domain;

/**
 * Dominio com ciclo de chamadas, para o autoteste da suite de arquitetura.
 *
 * <p>{@code register} consulta {@code confirm} e depois recusa; {@code confirm} volta a chamar
 * {@code register}. Quem chama {@code confirm} tambem pode receber a recusa — e a regra so percebe
 * isso se atravessar o ciclo sem depender da ordem em que avalia os dois lados.
 */
public final class CyclicRegistration {

    private CyclicRegistration() {}

    public static String register(String raw, int depth) {
        if (depth > 0) {
            confirm(raw, depth - 1);
        }
        return Registration.of(raw);
    }

    public static String confirm(String raw, int depth) {
        return depth > 0 ? register(raw, depth - 1) : raw;
    }
}
