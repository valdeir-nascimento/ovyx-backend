package io.github.ovyx.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verificacao automatizada da regra de dependencia entre camadas (NFR-003).
 *
 * <p>Roda na build e a reprova quando a arquitetura e violada. Analisa somente as classes de
 * producao: as classes de teste incluem violacoes deliberadas, usadas por
 * {@link ArchitectureRulesSelfCheckTest}.
 */
@DisplayName("Architecture rules")
class ArchitectureRulesTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("io.github.ovyx");

    @Test
    @DisplayName("domain does not depend on any other layer")
    void domainMustNotDependOnOtherLayers() {
        ArchitectureRules.DOMAIN_MUST_NOT_DEPEND_ON_OTHER_LAYERS.check(PRODUCTION_CLASSES);
    }

    @Test
    @DisplayName("application does not depend on infrastructure or presentation")
    void applicationMustNotDependOnOuterLayers() {
        ArchitectureRules.APPLICATION_MUST_NOT_DEPEND_ON_OUTER_LAYERS.check(PRODUCTION_CLASSES);
    }

    @Test
    @DisplayName("domain and application do not depend on any framework")
    void domainAndApplicationMustBeFrameworkFree() {
        ArchitectureRules.DOMAIN_AND_APPLICATION_MUST_BE_FRAMEWORK_FREE.check(PRODUCTION_CLASSES);
    }

    @Test
    @DisplayName("every outbound port is an interface")
    void portsMustBeInterfaces() {
        ArchitectureRules.PORTS_MUST_BE_INTERFACES.check(PRODUCTION_CLASSES);
    }

    @Test
    @DisplayName("no handler serves a command and a query at the same time")
    void noClassHandlesBothCommandAndQuery() {
        ArchitectureRules.NO_CLASS_HANDLES_BOTH_COMMAND_AND_QUERY.check(PRODUCTION_CLASSES);
    }

    @Test
    @DisplayName("infrastructure does not depend on presentation")
    void infrastructureMustNotDependOnPresentation() {
        ArchitectureRules.INFRASTRUCTURE_MUST_NOT_DEPEND_ON_PRESENTATION.check(PRODUCTION_CLASSES);
    }

    @Test
    @DisplayName("presentation does not depend on infrastructure")
    void presentationMustNotDependOnInfrastructure() {
        ArchitectureRules.PRESENTATION_MUST_NOT_DEPEND_ON_INFRASTRUCTURE.check(PRODUCTION_CLASSES);
    }

    @Test
    @DisplayName("controllers carry no API documentation")
    void controllersMustNotCarryApiDocumentation() {
        ArchitectureRules.CONTROLLERS_MUST_NOT_CARRY_API_DOCUMENTATION.check(PRODUCTION_CLASSES);
    }

    @Test
    @DisplayName("every controller implements its API documentation interface")
    void controllersMustImplementTheirApiInterface() {
        ArchitectureRules.CONTROLLERS_MUST_IMPLEMENT_THEIR_API_INTERFACE.check(PRODUCTION_CLASSES);
    }
}
