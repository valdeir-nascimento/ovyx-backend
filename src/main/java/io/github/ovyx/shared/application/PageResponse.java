package io.github.ovyx.shared.application;

import java.util.List;
import java.util.function.Function;

/**
 * Pagina devolvida por uma query.
 *
 * <p>
 * Tipo proprio, e nao o Page do Spring Data, para que o contrato nao mude quando
 * a implementacao de persistencia mudar. Vive em application, e nao em
 * presentation, porque quem produz a pagina e o QueryHandler.
 * </p>
 *
 * @param <T> tipo do item da pagina.
 */
public record PageResponse<T>(
    List<T> content,
    PageMetadata metadata
) {

    public <R> PageResponse<R> map(final Function<T, R> mapper) {
        return new PageResponse<>(content.stream().map(mapper).toList(), metadata);
    }

    public static <T> PageResponse<T> of(
        final List<T> content,
        final int page,
        final int size,
        final long totalElements
    ) {
        final int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, new PageMetadata(page, size, totalElements, totalPages));
    }
}
