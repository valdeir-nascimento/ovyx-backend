package io.github.ovyx.production.domain.model;

import java.util.Optional;
import java.util.UUID;

/**
 * Identidade do relatorio diario. UUID gerado pela aplicacao na abertura: a identidade nasce com o
 * agregado, antes de qualquer ida ao banco.
 */
public record DailyReportId(UUID value) {

    public DailyReportId {
        if (value == null) {
            throw new IllegalArgumentException("identidade do relatório é obrigatória");
        }
    }

    public static DailyReportId generate() {
        return new DailyReportId(UUID.randomUUID());
    }

    public static DailyReportId of(UUID value) {
        return new DailyReportId(value);
    }

    /** O relatorio que o endereco aponta, ou nenhum: o malformado recebe a mesma resposta do inexistente. */
    public static Optional<DailyReportId> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            UUID value = UUID.fromString(raw.strip());
            return value.toString().equalsIgnoreCase(raw.strip())
                    ? Optional.of(new DailyReportId(value))
                    : Optional.empty();
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
