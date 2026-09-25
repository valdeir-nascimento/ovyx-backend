package io.github.ovyx.identity.presentation.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

import io.github.ovyx.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sem origem configurada para a documentação, a API não fala com nenhuma outra origem: liberar é
 * decisão de cada ambiente, e não o padrão (FR-026).
 */
@AutoConfigureMockMvc
@DisplayName("API docs CORS off by default")
class ApiDocsCorsOffByDefaultIT extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("refuses even the usual documentation origin when none is configured")
    void givenNoConfiguredOrigin_whenPreflightingTheSignInFromTheDocs_thenRefuseIt() throws Exception {
        // given
        String usualDocs = "http://localhost:9090";

        // when
        MockHttpServletResponse response = mockMvc.perform(options("/api/v1/auth/sign-in")
                        .header(HttpHeaders.ORIGIN, usualDocs)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andReturn()
                .getResponse();

        // then
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }
}
