package io.github.ovyx.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verificacao automatizada da regra de dependencia entre camadas (NFR-003).
 *
 * <p>Roda na build e a reprova quando a arquitetura e violada. Analisa somente as classes de
 * producao: as classes de teste incluem violacoes deliberadas, usadas por
 * {@link ArchitectureRulesSelfCheckTest}.
 *
 * <p>Uma regra nova entra com uma linha em {@link #rules()}, e o autoteste precisa ganhar o caso que
 * prova que ela reprova.
 */
@DisplayName("Architecture rules")
class ArchitectureRulesTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("io.github.ovyx");

    private static Stream<Arguments> rules() {
        return Stream.of(
                Arguments.of(
                        "domain does not depend on any other layer",
                        ArchitectureRules.DOMAIN_MUST_NOT_DEPEND_ON_OTHER_LAYERS),
                Arguments.of(
                        "application does not depend on infrastructure or presentation",
                        ArchitectureRules.APPLICATION_MUST_NOT_DEPEND_ON_OUTER_LAYERS),
                Arguments.of(
                        "domain and application do not depend on any framework",
                        ArchitectureRules.DOMAIN_AND_APPLICATION_MUST_BE_FRAMEWORK_FREE),
                Arguments.of("every outbound port is an interface", ArchitectureRules.PORTS_MUST_BE_INTERFACES),
                Arguments.of(
                        "no handler serves a command and a query at the same time",
                        ArchitectureRules.NO_CLASS_HANDLES_BOTH_COMMAND_AND_QUERY),
                Arguments.of(
                        "infrastructure does not depend on presentation",
                        ArchitectureRules.INFRASTRUCTURE_MUST_NOT_DEPEND_ON_PRESENTATION),
                Arguments.of(
                        "presentation does not depend on infrastructure",
                        ArchitectureRules.PRESENTATION_MUST_NOT_DEPEND_ON_INFRASTRUCTURE),
                Arguments.of(
                        "no bounded context depends on another one",
                        ArchitectureRules.BOUNDED_CONTEXTS_MUST_BE_INDEPENDENT),
                Arguments.of(
                        "the shared kernel depends on no bounded context",
                        ArchitectureRules.SHARED_KERNEL_MUST_NOT_DEPEND_ON_BOUNDED_CONTEXTS),
                Arguments.of(
                        "controllers carry no API documentation",
                        ArchitectureRules.CONTROLLERS_MUST_NOT_CARRY_API_DOCUMENTATION),
                Arguments.of(
                        "every controller implements its API documentation interface",
                        ArchitectureRules.CONTROLLERS_MUST_IMPLEMENT_THEIR_API_INTERFACE),
                Arguments.of(
                        "no handler lets a domain refusal escape",
                        ArchitectureRules.HANDLERS_MUST_CATCH_DOMAIN_REFUSALS),
                Arguments.of(
                        "outside domain and application, a domain refusal arrives only as a Failure",
                        ArchitectureRules.OUTER_LAYERS_MUST_NOT_RECEIVE_DOMAIN_REFUSALS));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rules")
    @DisplayName("production code follows every architecture rule")
    void givenProductionClasses_whenEvaluatingAnArchitectureRule_thenFindNoViolation(
            String description, ArchRule rule) {
        // given — PRODUCTION_CLASSES, importadas uma vez para a classe inteira

        // when
        EvaluationResult result = rule.evaluate(PRODUCTION_CLASSES);

        // then
        // Os detalhes nomeiam cada classe violadora; uma regra que nao casa com nada tambem aparece
        // aqui, como "failed to check any classes", e reprova do mesmo jeito que o check() reprovava.
        assertThat(result.getFailureReport().getDetails()).as(description).isEmpty();
    }
}
