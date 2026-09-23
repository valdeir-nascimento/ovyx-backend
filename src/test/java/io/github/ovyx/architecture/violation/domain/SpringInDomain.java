package io.github.ovyx.architecture.violation.domain;

import org.springframework.stereotype.Component;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Classe de dominio anotada com Spring, violando o principio I. Existe apenas para
 * {@link io.github.ovyx.architecture.ArchitectureRulesSelfCheckTest} comprovar que a suite de
 * arquitetura reprova esta situacao.
 *
 * <p>Vive em {@code src/test}, entao nunca entra no artefato de producao nem e analisada por
 * {@code ArchitectureRulesTest}, que ignora classes de teste.
 */
@Component
public class SpringInDomain {

    public String describe() {
        return "dominio nao pode depender de framework";
    }
}
