package io.github.ovyx.shared.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.shared.application.spreadsheet.SpreadsheetFile;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** A resposta de uma exportação (R-004 e R-005 da 007): o arquivo, com o tipo, o nome e sem cache. */
@DisplayName("SpreadsheetResponses")
class SpreadsheetResponsesTest {

    private static final byte[] CONTENT = "PK conteúdo do xlsx".getBytes(StandardCharsets.UTF_8);

    @Test
    @DisplayName("answers the file with the type of xlsx, the name to save and no cache")
    void givenSpreadsheetFile_whenBuildingTheResponse_thenSendTheFileWithTheThreeHeaders() {
        // given
        SpreadsheetFile file = new SpreadsheetFile("gaiolas-codornas-galpao-1.xlsx", CONTENT);

        // when
        ResponseEntity<byte[]> response = SpreadsheetResponses.of(file);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"gaiolas-codornas-galpao-1.xlsx\"");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
        assertThat(response.getBody()).isEqualTo(CONTENT);
    }
}
