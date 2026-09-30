package io.github.ovyx.shared.infrastructure.spreadsheet;

import io.github.ovyx.shared.application.spreadsheet.SpreadsheetWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** O gravador das planilhas das exportacoes, para os handlers do production e do farm (R-003 da 007). */
@Configuration
public class SpreadsheetConfig {

    @Bean
    public SpreadsheetWriter spreadsheetWriter() {
        return new FastexcelSpreadsheetWriter();
    }
}
