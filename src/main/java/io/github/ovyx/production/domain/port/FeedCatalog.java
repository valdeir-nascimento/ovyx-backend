package io.github.ovyx.production.domain.port;

import io.github.ovyx.production.domain.model.CatalogFormula;
import io.github.ovyx.production.domain.model.FeedFormulaId;
import java.util.Optional;

/**
 * As formulas de racao, como o production precisa delas (R-004 da 004): uma camada anticorrupcao, como a
 * {@link FarmStructure}. O production nao depende do codigo do farm; o adaptador responde lendo a tabela
 * da formula.
 */
public interface FeedCatalog {

    /**
     * A formula, ativa ou inativa, com o preco e o consumo esperado atuais, ou nenhuma, se nao existe.
     *
     * <p>A linha fica travada para leitura ate o fim da transacao de quem pergunta: num lancamento, a formula
     * nao muda antes de ele gravar. A consulta da proposta usa a mesma leitura e roda sem transacao, e a
     * trava dura so o comando; numa transacao somente leitura o banco recusaria a trava.
     */
    Optional<CatalogFormula> formulaOf(FeedFormulaId id);
}
