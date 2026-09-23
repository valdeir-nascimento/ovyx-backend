package io.github.ovyx.architecture.sample.domain;

/**
 * Amostra que as regras de documentacao NAO podem reprovar.
 *
 * <p>Fora de {@code presentation}, {@code Controller} e so uma palavra do nome — aqui, o controle de
 * um sinal —, sem rota nem documentacao. Existe apenas para {@code ArchitectureRulesSelfCheckTest}
 * provar que o sufixo nao gera falso positivo.
 */
public class SignalController {

    public boolean isGreen() {
        return true;
    }
}
