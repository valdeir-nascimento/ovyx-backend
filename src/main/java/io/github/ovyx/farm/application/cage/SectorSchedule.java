package io.github.ovyx.farm.application.cage;

import java.time.LocalDate;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.shared.domain.WeighingSchedule;
import java.time.DayOfWeek;

/**
 * A agenda de pesagem de um setor como a lista de gaiolas a le (feature 010): a situacao dele e o dia da pesagem.
 *
 * @param weighingDay o dia da pesagem, ou {@code null} quando o setor segue o prazo de 7 dias
 */
public record SectorSchedule(Status status, DayOfWeek weighingDay) {

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public WeighingSchedule schedule() {
        return new WeighingSchedule(weighingDay);
    }

    /**
     * A gaiola com a situacao dela na agenda, com o dia de hoje da granja: so a gaiola ativa de setor ativo tem
     * situacao (FR-007 da 010).
     */
    public CageSummary standingOf(CageSummary cage, LocalDate today) {
        if (!isActive() || cage.status() != Status.ACTIVE) {
            return cage.withWeighing(null);
        }
        LocalDate lastWeighedOn = cage.lastWeighing() == null ? null : cage.lastWeighing().weighedOn();
        return cage.withWeighing(schedule().standingOf(today, lastWeighedOn));
    }
}
