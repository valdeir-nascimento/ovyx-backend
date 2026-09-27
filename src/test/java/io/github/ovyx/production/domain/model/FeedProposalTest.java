package io.github.ovyx.production.domain.model;

import static io.github.ovyx.production.domain.model.CatalogFormulas.POSTURA_PLUS;
import static io.github.ovyx.production.domain.model.CatalogFormulas.RECRIA_INACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.production.domain.valueobject.FeedEntry;
import io.github.ovyx.shared.domain.DomainException;
import io.github.ovyx.shared.domain.Violation;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A regra única da sugestão de ração (FR-009 da 004), usada pelo lançamento e pela proposta antes de gravar. */
@DisplayName("FeedProposal")
class FeedProposalTest {

    /** Uma gaiola como quem pede a proposta a conhece: o código e as aves. */
    private record Cage(String code, int birds) {}

    /** R$ 2,50 o quilo, 60 g por ave ao dia: com 1.000 aves, a proposta passa de 50.000 g. */
    private static final CatalogFormula ENGORDA = new CatalogFormula(
            FeedFormulaId.of(UUID.fromString("6c8e0a2c-4e6a-4c8e-8a0c-6e8a0c2e4a33")),
            "Engorda",
            true,
            new BigDecimal("2.50"),
            60);

    private static FeedProposal<Cage> proposalOf(FeedFormulaChoice choice, Cage... cages) {
        return FeedProposal.of(choice, List.of(cages), Cage::code, Cage::birds);
    }

    @Test
    @DisplayName("proposes the birds of each cage times the expected intake, in the order the cages came")
    void givenPendingCages_whenProposing_thenProposeTheBirdsTimesTheExpectedIntakeInOrder() {
        // given
        Cage b07 = new Cage("B-07", 50);
        Cage a01 = new Cage("A-01", 48);

        // when
        FeedProposal<Cage> proposal = proposalOf(FeedFormulaChoice.of(POSTURA_PLUS), b07, a01);

        // then
        assertThat(proposal.formula()).isEqualTo(POSTURA_PLUS);
        assertThat(proposal.entries().keySet()).containsExactly(b07, a01);
        assertThat(proposal.entries().values())
                .extracting(FeedEntry::consumption)
                .containsExactly(1400, 1344);
    }

    @Test
    @DisplayName("proposes nothing when no cage is pending, and keeps the formula")
    void givenNoPendingCage_whenProposing_thenProposeNothing() {
        // given
        FeedFormulaChoice choice = FeedFormulaChoice.of(POSTURA_PLUS);

        // when
        FeedProposal<Cage> proposal = proposalOf(choice);

        // then
        assertThat(proposal.entries()).isEmpty();
        assertThat(proposal.formula()).isEqualTo(POSTURA_PLUS);
    }

    @Test
    @DisplayName("refuses every cage whose proposal would pass 50,000 g at once, in the formula field")
    void givenTwoCagesAboveTheMaximum_whenProposing_thenRefuseBothAtOnce() {
        // given
        FeedFormulaChoice choice = FeedFormulaChoice.of(ENGORDA);

        // when
        ThrowingCallable proposing =
                () -> proposalOf(choice, new Cage("A-01", 1000), new Cage("A-02", 50), new Cage("B-07", 900));

        // then
        assertThatThrownBy(proposing)
                .isInstanceOfSatisfying(DomainException.class, refusal -> assertThat(refusal.violations())
                        .extracting(Violation::field, Violation::message)
                        .containsExactly(
                                tuple(
                                        "formulaId",
                                        "A proposta da gaiola A-01 passaria de 50.000 g. Lance a ração dela pela própria gaiola."),
                                tuple(
                                        "formulaId",
                                        "A proposta da gaiola B-07 passaria de 50.000 g. Lance a ração dela pela própria gaiola.")));
    }

    @Test
    @DisplayName("refuses an inactive formula before looking at the cages")
    void givenInactiveFormula_whenProposing_thenRefuseOnlyTheFormula() {
        // given
        FeedFormulaChoice choice = FeedFormulaChoice.of(RECRIA_INACTIVE);

        // when
        ThrowingCallable proposing = () -> proposalOf(choice, new Cage("A-01", 5000));

        // then
        assertThatThrownBy(proposing)
                .isInstanceOfSatisfying(DomainException.class, refusal -> assertThat(refusal.violations())
                        .extracting(Violation::code)
                        .containsExactly(ProductionErrorCode.FORMULA_INACTIVE));
    }

    @Test
    @DisplayName("refuses a missing formula before looking at the cages")
    void givenNoFormula_whenProposing_thenRefuseAsFormulaRequired() {
        // given
        FeedFormulaChoice choice = FeedFormulaChoice.absent();

        // when
        ThrowingCallable proposing = () -> proposalOf(choice, new Cage("A-01", 50));

        // then
        assertThatThrownBy(proposing)
                .isInstanceOfSatisfying(DomainException.class, refusal -> assertThat(refusal.violations())
                        .extracting(Violation::code)
                        .containsExactly(ProductionErrorCode.FORMULA_REQUIRED));
    }
}
