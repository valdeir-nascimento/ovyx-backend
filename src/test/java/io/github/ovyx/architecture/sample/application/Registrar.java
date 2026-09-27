package io.github.ovyx.architecture.sample.application;

import io.github.ovyx.architecture.sample.domain.Registration;

/**
 * Colaborador de aplicacao que nao e tratador, para o autoteste da suite de arquitetura.
 *
 * <p>Deixa a recusa subir, e esta correto: quem a traduz em {@code Failure} e o tratador, no ponto
 * em que o chama. A regra precisa segui-lo para saber que o tratador tem de capturar ali.
 */
public class Registrar {

    public String register(String raw) {
        return Registration.of(raw);
    }
}
