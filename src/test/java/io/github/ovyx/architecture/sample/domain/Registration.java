package io.github.ovyx.architecture.sample.domain;

import io.github.ovyx.shared.domain.Notification;
import java.util.List;

/**
 * Dominio que recusa, para o autoteste da suite de arquitetura.
 *
 * <p>Nao e violacao: e o dominio se comportando como o principio III manda. Recusa pelo mesmo
 * caminho da producao — acumula no {@link Notification} e recusa em {@code throwIfAny} —, de modo
 * que a {@code DomainException} nasce dois niveis abaixo de quem o tratador chama. E esse caminho
 * que o autoteste precisa provar que a regra percorre.
 */
public final class Registration {

    private Registration() {}

    public static String of(String raw) {
        Notification notification = new Notification();
        notification.requirePresent("value", raw, SampleErrorCode.VALUE_REQUIRED, "Informe o valor.");
        notification.throwIfAny(SampleErrorCode.VALIDATION_FAILED);
        return raw;
    }

    /** Recusa por uma referencia de metodo, dentro do proprio dominio. */
    public static List<String> ofAll(List<String> raws) {
        return raws.stream().map(Registration::of).toList();
    }
}
