package io.github.ovyx.shared.application;

/**
 * Metadados de paginacao.
 *
 * <p>
 * {@code totalElements} e obrigatorio em toda listagem: o consumidor
 * precisa saber quantos registros atendem ao filtro, nao so os da pagina atual.
 * </p>
 */
public record PageMetadata(
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
