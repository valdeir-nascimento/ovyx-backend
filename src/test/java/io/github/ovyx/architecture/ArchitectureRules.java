package io.github.ovyx.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.domain.DomainException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * As regras de arquitetura do Ovyx, em um unico lugar.
 *
 * <p>Ficam aqui, e nao dentro de um teste, para que {@code ArchitectureRulesTest} e
 * {@code ArchitectureRulesSelfCheckTest} avaliem exatamente as mesmas regras. Se o autoteste usasse
 * uma copia, ele provaria que uma regra qualquer funciona — nao que <em>esta</em> suite funciona.
 *
 * <p>Nenhuma regra usa {@code allowEmptyShould}. Os pacotes ja existem, e com o afrouxamento um
 * renomeio de pacote desligaria a regra em silencio: ela passaria a nao casar com nada e a aprovar
 * tudo.
 */
public final class ArchitectureRules {

    private ArchitectureRules() {}

    /** Pacotes de framework que nao podem aparecer em {@code domain} nem em {@code application}. */
    private static final String[] FRAMEWORK_PACKAGES = {
        "org.springframework..",
        "jakarta.persistence..",
        "jakarta.validation..",
        "jakarta.servlet..",
        "com.fasterxml.jackson..",
        "tools.jackson..",
        "org.hibernate..",
        "org.flywaydb..",
        // Anotacoes do OpenAPI descrevem o contrato HTTP: pertencem a apresentacao (A13).
        "io.swagger..",
    };

    /** Principio I: a regra de dependencia aponta para dentro; o dominio nao conhece nenhuma outra camada. */
    public static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_OTHER_LAYERS = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..", "..presentation..")
            .because("o dominio nao pode conhecer nenhuma outra camada (principio I da constituicao)");

    /** Principio I: a aplicacao so olha para dentro. */
    public static final ArchRule APPLICATION_MUST_NOT_DEPEND_ON_OUTER_LAYERS = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..", "..presentation..")
            .because("a aplicacao nao pode depender de infraestrutura nem de apresentacao (principio I)");

    /** Principio I e restricoes tecnologicas: dominio e aplicacao sao livres de framework. */
    public static final ArchRule DOMAIN_AND_APPLICATION_MUST_BE_FRAMEWORK_FREE = noClasses()
            .that()
            .resideInAnyPackage("..domain..", "..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(FRAMEWORK_PACKAGES)
            .because("dominio e aplicacao nao podem depender de framework (principio I e restricoes tecnologicas)");

    /** Principio II: toda porta de saida do dominio e uma interface. */
    public static final ArchRule PORTS_MUST_BE_INTERFACES = classes()
            .that()
            .resideInAPackage("..domain.port..")
            .should()
            .beInterfaces()
            .because("porta de saida e contrato, nao implementacao (principio II)");

    /** Principio V: um mesmo tratador nunca serve comando e consulta. */
    public static final ArchRule NO_CLASS_HANDLES_BOTH_COMMAND_AND_QUERY = noClasses()
            .that()
            .implement(CommandHandler.class)
            .should()
            .implement(QueryHandler.class)
            .because("escrita e leitura sao separadas: um tratador nunca serve os dois lados (principio V)");

    /** Principio I: infraestrutura nao conhece a camada de entrega. */
    public static final ArchRule INFRASTRUCTURE_MUST_NOT_DEPEND_ON_PRESENTATION = noClasses()
            .that()
            .resideInAPackage("..infrastructure..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..presentation..")
            .because("infraestrutura e apresentacao sao camadas irmas: nenhuma depende da outra (principio I)");

    /**
     * Principio I: a camada de entrega fala com os casos de uso, nunca direto com adaptadores.
     *
     * <p>Um controller que chamasse um adaptador JPA pularia a camada de aplicacao — e com ela o
     * {@code Result}, a recusa do dominio e as invariantes do agregado.
     */
    public static final ArchRule PRESENTATION_MUST_NOT_DEPEND_ON_INFRASTRUCTURE = noClasses()
            .that()
            .resideInAPackage("..presentation..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .because("a apresentacao usa os casos de uso, nunca os adaptadores diretamente (principio I)");

    /**
     * Estereotipo de controller do Spring. Referido pelo nome para nao amarrar a suite ao Spring.
     *
     * <p>Como meta-anotacao, casa com {@code @Controller}, {@code @RestController} e qualquer anotacao
     * composta sobre elas — mas nao com {@code @RestControllerAdvice}, que nao e controller.
     */
    private static final String CONTROLLER_STEREOTYPE = "org.springframework.stereotype.Controller";

    /**
     * Quem e controller para as regras de documentacao: a classe com o estereotipo, ou a de
     * {@code presentation} com sufixo {@code Controller}.
     *
     * <p>O sufixo existe para que a violacao deliberada do autoteste nao precise ser um controller de
     * verdade, o que a poria no contexto de todo teste de integracao. Fica restrito a
     * {@code presentation} porque, fora dela, {@code Controller} e so uma palavra — um
     * {@code TrafficController} de dominio nao tem rota nem documentacao.
     */
    private static final DescribedPredicate<JavaClass> CONTROLLERS = JavaClass.Predicates.simpleNameEndingWith(
                    "Controller")
            .and(JavaClass.Predicates.resideInAPackage("..presentation.."))
            .or(CanBeAnnotated.Predicates.metaAnnotatedWith(CONTROLLER_STEREOTYPE))
            .as("controllers");

    /**
     * Restricoes tecnologicas: a documentacao da API fica na interface {@code <Recurso>Api}, e o
     * controller fica so com as rotas e a traducao do {@code Result}.
     *
     * <p>Barra tambem {@code org.springdoc}: anotacoes como {@code @ParameterObject} so servem ao
     * documento e pertencem a interface, como as do {@code io.swagger}.
     */
    public static final ArchRule CONTROLLERS_MUST_NOT_CARRY_API_DOCUMENTATION = noClasses()
            .that(CONTROLLERS)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("io.swagger..", "org.springdoc..")
            .because("a documentacao da API fica na interface <Recurso>Api, nao no controller (restricoes tecnologicas)");

    /** Restricoes tecnologicas: todo {@code XController} implementa a interface {@code XApi} do mesmo pacote. */
    public static final ArchRule CONTROLLERS_MUST_IMPLEMENT_THEIR_API_INTERFACE = classes()
            .that(CONTROLLERS)
            .should(implementTheirApiInterface())
            .because("a documentacao de cada controller vive na interface <Recurso>Api (restricoes tecnologicas)");

    private static ArchCondition<JavaClass> implementTheirApiInterface() {
        return new ArchCondition<>("implement the <Resource>Api interface of the same package") {
            @Override
            public void check(JavaClass controller, ConditionEvents events) {
                String expected = controller.getPackageName() + "."
                        + controller.getSimpleName().replaceFirst("Controller$", "") + "Api";
                boolean implemented = controller.getRawInterfaces().stream()
                        .anyMatch(api -> api.getName().equals(expected) && api.isInterface());
                if (!implemented) {
                    events.add(SimpleConditionEvent.violated(
                            controller, controller.getName() + " nao implementa " + expected));
                }
            }
        };
    }

    /**
     * Principio IV: o tratador captura a recusa do dominio e a traduz em {@code Failure}.
     *
     * <p>A {@code DomainException} nao e declarada, entao o compilador nao cobra o {@code catch}. Sem
     * esta regra, um {@code catch} esquecido deixa a excecao chegar ao tratador global, que ainda
     * responde um 400 plausivel — a fatia parece funcionar, o {@code Result} deixa de ser o canal de
     * falha, e nenhum teste percebe.
     *
     * <p>A verificacao e sobre a <em>chamada</em>, nao sobre a classe: exige que a chamada que pode
     * provocar a recusa esteja dentro de um {@code try} que a captura. Um {@code catch} em outro
     * metodo, ou em outro trecho do mesmo metodo, nao conta.
     */
    public static final ArchRule HANDLERS_MUST_CATCH_DOMAIN_REFUSALS = classes()
            .that()
            .implement(CommandHandler.class)
            .or()
            .implement(QueryHandler.class)
            .should(catchTheDomainRefusalsTheyProvoke())
            .because("o caso de uso traduz a recusa do dominio em Failure; deixa-la escapar reprova (principio IV)");

    private static ArchCondition<JavaClass> catchTheDomainRefusalsTheyProvoke() {
        return new ArchCondition<>("catch the domain refusals they provoke") {
            @Override
            public void check(JavaClass handler, ConditionEvents events) {
                for (JavaCodeUnit codeUnit : handler.getCodeUnits()) {
                    for (JavaMethodCall call : codeUnit.getMethodCallsFromSelf()) {
                        // So a chamada que cruza para o dominio interessa. Sem este filtro, o metodo
                        // ponte que o compilador gera para a interface generica — handle(Command),
                        // que so delega ao handle tipado — aparecia como violacao, embora o metodo
                        // real capture. Chamada interna da propria classe e avaliada no ponto em que
                        // ela mesma chama o dominio.
                        if (!isDomain(call.getTargetOwner())) {
                            continue;
                        }
                        if (provokesRefusal(call) && !isCaughtAtTheCall(call)) {
                            events.add(SimpleConditionEvent.violated(
                                    handler,
                                    codeUnit.getFullName() + " chama " + call.getTarget().getFullName()
                                            + ", que recusa por DomainException, sem capturar a recusa"));
                        }
                    }
                }
            }
        };
    }

    /** Classes do dominio, de qualquer contexto delimitado. */
    private static boolean isDomain(JavaClass javaClass) {
        return javaClass.getPackageName().contains(".domain");
    }

    /** Verdadeiro quando o alvo da chamada pode recusar, direta ou indiretamente, dentro do dominio. */
    private static boolean provokesRefusal(JavaMethodCall call) {
        return call.getTarget()
                .resolveMember()
                .map(target -> refuses(target, new HashSet<>()))
                .orElse(false);
    }

    /**
     * Verdadeiro quando o metodo constroi uma {@code DomainException}, ou chama alguem do dominio que
     * a constroi.
     *
     * <p>Olha a construcao da excecao, e nao o nome de quem acumula: assim a regra sobrevive a
     * renomeacao do acumulador, e vale para qualquer recusa futura escrita a mao.
     */
    private static boolean refuses(JavaCodeUnit codeUnit, Set<String> visited) {
        if (!visited.add(codeUnit.getFullName())) {
            return false;
        }
        boolean buildsRefusal = codeUnit.getConstructorCallsFromSelf().stream()
                .anyMatch(construction -> construction.getTargetOwner().isAssignableTo(DomainException.class));
        if (buildsRefusal) {
            return true;
        }
        return codeUnit.getMethodCallsFromSelf().stream()
                .filter(call -> isDomain(call.getTargetOwner()))
                .map(call -> call.getTarget().resolveMember())
                .flatMap(Optional::stream)
                .anyMatch(target -> refuses(target, visited));
    }

    /** Verdadeiro quando a propria chamada esta dentro de um {@code try} que captura a recusa. */
    private static boolean isCaughtAtTheCall(JavaMethodCall call) {
        return call.getOwner().getTryCatchBlocks().stream()
                .filter(block -> block.getAccessesContainedInTryBlock().contains(call))
                .anyMatch(block -> block.getCaughtThrowables().stream()
                        .anyMatch(caught -> caught.isAssignableFrom(DomainException.class)));
    }
}
