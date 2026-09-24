package io.github.ovyx.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaCodeUnitAccess;
import com.tngtech.archunit.core.domain.JavaCodeUnitReference;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.domain.DomainException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * responde um 409 "Operacao recusada" — a fatia parece funcionar, o {@code Result} deixa de ser o
     * canal de falha, e so um teste que confira o status exato percebe.
     *
     * <p>A verificacao e sobre o <em>ponto</em> de cada chamada, nao sobre a classe: o que pode
     * provocar a recusa precisa estar dentro de um {@code try} que a captura. Um {@code catch} em outro
     * metodo, ou em outro trecho do mesmo metodo, nao conta.
     *
     * <p>Pode provocar a recusa o que, seguido por dominio e aplicacao, chega a construcao de uma
     * {@code DomainException} sem passar por um {@code try} que a capture. O caminho segue chamadas e
     * referencias, de metodo e de construtor, inclusive as fabricas da propria recusa; as sobrescritas
     * e implementacoes importadas, e as de interfaces de fora do projeto, como {@code Supplier},
     * quando instanciadas pela propria classe de quem chama; e os colaboradores de {@code application},
     * inclusive os metodos privados do proprio tratador —
     * que por isso podem deixar a recusa subir, desde que o tratador a capture onde os chama.
     * Adaptadores ficam fora: o que eles lancam nao e recusa do dominio.
     *
     * <p>Limites conhecidos, todos da leitura do bytecode pelo ArchUnit:
     *
     * <ul>
     *   <li>um {@code catch} que relanca a recusa conta como captura, porque o conteudo do bloco
     *       {@code catch} nao e visivel;
     *   <li>a chamada feita dentro de uma lambda nao conta como dentro do {@code try} que envolve a
     *       lambda, porque o acesso e atribuido ao metodo, nao ao bloco. A regra reprova, e o
     *       {@code catch} precisa ficar dentro da propria lambda;
     *   <li>a implementacao de uma interface de fora do projeto — uma {@code Function} ou um
     *       {@code Predicate} do dominio — so e seguida quando a propria classe de quem chama a
     *       instancia. Injetada pelo construtor, devolvida por uma fabrica, ou entregue a quem a
     *       executa ({@code result.map(new Regra())}, {@code stream.map(regra)}), ela nao e seguida.
     *       Seguir todas fazia de cada {@code Result.map} uma recusa. Lambdas e referencias de metodo
     *       sao seguidas;
     *   <li>so o metodo privado e avaliado no ponto em que o tratador o chama. Um auxiliar protegido
     *       ou de pacote sem {@code catch} e reprovado mesmo chamado dentro do {@code try}, porque
     *       pode ser chamado de fora;
     *   <li>a chamada por uma interface ou classe-base do projeto conta como recusa se
     *       <em>qualquer</em> implementacao importada recusar;
     *   <li>uma recusa construida antes e guardada — num campo estatico, lancada com
     *       {@code throw RECUSA} — nao e seguida: ler um campo nao e chamada, e o caminho parte da
     *       construcao.
     * </ul>
     */
    public static final ArchRule HANDLERS_MUST_CATCH_DOMAIN_REFUSALS = classes()
            .that()
            .implement(CommandHandler.class)
            .or()
            .implement(QueryHandler.class)
            .should(catchTheDomainRefusalsTheyProvoke())
            .because("o caso de uso traduz a recusa do dominio em Failure; deixa-la escapar reprova (principio IV)");

    /**
     * Principio IV: fora de dominio e aplicacao, a recusa do dominio so chega como {@code Failure}, pelo
     * caso de uso.
     *
     * <p>E a premissa de {@link #HANDLERS_MUST_CATCH_DOMAIN_REFUSALS}: um colaborador de
     * {@code application} pode deixar a recusa subir porque so o tratador o chama. Sem esta regra, um
     * controller que chamasse o colaborador — ou o dominio — direto receberia a recusa sem ninguem a
     * traduzir, e ela sairia como 409 pelo tratador global. Um {@code catch} na borda nao conserta:
     * traduzir a recusa e papel do caso de uso. Vale para tudo o que nao e dominio nem aplicacao, e
     * nao so para apresentacao e infraestrutura: um pacote novo fora das quatro camadas nao vira
     * brecha.
     */
    public static final ArchRule OUTER_LAYERS_MUST_NOT_RECEIVE_DOMAIN_REFUSALS = classes()
            .that()
            .resideOutsideOfPackages("..domain..", "..application..")
            .should(notInvokeWhatLetsADomainRefusalEscape())
            .because("a recusa do dominio chega a borda so como Failure do caso de uso (principio IV)");

    private static ArchCondition<JavaClass> catchTheDomainRefusalsTheyProvoke() {
        return new ArchCondition<>("catch the domain refusals they provoke") {
            @Override
            public void check(JavaClass handler, ConditionEvents events) {
                Set<JavaCodeUnit> escaping = refusalsEscapingFrom(handler.getCodeUnits());
                handler.getCodeUnits().stream()
                        .filter(ArchitectureRules::isEntryPoint)
                        .forEach(entryPoint -> invocationsFrom(entryPoint)
                                .filter(access -> provokesRefusal(access, escaping) && !isCaughtAtTheSite(access))
                                .forEach(access -> events.add(SimpleConditionEvent.violated(
                                        handler,
                                        entryPoint.getFullName() + " " + verbOf(access) + " "
                                                + access.getTarget().getFullName()
                                                + ", que pode recusar por DomainException, sem capturar a recusa"))));
            }
        };
    }

    private static ArchCondition<JavaClass> notInvokeWhatLetsADomainRefusalEscape() {
        return new ArchCondition<>("not invoke what lets a domain refusal escape") {
            @Override
            public void check(JavaClass outer, ConditionEvents events) {
                Set<JavaCodeUnit> escaping = refusalsEscapingFrom(outer.getCodeUnits());
                outer.getCodeUnits()
                        .forEach(codeUnit -> invocationsFrom(codeUnit)
                                .filter(access -> provokesRefusal(access, escaping))
                                .forEach(access -> events.add(SimpleConditionEvent.violated(
                                        outer,
                                        codeUnit.getFullName() + " " + verbOf(access) + " "
                                                + access.getTarget().getFullName()
                                                + ", que pode recusar por DomainException; a recusa so chega a"
                                                + " borda como Failure do caso de uso"))));
            }
        };
    }

    /**
     * Por onde o tratador e chamado de fora.
     *
     * <p>O metodo privado e avaliado no ponto em que o tratador o chama: um auxiliar sem {@code catch},
     * chamado dentro de um {@code try}, esta correto. O metodo ponte que o compilador gera para a
     * interface generica — {@code handle(Command)}, que so delega ao {@code handle} tipado — repetiria
     * a violacao do metodo real.
     */
    private static boolean isEntryPoint(JavaCodeUnit codeUnit) {
        Set<JavaModifier> modifiers = codeUnit.getModifiers();
        return !modifiers.contains(JavaModifier.PRIVATE)
                && !modifiers.contains(JavaModifier.BRIDGE)
                && !modifiers.contains(JavaModifier.SYNTHETIC);
    }

    /**
     * Os trechos de dominio e aplicacao, alcancaveis a partir das raizes, que deixam sair uma recusa
     * que eles mesmos nao capturaram.
     *
     * <p>Calculado por ponto fixo: o conjunto comeca vazio e cresce ate estabilizar. Assim o resultado
     * nao depende da ordem em que o ArchUnit entrega os acessos. Com uma memoria gravada no meio de um
     * ciclo, dependia — e a violacao escapava em parte das importacoes.
     */
    private static Set<JavaCodeUnit> refusalsEscapingFrom(Collection<JavaCodeUnit> roots) {
        Set<JavaCodeUnit> reachable = new HashSet<>();
        Deque<JavaCodeUnit> pending = new ArrayDeque<>(roots);
        while (!pending.isEmpty()) {
            JavaCodeUnit codeUnit = pending.pop();
            if (reachable.add(codeUnit)) {
                invocationsFrom(codeUnit).flatMap(ArchitectureRules::candidatesOf).forEach(pending::push);
            }
        }
        Set<JavaCodeUnit> escaping = reachable.stream()
                .filter(ArchitectureRules::isRefusalConstructor)
                .collect(Collectors.toCollection(HashSet::new));
        boolean grew;
        do {
            List<JavaCodeUnit> found = reachable.stream()
                    .filter(codeUnit -> !escaping.contains(codeUnit))
                    .filter(codeUnit -> invocationsFrom(codeUnit)
                            .anyMatch(access -> provokesRefusal(access, escaping) && !isCaughtAtTheSite(access)))
                    .toList();
            grew = escaping.addAll(found);
        } while (grew);
        return escaping;
    }

    /** Chamadas e referencias, de metodo e de construtor, feitas pelo trecho de codigo. */
    private static Stream<JavaCodeUnitAccess<?>> invocationsFrom(JavaCodeUnit codeUnit) {
        return Stream.<JavaCodeUnitAccess<?>>concat(
                codeUnit.getCallsFromSelf().stream(), codeUnit.getCodeUnitReferencesFromSelf().stream());
    }

    private static String verbOf(JavaCodeUnitAccess<?> access) {
        return access instanceof JavaCodeUnitReference<?> ? "referencia" : "chama";
    }

    /** Verdadeiro quando o acesso alcanca um trecho que deixa uma recusa do dominio sair. */
    private static boolean provokesRefusal(JavaCodeUnitAccess<?> access, Set<JavaCodeUnit> escaping) {
        return candidatesOf(access).anyMatch(escaping::contains);
    }

    /**
     * Onde a recusa nasce: um construtor da {@code DomainException} ou de uma subclasse dela.
     *
     * <p>Olha a construcao da excecao, e nao o nome de quem acumula: assim a regra sobrevive a
     * renomeacao do acumulador, e vale para qualquer recusa futura escrita a mao. Chamar o construtor,
     * referencia-lo ({@code orElseThrow(Recusa::new)}) ou chamar uma fabrica estatica que o chama sao
     * caminhos como quaisquer outros ate ele.
     */
    private static boolean isRefusalConstructor(JavaCodeUnit codeUnit) {
        return codeUnit instanceof JavaConstructor && codeUnit.getOwner().isAssignableTo(DomainException.class);
    }

    /**
     * O que o acesso pode executar, dentro de dominio e aplicacao: o alvo e as sobrescritas e
     * implementacoes importadas — a chamada por uma interface executa uma delas.
     */
    private static Stream<JavaCodeUnit> candidatesOf(JavaCodeUnitAccess<?> access) {
        Optional<? extends JavaCodeUnit> target = access.getTarget().resolveMember();
        return target.stream()
                .flatMap(codeUnit ->
                        Stream.concat(Stream.of(codeUnit), overridesOf(codeUnit, access.getOriginOwner())))
                .filter(codeUnit -> isInnerLayer(codeUnit.getOwner()));
    }

    /**
     * Sobrescritas e implementacoes importadas do metodo, que a chamada pode executar.
     *
     * <p>Para um tipo do projeto, qualquer implementacao importada conta. Para uma interface de fora
     * — {@code Supplier}, {@code Function} —, so as que a propria classe de quem chama instancia, como
     * a classe anonima montada ali mesmo. Contar todas fazia de cada {@code Result.map}, que chama
     * {@code Function.apply}, uma recusa, bastando que alguma {@code Function} do dominio recusasse.
     * Classes de fora nao entram: todo objeto estende {@code Object}, e cada {@code toString} viraria
     * candidato.
     */
    private static Stream<JavaCodeUnit> overridesOf(JavaCodeUnit target, JavaClass caller) {
        JavaClass owner = target.getOwner();
        boolean overridable = target instanceof JavaMethod && !target.getModifiers().contains(JavaModifier.STATIC);
        if (!overridable || !(isInnerLayer(owner) || owner.isInterface())) {
            return Stream.empty();
        }
        Set<JavaClass> implementations = isInnerLayer(owner)
                ? owner.getAllSubclasses()
                : instantiatedBy(caller).stream()
                        .filter(instantiated -> instantiated.isAssignableTo(owner.getName()))
                        .collect(Collectors.toSet());
        List<String> parameters = parameterNamesOf(target);
        return implementations.stream()
                .flatMap(subclass -> subclass.getMethods().stream())
                .filter(method -> method.getName().equals(target.getName()))
                .filter(method -> parameterNamesOf(method).equals(parameters))
                .map(JavaCodeUnit.class::cast);
    }

    /** Classes que a classe instancia, por {@code new} ou por referencia ao construtor. */
    private static Set<JavaClass> instantiatedBy(JavaClass javaClass) {
        return javaClass.getCodeUnits().stream()
                .flatMap(codeUnit -> Stream.<JavaCodeUnitAccess<?>>concat(
                        codeUnit.getConstructorCallsFromSelf().stream(),
                        codeUnit.getConstructorReferencesFromSelf().stream()))
                .map(JavaCodeUnitAccess::getTargetOwner)
                .collect(Collectors.toSet());
    }

    private static List<String> parameterNamesOf(JavaCodeUnit codeUnit) {
        return codeUnit.getRawParameterTypes().stream().map(JavaClass::getName).toList();
    }

    /** Dominio e aplicacao, de qualquer contexto delimitado: as camadas onde a recusa nasce e e capturada. */
    private static boolean isInnerLayer(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        return packageName.contains(".domain") || packageName.contains(".application");
    }

    /** Verdadeiro quando o proprio acesso esta dentro de um {@code try} que captura a recusa. */
    private static boolean isCaughtAtTheSite(JavaCodeUnitAccess<?> access) {
        return access.getOwner().getTryCatchBlocks().stream()
                .filter(block -> block.getAccessesContainedInTryBlock().contains(access))
                .anyMatch(block -> block.getCaughtThrowables().stream()
                        .anyMatch(caught -> caught.isAssignableFrom(DomainException.class)));
    }
}
