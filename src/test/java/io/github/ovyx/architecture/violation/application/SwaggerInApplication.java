package io.github.ovyx.architecture.violation.application;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Comando da camada de aplicacao anotado com OpenAPI. Documentar o contrato HTTP e papel da
 * apresentacao; aqui, o caso de uso ficaria preso ao formato de uma entrega especifica. Existe
 * apenas para {@link io.github.ovyx.architecture.ArchitectureRulesSelfCheckTest} comprovar que a
 * suite de arquitetura reprova esta situacao.
 */
@Schema(description = "comando que nao deveria conhecer o OpenAPI")
public record SwaggerInApplication(String value) {}
