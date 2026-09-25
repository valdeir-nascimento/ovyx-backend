package io.github.ovyx.farm.domain.valueobject;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Rule;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Nome do setor (FR-001, FR-002).
 *
 * <p>O tamanho e contado em caracteres, e nao em unidades UTF-16, como o {@code char_length} da
 * restricao do banco: contado em unidades, um nome de um caractere fora do plano basico passava aqui e
 * a gravacao era recusada com erro tecnico.
 *
 * @param value nome ja aparado, de 2 a 80 caracteres
 */
public record SectorName(String value) {

    private static final int MINIMUM_LENGTH = 2;
    private static final int MAXIMUM_LENGTH = 80;
    private static final String FIELD = "name";

    private static final List<Rule<String>> RULES = List.of(
            Rule.of(
                    name -> charactersOf(name) >= MINIMUM_LENGTH,
                    FarmErrorCode.SECTOR_NAME_TOO_SHORT,
                    "O nome do setor deve ter ao menos 2 caracteres."),
            Rule.of(
                    name -> charactersOf(name) <= MAXIMUM_LENGTH,
                    FarmErrorCode.SECTOR_NAME_TOO_LONG,
                    "O nome do setor deve ter no máximo 80 caracteres."));

    /**
     * Construtor canonico: so a garantia estrutural, sem regra de negocio.
     *
     * <p>E o caminho da reidratacao, que le do banco um valor ja validado.
     */
    public SectorName {
        Objects.requireNonNull(value, "value");
    }

    /**
     * Registra no {@link Notification} as violacoes do campo, sem lancar.
     *
     * <p>E o caminho do agregado, que reune as violacoes de todos os campos antes de recusar (FR-017).
     */
    public static void validate(String raw, Notification notification) {
        if (notification.requirePresent(FIELD, raw, FarmErrorCode.SECTOR_NAME_REQUIRED, "Informe o nome do setor.")) {
            notification.check(FIELD, raw.strip(), RULES);
        }
    }

    /**
     * Cria o nome, recusando na hora com as violacoes do campo.
     *
     * @param raw texto como digitado
     * @throws io.github.ovyx.shared.domain.DomainException quando alguma regra do campo e violada
     */
    public static SectorName of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);
        return new SectorName(raw.strip());
    }

    /**
     * Se os dois nomes sao o mesmo para a regra de unicidade: sem distinguir maiusculas (FR-002). Os
     * espacos das pontas ja ficaram de fora na criacao.
     */
    public boolean sameAs(SectorName other) {
        return comparisonKey().equals(other.comparisonKey());
    }

    /** A forma do nome que a unicidade compara, a mesma do indice {@code lower(name)} do banco. */
    public String comparisonKey() {
        return value.toLowerCase(Locale.ROOT);
    }

    private static int charactersOf(String text) {
        return text.codePointCount(0, text.length());
    }

    @Override
    public String toString() {
        return value;
    }
}
