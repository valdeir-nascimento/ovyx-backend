package io.github.ovyx.production.domain.model;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.production.domain.valueobject.FeedEntry;
import io.github.ovyx.shared.domain.DomainException;
import io.github.ovyx.shared.domain.Notification;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * A proposta da racao do setor pela sugestao (FR-009 da 004): a formula escolhida e, para cada gaiola ainda
 * sem racao, as aves dela vezes o consumo esperado.
 *
 * <p>E a regra unica da sugestao, usada pelo lancamento no agregado e pela proposta que a tela mostra antes
 * de gravar: primeiro a formula, que precisa existir e estar ativa; depois as propostas, que nao podem passar
 * do consumo maximo. Assim a tela nunca propoe o que o lancamento recusaria.
 *
 * <p>So a regra a constroi ({@link #of}): nao ha proposta de formula inativa nem acima do maximo.
 *
 * @param <C> a gaiola, como quem pede a proposta a conhece
 */
public final class FeedProposal<C> {

    private final CatalogFormula formula;
    private final Map<C, FeedEntry> entries;

    private FeedProposal(CatalogFormula formula, Map<C, FeedEntry> entries) {
        this.formula = formula;
        this.entries = Collections.unmodifiableMap(entries);
    }

    /**
     * A proposta da formula escolhida para as gaiolas pendentes dadas.
     *
     * @param codeOf o codigo da gaiola, como as telas o escrevem, para a recusa dizer qual passou do maximo
     * @param birdsOf as aves da gaiola
     * @throws DomainException quando a formula falta, nao existe ou esta inativa, ou quando a proposta de alguma
     *     gaiola passaria do consumo maximo; todas as gaiolas que passam, de uma vez
     */
    public static <C> FeedProposal<C> of(
            FeedFormulaChoice choice, List<C> pendingCages, Function<C, String> codeOf, ToIntFunction<C> birdsOf) {
        Notification notification = new Notification();
        Optional<CatalogFormula> found = choice.validate(notification);
        found.ifPresent(formula -> formula.validateActive(notification));
        notification.throwIfAny(ProductionErrorCode.VALIDATION_FAILED);
        CatalogFormula formula = found.orElseThrow();

        Map<C, FeedEntry> entries = new LinkedHashMap<>();
        for (C cage : pendingCages) {
            FeedEntry proposal = FeedEntry.suggestedFor(formula, birdsOf.applyAsInt(cage));
            FeedEntry.validateSuggestion(proposal, codeOf.apply(cage), notification);
            entries.put(cage, proposal);
        }
        notification.throwIfAny(ProductionErrorCode.VALIDATION_FAILED);
        return new FeedProposal<>(formula, entries);
    }

    /** A formula escolhida, ativa. */
    public CatalogFormula formula() {
        return formula;
    }

    /** A proposta de cada gaiola pendente, na ordem em que vieram. */
    public Map<C, FeedEntry> entries() {
        return entries;
    }
}
