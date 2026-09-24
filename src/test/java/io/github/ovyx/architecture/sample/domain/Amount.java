package io.github.ovyx.architecture.sample.domain;

import io.github.ovyx.shared.domain.Notification;

/**
 * Objeto de valor que recusa no construtor, para o autoteste da suite de arquitetura.
 *
 * <p>Uma regra que olhasse so chamadas de metodo nao veria {@code new Amount(...)} recusar.
 *
 * @param value valor informado
 */
public record Amount(String value) {

    public Amount {
        Notification notification = new Notification();
        notification.requirePresent("value", value, SampleErrorCode.VALUE_REQUIRED, "Informe o valor.");
        notification.throwIfAny(SampleErrorCode.VALIDATION_FAILED);
    }
}
