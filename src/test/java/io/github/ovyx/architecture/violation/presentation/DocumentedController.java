package io.github.ovyx.architecture.violation.presentation;

import io.swagger.v3.oas.annotations.Operation;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Controller com a documentacao da API no proprio metodo, e sem a interface {@code DocumentedApi}
 * que deveria carrega-la. Nao leva {@code @RestController} de proposito: seria registrado como
 * endpoint em todo teste de integracao. Existe apenas para o autoteste da suite de arquitetura.
 */
public class DocumentedController {

    @Operation(summary = "Operacao documentada no lugar errado")
    public String handle() {
        return "documentado no controller";
    }
}
