package io.github.ovyx.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import io.github.ovyx.architecture.sample.application.CatchingHandler;
import io.github.ovyx.architecture.sample.domain.SignalController;
import io.github.ovyx.architecture.violation.application.EscapingHandler;
import io.github.ovyx.architecture.violation.application.SwaggerInApplication;
import io.github.ovyx.architecture.violation.domain.RefusingRegistration;
import io.github.ovyx.architecture.violation.presentation.DocumentedController;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ovyxarchviolation.presentation.AnnotatedResource;

/**
 * Prova que o portao de arquitetura existe de fato (SC-009, cenario V-17 do quickstart).
 *
 * <p>Uma suite de arquitetura que nunca reprovou nada pode estar apenas mal escrita: regra com
 * pacote errado passa silenciosamente para sempre. Este teste submete classes com violacoes
 * deliberadas as <strong>mesmas</strong> regras de {@link ArchitectureRulesTest} e exige que
 * <strong>cada uma</strong> das dez as reprove.
 *
 * <p>Cada caso exige que o relatorio cite a violacao esperada. Aceitar qualquer
 * {@link AssertionError} nao bastava: a regra que deixa de casar com a classe tambem falha, com
 * "failed to check any classes", e o autoteste passava sem provar nada — o que a revisao da Fase 3e
 * demonstrou por mutacao.
 */
@DisplayName("Architecture rules self-check")
class ArchitectureRulesSelfCheckTest {

    /** Classes de teste com violacoes propositais; nunca entram no artefato de producao. */
    private static final JavaClasses VIOLATING_CLASSES =
            new ClassFileImporter().importPackages("io.github.ovyx.architecture.violation");

    private static JavaClasses only(Class<?>... classes) {
        return new ClassFileImporter().importClasses(classes);
    }

    /**
     * Cada caso: a regra, as classes submetidas a ela e o trecho que o relatorio precisa citar.
     *
     * <p>Os casos isolados com {@link #only} existem porque, no conjunto completo, outra violacao ja
     * reprovaria a regra e o caso passaria sem provar a situacao que ele descreve.
     */
    private static Stream<Arguments> deliberateViolations() {
        return Stream.of(
                Arguments.of(
                        "domain depending on infrastructure",
                        ArchitectureRules.DOMAIN_MUST_NOT_DEPEND_ON_OTHER_LAYERS,
                        VIOLATING_CLASSES,
                        "DomainDependingOnInfrastructure"),
                Arguments.of(
                        "application calling an infrastructure adapter directly",
                        ArchitectureRules.APPLICATION_MUST_NOT_DEPEND_ON_OUTER_LAYERS,
                        VIOLATING_CLASSES,
                        "ApplicationUsingInfrastructure"),
                Arguments.of(
                        "a framework inside the domain",
                        ArchitectureRules.DOMAIN_AND_APPLICATION_MUST_BE_FRAMEWORK_FREE,
                        VIOLATING_CLASSES,
                        "SpringInDomain"),
                Arguments.of(
                        "OpenAPI annotations inside the application layer",
                        ArchitectureRules.DOMAIN_AND_APPLICATION_MUST_BE_FRAMEWORK_FREE,
                        only(SwaggerInApplication.class),
                        "io.swagger"),
                Arguments.of(
                        "an outbound port declared as a concrete class",
                        ArchitectureRules.PORTS_MUST_BE_INTERFACES,
                        VIOLATING_CLASSES,
                        "NotAnInterface"),
                Arguments.of(
                        "a handler serving both a command and a query",
                        ArchitectureRules.NO_CLASS_HANDLES_BOTH_COMMAND_AND_QUERY,
                        VIOLATING_CLASSES,
                        "BothHandlers"),
                Arguments.of(
                        "infrastructure depending on presentation",
                        ArchitectureRules.INFRASTRUCTURE_MUST_NOT_DEPEND_ON_PRESENTATION,
                        VIOLATING_CLASSES,
                        "InfrastructureUsingPresentation"),
                Arguments.of(
                        "presentation calling an infrastructure adapter directly",
                        ArchitectureRules.PRESENTATION_MUST_NOT_DEPEND_ON_INFRASTRUCTURE,
                        VIOLATING_CLASSES,
                        "SomeController"),
                Arguments.of(
                        "OpenAPI annotations on a presentation class named as a controller",
                        ArchitectureRules.CONTROLLERS_MUST_NOT_CARRY_API_DOCUMENTATION,
                        only(DocumentedController.class),
                        "io.swagger"),
                Arguments.of(
                        "springdoc annotations on a class annotated as a REST controller",
                        ArchitectureRules.CONTROLLERS_MUST_NOT_CARRY_API_DOCUMENTATION,
                        only(AnnotatedResource.class),
                        "ParameterObject"),
                Arguments.of(
                        "a presentation class named as a controller without its API interface",
                        ArchitectureRules.CONTROLLERS_MUST_IMPLEMENT_THEIR_API_INTERFACE,
                        only(DocumentedController.class),
                        "DocumentedApi"),
                Arguments.of(
                        "a REST controller without its API interface",
                        ArchitectureRules.CONTROLLERS_MUST_IMPLEMENT_THEIR_API_INTERFACE,
                        only(AnnotatedResource.class),
                        "AnnotatedResourceApi"),
                Arguments.of(
                        "a handler that lets a domain refusal escape",
                        ArchitectureRules.HANDLERS_MUST_CATCH_DOMAIN_REFUSALS,
                        only(EscapingHandler.class, RefusingRegistration.class),
                        "EscapingHandler"));
    }

    @ParameterizedTest(name = "rejects {0}")
    @MethodSource("deliberateViolations")
    @DisplayName("every rule rejects its deliberate violation")
    void givenDeliberateViolation_whenEvaluatingTheRule_thenRejectItNamingTheViolation(
            String situation, ArchRule rule, JavaClasses classes, String expectedViolation) {
        // given — rule, classes and expected violation from @MethodSource

        // when
        EvaluationResult result = rule.evaluate(classes);

        // then
        assertThat(result.hasViolation())
                .as("a regra deveria ter reprovado a violacao deliberada: %s", rule.getDescription())
                .isTrue();
        assertThat(result.getFailureReport().getDetails())
                .as("o relatorio deveria apontar %s", expectedViolation)
                .anyMatch(detail -> detail.contains(expectedViolation));
    }

    @Test
    @DisplayName("accepts a handler that catches the refusal it provokes")
    void givenHandlerCatchingTheRefusalItProvokes_whenEvaluatingTheRule_thenAcceptIt() {
        // given
        // Sem este caso, uma regra que reprovasse sempre passaria no teste de rejeicao acima.
        JavaClasses compliant = only(CatchingHandler.class, RefusingRegistration.class);

        // when
        EvaluationResult result = ArchitectureRules.HANDLERS_MUST_CATCH_DOMAIN_REFUSALS.evaluate(compliant);

        // then
        assertThat(result.hasViolation()).isFalse();
    }

    @Test
    @DisplayName("does not treat a class named as a controller outside presentation as one")
    void givenControllerNamedClassOutsidePresentation_whenEvaluatingTheRule_thenDoNotTreatItAsAController() {
        // given
        // Sem controller no conjunto, a regra nao casa com nada; o afrouxamento vale so aqui.
        JavaClasses signal = only(SignalController.class);

        // when
        EvaluationResult result = ArchitectureRules.CONTROLLERS_MUST_IMPLEMENT_THEIR_API_INTERFACE
                .allowEmptyShould(true)
                .evaluate(signal);

        // then
        assertThat(result.hasViolation()).isFalse();
    }
}
