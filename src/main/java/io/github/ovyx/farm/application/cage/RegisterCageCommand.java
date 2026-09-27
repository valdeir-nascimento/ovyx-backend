package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.shared.application.Command;

/**
 * Cadastro de uma gaiola num setor (FR-006).
 *
 * <p>Os campos chegam como foram digitados, inclusive o numero e as aves: decimal ou texto vira
 * violacao do campo, junto das demais, e nao corpo ilegivel (FR-017, R-013).
 *
 * @param sectorId o identificador como veio no endereco; malformado, o setor nao e encontrado
 */
public record RegisterCageCommand(String sectorId, String battery, String number, String birdCount)
        implements Command<CageId> {}
