package ovyxarchviolation.presentation;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Controller de verdade, com {@code @RestController} e sem o sufixo {@code Controller}, que leva
 * anotacao do springdoc e nao implementa {@code AnnotatedResourceApi}. Prova o ramo do estereotipo
 * das regras de documentacao, que a violacao por sufixo nao exercita.
 *
 * <p>Fica fora de {@code io.github.ovyx} de proposito: ali o scan de componentes da aplicacao o
 * registraria como endpoint em todo teste de integracao. Existe apenas para
 * {@code ArchitectureRulesSelfCheckTest}.
 */
@RestController
public class AnnotatedResource {

    /** Parametros de paginacao, agrupados como o springdoc sugere. */
    public record Page(int number, int size) {}

    @GetMapping("/violacao-deliberada")
    public String list(@ParameterObject Page page) {
        return "documentado no controller";
    }
}
