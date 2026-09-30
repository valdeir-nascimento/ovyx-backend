package io.github.ovyx.shared.presentation;

import io.github.ovyx.shared.application.spreadsheet.SpreadsheetFile;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * A resposta de sucesso de uma exportacao (R-004 e R-005 da 007): o arquivo, com o tipo do .xlsx, o nome para
 * salvar e sem cache.
 *
 * <p>O sucesso e montado aqui porque o corpo e binario e nao passa pelo conversor JSON; a falha continua indo
 * pelo {@link ResultHttpMapper#problem}, que decide o status.
 */
public final class SpreadsheetResponses {

    /** O tipo das planilhas do Excel. */
    public static final String SPREADSHEET = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private SpreadsheetResponses() {}

    public static ResponseEntity<byte[]> of(SpreadsheetFile file) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(SPREADSHEET))
                .header(
                        "Content-Disposition",
                        ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .cacheControl(CacheControl.noStore())
                .body(file.content());
    }
}
