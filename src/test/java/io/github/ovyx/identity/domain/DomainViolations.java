package io.github.ovyx.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.ovyx.shared.domain.DomainException;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/**
 * Le as violacoes de uma recusa do dominio.
 *
 * <p>Os testes afirmam sobre o campo e a mensagem que a pessoa vai ver, e nao sobre o tipo da
 * excecao: e isso que quebra quando a regra muda.
 */
public final class DomainViolations {

    private DomainViolations() {}

    /** Executa o que deve ser recusado e devolve os campos que vieram na recusa. */
    public static Map<String, String> of(ThrowingCallable refused) {
        DomainException thrown = catchThrowableOfType(DomainException.class, refused);

        assertThat(thrown).as("esperava que o domínio recusasse").isNotNull();
        return thrown.details();
    }
}
