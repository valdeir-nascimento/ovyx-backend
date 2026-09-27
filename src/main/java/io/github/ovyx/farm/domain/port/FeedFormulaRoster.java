package io.github.ovyx.farm.domain.port;

import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.farm.domain.valueobject.FeedFormulaName;

/**
 * O que uma formula consulta sobre as demais para guardar o nome unico (R-011 da 004).
 *
 * <p>Somente leitura, de proposito, como o {@link SectorRoster}: o agregado decide e recusa, e esta
 * porta so responde a pergunta dele. O {@link FeedFormulaRepository} a estende.
 */
public interface FeedFormulaRoster {

    /**
     * Se outra formula, que nao {@code exceptId}, ja usa o nome, comparado sem maiusculas. Ativas e
     * inativas contam: a formula da nome aos custos do passado.
     */
    boolean nameInUse(FeedFormulaName name, FeedFormulaId exceptId);
}
