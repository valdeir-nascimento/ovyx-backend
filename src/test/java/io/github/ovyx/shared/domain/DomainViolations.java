package io.github.ovyx.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.List;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/**
 * Le as violacoes de uma recusa do dominio.
 *
 * <p>Os testes afirmam sobre o campo e o codigo da regra, e nao sobre o tipo da excecao nem sobre o
 * texto da mensagem: e isso que quebra quando a regra muda, e nao quando o texto e reescrito.
 */
public final class DomainViolations {

    private DomainViolations() {}

    /** Executa o que deve ser recusado e devolve as violacoes, regra a regra. */
    public static List<Violation> violationsOf(ThrowingCallable refused) {
        return refusalOf(refused).violations();
    }

    /** Executa o que deve ser recusado e devolve o que a API publica: campo -> mensagens em portugues. */
    public static Map<String, String> detailsOf(ThrowingCallable refused) {
        return refusalOf(refused).details();
    }

    /** Executa o que deve ser recusado e devolve o codigo da recusa, o que chega ao cliente em {@code code}. */
    public static ErrorCode refusalCodeOf(ThrowingCallable refused) {
        return refusalOf(refused).errorCode();
    }

    /** Executa o que deve ser recusado e devolve a mensagem geral da recusa, o que chega em {@code detail}. */
    public static String refusalMessageOf(ThrowingCallable refused) {
        return refusalOf(refused).getMessage();
    }

    private static DomainException refusalOf(ThrowingCallable refused) {
        DomainException thrown = catchThrowableOfType(DomainException.class, refused);

        assertThat(thrown).as("esperava que o domínio recusasse").isNotNull();
        return thrown;
    }
}
