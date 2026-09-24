package io.github.ovyx.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import io.github.ovyx.architecture.sample.domain.SignalController;
import io.github.ovyx.architecture.violation.application.SwaggerInApplication;
import io.github.ovyx.architecture.violation.presentation.DocumentedController;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
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
 * <strong>cada uma</strong> das onze as reprove.
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

    private static final String SAMPLE_DOMAIN = "io.github.ovyx.architecture.sample.domain";
    private static final String SAMPLE_APPLICATION = "io.github.ovyx.architecture.sample.application";
    private static final String ESCAPING_HANDLERS_PACKAGE = "io.github.ovyx.architecture.violation.application";

    /**
     * O nucleo compartilhado entra junto porque toda recusa real sai de {@code Notification.throwIfAny},
     * dois niveis abaixo de quem o tratador chama. Sem ele no conjunto, a regra nao alcanca a
     * construcao da excecao e o autoteste deixa de provar o caminho que a producao percorre.
     */
    private static final String SHARED_KERNEL = "io.github.ovyx.shared.domain";

    /** O {@code Result} entra para que o {@code map}, que chama {@code Function.apply}, seja avaliado de verdade. */
    private static final String SHARED_APPLICATION = "io.github.ovyx.shared.application";

    /** Tratadores que deixam a recusa escapar, um por caminho, com o dominio que eles chamam. */
    private static final JavaClasses ESCAPING_HANDLERS = new ClassFileImporter()
            .importPackages(
                    ESCAPING_HANDLERS_PACKAGE,
                    SAMPLE_APPLICATION,
                    SAMPLE_DOMAIN,
                    SHARED_KERNEL,
                    SHARED_APPLICATION);

    /** O tratador correto, com o colaborador e o dominio que ele chama. */
    private static final JavaClasses CATCHING_HANDLER =
            new ClassFileImporter().importPackages(SAMPLE_APPLICATION, SAMPLE_DOMAIN, SHARED_KERNEL, SHARED_APPLICATION);

    /** Classes fora de dominio e aplicacao que pulam o caso de uso, com o que elas chamam. */
    private static final JavaClasses LEAKING_BORDERS = new ClassFileImporter()
            .importPackages(
                    "io.github.ovyx.architecture.violation.presentation",
                    "io.github.ovyx.architecture.violation.infrastructure",
                    "io.github.ovyx.architecture.violation.web",
                    SAMPLE_APPLICATION,
                    SAMPLE_DOMAIN,
                    SHARED_KERNEL,
                    SHARED_APPLICATION);

    /** Entrada correta, que fala com o caso de uso. */
    private static final JavaClasses COMPLIANT_ENDPOINT = new ClassFileImporter()
            .importPackages(
                    "io.github.ovyx.architecture.sample.presentation",
                    SAMPLE_APPLICATION,
                    SAMPLE_DOMAIN,
                    SHARED_KERNEL,
                    SHARED_APPLICATION);

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
                refusalEscaping("from a direct call to the domain", "ByDirectCall"),
                refusalEscaping("from a method reference to the domain", "ByMethodReference"),
                refusalEscaping("from a domain constructor", "ByConstruction"),
                refusalEscaping("from a reference to a domain constructor", "ByConstructorReference"),
                refusalEscaping("through a domain interface", "ThroughDomainInterface"),
                refusalEscaping("through a method reference inside the domain", "ThroughDomainMethodReference"),
                refusalEscaping("through an application collaborator", "ThroughCollaborator"),
                refusalEscaping("through a private method of the handler", "ThroughPrivateHelper"),
                refusalEscaping("from a reference to the refusal constructor", "ByRefusalConstructorReference"),
                refusalEscaping("from a static factory of the refusal", "ByRefusalFactory"),
                refusalEscaping("through an anonymous class behind an outside interface", "ThroughAnonymousClass"),
                refusalEscaping("through a call cycle", "ThroughCycle"),
                refusalEscaping("inside a try that catches another exception type", "WrongCatchType"),
                refusalEscaping(
                        "through an outside interface it builds by constructor reference",
                        "ThroughAnOutsideInterfaceBuiltByReference"),
                refusalReachingTheBorder(
                        "presentation calling an application collaborator that lets the refusal escape",
                        "RefusalLeakingEndpoint.registerThroughTheCollaborator"),
                refusalReachingTheBorder(
                        "presentation calling the refusing domain directly",
                        "RefusalLeakingEndpoint.registerThroughTheDomain"),
                refusalReachingTheBorder(
                        "presentation catching the refusal itself", "RefusalLeakingEndpoint.registerCatchingTheRefusal"),
                refusalReachingTheBorder(
                        "infrastructure calling a refusing domain factory", "RefusalLeakingAdapter.restore"),
                refusalReachingTheBorder(
                        "a class outside the four layers calling a collaborator that lets the refusal escape",
                        "RefusalLeakingFilter.filter"));
    }

    private static Arguments refusalReachingTheBorder(String situation, String codeUnit) {
        return Arguments.of(
                situation, ArchitectureRules.OUTER_LAYERS_MUST_NOT_RECEIVE_DOMAIN_REFUSALS, LEAKING_BORDERS, codeUnit);
    }

    private static Arguments refusalEscaping(String path, String handler) {
        return Arguments.of(
                "a handler that lets a domain refusal escape " + path,
                ArchitectureRules.HANDLERS_MUST_CATCH_DOMAIN_REFUSALS,
                ESCAPING_HANDLERS,
                "EscapingHandlers$" + handler + ".");
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
    @DisplayName("accepts a handler that catches, on every path, the refusal it provokes")
    void givenHandlerCatchingTheRefusalOnEveryPath_whenEvaluatingTheRule_thenAcceptIt() {
        // given
        // Sem este caso, uma regra que reprovasse sempre passaria nos testes de rejeicao acima. O
        // tratador percorre os mesmos caminhos dos que deixam escapar, inclusive um metodo privado
        // sem catch chamado dentro do try.
        JavaClasses compliant = CATCHING_HANDLER;

        // when
        EvaluationResult result = ArchitectureRules.HANDLERS_MUST_CATCH_DOMAIN_REFUSALS.evaluate(compliant);

        // then
        assertThat(result.getFailureReport().getDetails()).isEmpty();
    }

    @RepeatedTest(value = 30, name = "fresh import {currentRepetition} of {totalRepetitions}")
    @DisplayName("rejects the escape through a call cycle whatever the order of evaluation")
    void givenHandlerEscapingThroughACallCycle_whenEvaluatingAFreshImport_thenRejectIt() {
        // given
        // A ordem em que o ArchUnit entrega os acessos muda a cada importacao. Com a resposta de um
        // trecho gravada no meio do ciclo, a violacao escapava em cerca de um quarto das importacoes;
        // um unico caso passaria por sorte.
        JavaClasses freshImport = new ClassFileImporter()
                .importPackages(ESCAPING_HANDLERS_PACKAGE, SAMPLE_APPLICATION, SAMPLE_DOMAIN, SHARED_KERNEL);

        // when
        EvaluationResult result = ArchitectureRules.HANDLERS_MUST_CATCH_DOMAIN_REFUSALS.evaluate(freshImport);

        // then
        assertThat(result.getFailureReport().getDetails())
                .anyMatch(detail -> detail.contains("EscapingHandlers$ThroughCycle."));
    }

    @Test
    @DisplayName("accepts an endpoint that reaches the refusing domain only through the use case")
    void givenEndpointCallingTheUseCase_whenEvaluatingTheOuterLayersRule_thenAcceptIt() {
        // given
        // O caso de uso chama o mesmo dominio que recusa; a regra precisa ver que ele captura.
        JavaClasses compliant = COMPLIANT_ENDPOINT;

        // when
        EvaluationResult result =
                ArchitectureRules.OUTER_LAYERS_MUST_NOT_RECEIVE_DOMAIN_REFUSALS.evaluate(compliant);

        // then
        assertThat(result.getFailureReport().getDetails()).isEmpty();
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
