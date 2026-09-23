package io.github.ovyx.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import io.github.ovyx.architecture.sample.domain.SignalController;
import io.github.ovyx.architecture.violation.application.SwaggerInApplication;
import io.github.ovyx.architecture.violation.presentation.DocumentedController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ovyxarchviolation.presentation.AnnotatedResource;

/**
 * Prova que o portao de arquitetura existe de fato (SC-009, cenario V-17 do quickstart).
 *
 * <p>Uma suite de arquitetura que nunca reprovou nada pode estar apenas mal escrita: regra com
 * pacote errado passa silenciosamente para sempre. Este teste submete classes com violacoes
 * deliberadas as <strong>mesmas</strong> regras de {@link ArchitectureRulesTest} e exige que
 * <strong>cada uma</strong> das nove as reprove.
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

    /** Exige que a regra reprove as classes e que o relatorio aponte a violacao esperada. */
    private static void assertRejects(ArchRule rule, JavaClasses classes, String expectedViolation) {
        EvaluationResult result = rule.evaluate(classes);

        assertThat(result.hasViolation())
                .as("a regra deveria ter reprovado a violacao deliberada: %s", rule.getDescription())
                .isTrue();
        assertThat(result.getFailureReport().getDetails())
                .as("o relatorio deveria apontar %s", expectedViolation)
                .anyMatch(detail -> detail.contains(expectedViolation));
    }

    @Test
    @DisplayName("rejects domain depending on infrastructure")
    void rejectsDomainDependingOnInfrastructure() {
        assertRejects(
                ArchitectureRules.DOMAIN_MUST_NOT_DEPEND_ON_OTHER_LAYERS,
                VIOLATING_CLASSES,
                "DomainDependingOnInfrastructure");
    }

    @Test
    @DisplayName("rejects application calling an infrastructure adapter directly")
    void rejectsApplicationDependingOnInfrastructure() {
        assertRejects(
                ArchitectureRules.APPLICATION_MUST_NOT_DEPEND_ON_OUTER_LAYERS,
                VIOLATING_CLASSES,
                "ApplicationUsingInfrastructure");
    }

    @Test
    @DisplayName("rejects a framework inside the domain")
    void rejectsFrameworkInsideDomain() {
        assertRejects(ArchitectureRules.DOMAIN_AND_APPLICATION_MUST_BE_FRAMEWORK_FREE, VIOLATING_CLASSES, "SpringInDomain");
    }

    @Test
    @DisplayName("rejects OpenAPI annotations inside the application layer")
    void rejectsOpenApiAnnotationsInsideApplication() {
        // Isolada: no conjunto completo, a violacao do Spring no dominio ja reprovaria a regra.
        assertRejects(
                ArchitectureRules.DOMAIN_AND_APPLICATION_MUST_BE_FRAMEWORK_FREE,
                only(SwaggerInApplication.class),
                "io.swagger");
    }

    @Test
    @DisplayName("rejects an outbound port declared as a concrete class")
    void rejectsPortDeclaredAsConcreteClass() {
        assertRejects(ArchitectureRules.PORTS_MUST_BE_INTERFACES, VIOLATING_CLASSES, "NotAnInterface");
    }

    @Test
    @DisplayName("rejects a handler serving both a command and a query")
    void rejectsHandlerServingBothSides() {
        assertRejects(ArchitectureRules.NO_CLASS_HANDLES_BOTH_COMMAND_AND_QUERY, VIOLATING_CLASSES, "BothHandlers");
    }

    @Test
    @DisplayName("rejects infrastructure depending on presentation")
    void rejectsInfrastructureDependingOnPresentation() {
        assertRejects(
                ArchitectureRules.INFRASTRUCTURE_MUST_NOT_DEPEND_ON_PRESENTATION,
                VIOLATING_CLASSES,
                "InfrastructureUsingPresentation");
    }

    @Test
    @DisplayName("rejects presentation calling an infrastructure adapter directly")
    void rejectsPresentationDependingOnInfrastructure() {
        assertRejects(ArchitectureRules.PRESENTATION_MUST_NOT_DEPEND_ON_INFRASTRUCTURE, VIOLATING_CLASSES, "SomeController");
    }

    @Test
    @DisplayName("rejects OpenAPI annotations on a presentation class named as a controller")
    void rejectsApiDocumentationOnControllerBySuffix() {
        assertRejects(
                ArchitectureRules.CONTROLLERS_MUST_NOT_CARRY_API_DOCUMENTATION,
                only(DocumentedController.class),
                "io.swagger");
    }

    @Test
    @DisplayName("rejects springdoc annotations on a class annotated as a REST controller")
    void rejectsApiDocumentationOnControllerByStereotype() {
        assertRejects(
                ArchitectureRules.CONTROLLERS_MUST_NOT_CARRY_API_DOCUMENTATION,
                only(AnnotatedResource.class),
                "ParameterObject");
    }

    @Test
    @DisplayName("rejects a presentation class named as a controller without its API interface")
    void rejectsControllerWithoutApiInterfaceBySuffix() {
        assertRejects(
                ArchitectureRules.CONTROLLERS_MUST_IMPLEMENT_THEIR_API_INTERFACE,
                only(DocumentedController.class),
                "DocumentedApi");
    }

    @Test
    @DisplayName("rejects a REST controller without its API interface")
    void rejectsControllerWithoutApiInterfaceByStereotype() {
        assertRejects(
                ArchitectureRules.CONTROLLERS_MUST_IMPLEMENT_THEIR_API_INTERFACE,
                only(AnnotatedResource.class),
                "AnnotatedResourceApi");
    }

    @Test
    @DisplayName("does not treat a class named as a controller outside presentation as one")
    void ignoresControllerNamedClassOutsidePresentation() {
        // Sem controller no conjunto, a regra nao casa com nada; o afrouxamento vale so aqui.
        JavaClasses signal = only(SignalController.class);

        assertThat(ArchitectureRules.CONTROLLERS_MUST_IMPLEMENT_THEIR_API_INTERFACE
                        .allowEmptyShould(true)
                        .evaluate(signal)
                        .hasViolation())
                .isFalse();
    }
}
