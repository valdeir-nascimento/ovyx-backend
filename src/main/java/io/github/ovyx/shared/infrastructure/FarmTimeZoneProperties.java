package io.github.ovyx.shared.infrastructure;

import java.time.DateTimeException;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * O fuso da granja (R-006 da 003; R-004 da 005).
 *
 * @param timeZone o fuso da granja, do banco IANA: decide o que e "hoje" para a data da coleta e para a data
 *     da pesagem. Sem configuracao, o horario de Brasilia.
 */
@ConfigurationProperties(prefix = "ovyx.farm")
public record FarmTimeZoneProperties(@DefaultValue("America/Sao_Paulo") String timeZone) {

    public FarmTimeZoneProperties {
        try {
            ZoneId.of(timeZone);
        } catch (DateTimeException unknown) {
            // Sem encadear a causa: quem le a falha da subida precisa do nome da propriedade, e nao do fuso.
            throw new IllegalArgumentException(
                    "ovyx.farm.time-zone precisa ser um fuso do banco IANA, como America/Sao_Paulo: "
                            + timeZone);
        }
    }

    /** O fuso da granja. */
    public ZoneId zone() {
        return ZoneId.of(timeZone);
    }
}
