package io.github.ovyx.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Testes do identificador de correlacao.
 *
 * <p>Sem ele, o log nao permite reconstruir uma requisicao especifica. E ele precisa sair do MDC ao
 * fim: a thread volta para o pool, e um identificador esquecido passa a etiquetar a requisicao
 * seguinte com o rastro da anterior.
 */
@DisplayName("CorrelationIdFilter")
class CorrelationIdFilterTest {

    private static final String MDC_KEY = "correlationId";

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    private static FilterChain capturing(StringBuilder seenInsideTheChain) {
        return (request, response) -> seenInsideTheChain.append(MDC.get(MDC_KEY));
    }

    @Test
    @DisplayName("Keeps the correlation id the caller sent")
    void givenRequestWithCorrelationId_whenFiltering_thenKeepItInTheMdcAndEchoItBack() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "correlacao-do-chamador");
        MockHttpServletResponse response = new MockHttpServletResponse();
        StringBuilder insideTheChain = new StringBuilder();

        // when
        filter.doFilter(request, response, capturing(insideTheChain));

        // then
        assertThat(insideTheChain).hasToString("correlacao-do-chamador");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("correlacao-do-chamador");
    }

    @Test
    @DisplayName("Creates a correlation id when the caller sent none")
    void givenRequestWithoutCorrelationId_whenFiltering_thenCreateOneAndAnswerWithIt() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        StringBuilder insideTheChain = new StringBuilder();

        // when
        filter.doFilter(request, response, capturing(insideTheChain));

        // then
        assertThat(UUID.fromString(insideTheChain.toString())).isNotNull();
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo(insideTheChain.toString());
    }

    @Test
    @DisplayName("Leaves no correlation id behind for the next request on the same thread")
    void givenFinishedRequest_whenFiltering_thenRemoveTheCorrelationIdFromTheMdc() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, (ignored, alsoIgnored) -> {
            // a cadeia nao precisa fazer nada: o que importa e o estado depois dela
        });

        // then
        assertThat(MDC.get(MDC_KEY)).isNull();
    }
}
