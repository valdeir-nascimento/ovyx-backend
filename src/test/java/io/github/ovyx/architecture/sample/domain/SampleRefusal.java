package io.github.ovyx.architecture.sample.domain;

import io.github.ovyx.shared.domain.DomainException;
import java.util.Map;

/**
 * Recusa propria do dominio de amostra, para o autoteste da suite de arquitetura.
 *
 * <p>Tem duas formas de nascer que a regra precisa enxergar: a referencia ao construtor
 * ({@code orElseThrow(SampleRefusal::new)}) e a fabrica estatica, em que o {@code new} fica escondido
 * atras de uma chamada de metodo.
 */
public class SampleRefusal extends DomainException {

    public SampleRefusal() {
        super(SampleErrorCode.VALUE_REQUIRED, "Informe o valor.", Map.of());
    }

    public static SampleRefusal missingValue() {
        return new SampleRefusal();
    }
}
