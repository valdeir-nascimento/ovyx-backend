package io.github.ovyx.architecture.violation.domain;

import io.github.ovyx.shared.domain.DomainException;
import io.github.ovyx.shared.domain.ErrorCode;
import java.util.Map;

/**
 * Dominio que recusa, para o autoteste da suite de arquitetura.
 *
 * <p>Nao e violacao por si: e o dominio se comportando como o principio III manda, lancando
 * {@code DomainException} com o campo recusado. Existe para que
 * {@link io.github.ovyx.architecture.violation.application.EscapingHandler} tenha o que chamar.
 */
public final class RefusingRegistration {

    private RefusingRegistration() {}

    public static String of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainException(SampleErrorCode.VALIDATION_FAILED, "Dados inválidos.", Map.of("field", "Informe o valor."));
        }
        return raw;
    }

    private enum SampleErrorCode implements ErrorCode {
        VALIDATION_FAILED;

        @Override
        public String code() {
            return name();
        }
    }
}
