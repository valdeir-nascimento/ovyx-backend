package io.github.ovyx.architecture.sample.domain;

/**
 * Implementacao que recusa, para o autoteste da suite de arquitetura.
 *
 * <p>Quem chama pela interface nao ve este codigo no ponto da chamada: a regra precisa segui-lo
 * pelas implementacoes.
 */
public final class StrictRegistrationPolicy implements RegistrationPolicy {

    @Override
    public String check(String raw) {
        return Registration.of(raw);
    }
}
