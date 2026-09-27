package io.github.ovyx.architecture.sample.domain;

import java.util.function.Function;

/**
 * Implementacao de {@code Function} que recusa, para o autoteste da suite de arquitetura.
 *
 * <p>Nao e violacao: e um dominio legitimo. Existe para provar que a regra nao trata como recusa
 * toda chamada a {@code Function.apply} do projeto — como a que o {@code Result.map} faz — so
 * porque alguma implementacao, em algum lugar, recusa. Sem essa guarda, qualquer controller com
 * {@code result.map(...)} seria reprovado.
 */
public final class RefusingFormat implements Function<String, String> {

    @Override
    public String apply(String raw) {
        return Registration.of(raw);
    }
}
