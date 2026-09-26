package io.github.ovyx.production.infrastructure;

import java.time.DateTimeException;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuracao do contexto production.
 *
 * @param timeZone o fuso da granja, do banco IANA (R-006): decide o que e "hoje" para a data da coleta.
 *     Sem configuracao, o horario de Brasilia.
 */
@ConfigurationProperties(prefix = "ovyx.production")
public record ProductionProperties(@DefaultValue("America/Sao_Paulo") String timeZone) {

    public ProductionProperties {
        try {
            ZoneId.of(timeZone);
        } catch (DateTimeException unknown) {
            // Sem encadear a causa: quem le a falha da subida precisa do nome da propriedade, e nao do fuso.
            throw new IllegalArgumentException(
                    "ovyx.production.time-zone precisa ser um fuso do banco IANA, como America/Sao_Paulo: "
                            + timeZone);
        }
    }

    /** O fuso da granja. */
    public ZoneId zone() {
        return ZoneId.of(timeZone);
    }
}
