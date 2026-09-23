package io.github.ovyx.architecture.violation.domain.port;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Porta de saida declarada como classe concreta em vez de interface, violando o principio II.
 * Existe apenas para o autoteste da suite de arquitetura.
 */
public class NotAnInterface {

    public void save(String anything) {
        // sem implementacao: a classe existe apenas para ser reprovada pela regra
    }
}
