package io.github.ovyx.farm.infrastructure.persistence;

import io.github.ovyx.farm.domain.model.Cage;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.valueobject.Battery;
import io.github.ovyx.farm.domain.valueobject.BirdCount;
import io.github.ovyx.farm.domain.valueobject.CageNumber;
import io.github.ovyx.farm.domain.valueobject.SectorDescription;
import io.github.ovyx.farm.domain.valueobject.SectorName;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Traducao entre o agregado, com as gaiolas, e as linhas das tabelas.
 *
 * <p>Na volta usa {@code Sector.restore} e {@code Cage.restore}, e nao as fabricas de cadastro: os
 * dados ja foram validados quando entraram.
 */
final class SectorRecordMapper {

    private SectorRecordMapper() {}

    static SectorRecord toRecord(Sector sector) {
        SectorRecord record = new SectorRecord(
                sector.id().value(),
                sector.name().value(),
                descriptionOf(sector),
                sector.status(),
                sector.createdAt(),
                sector.updatedAt());
        sector.cages().forEach(cage -> record.getCages().add(toRecord(cage)));
        return record;
    }

    /**
     * Aplica o agregado sobre as linhas carregadas.
     *
     * @return se alguma coluna do setor mudou ou alguma gaiola entrou: nesses casos a versao do setor ja
     *     sobe sozinha; nos outros, quem grava precisa subi-la
     */
    static boolean applyTo(SectorRecord record, Sector sector) {
        boolean sectorChanged = !Objects.equals(record.getName(), sector.name().value())
                || !Objects.equals(record.getDescription(), descriptionOf(sector))
                || record.getStatus() != sector.status()
                || !Objects.equals(record.getUpdatedAt(), sector.updatedAt());
        record.apply(sector.name().value(), descriptionOf(sector), sector.status(), sector.updatedAt());

        Map<UUID, CageRecord> stored =
                record.getCages().stream().collect(Collectors.toMap(CageRecord::getId, Function.identity()));
        boolean cagesAdded = false;
        for (Cage cage : sector.cages()) {
            CageRecord existing = stored.get(cage.id().value());
            if (existing == null) {
                record.getCages().add(toRecord(cage));
                cagesAdded = true;
            } else {
                existing.apply(
                        cage.battery().value(),
                        cage.number().value(),
                        cage.birdCount().value(),
                        cage.status(),
                        cage.deactivatedWithSector(),
                        cage.updatedAt());
            }
        }
        return sectorChanged || cagesAdded;
    }

    static Sector toDomain(SectorRecord record) {
        return Sector.restore(
                SectorId.of(record.getId()),
                new SectorName(record.getName()),
                record.getDescription() == null ? null : new SectorDescription(record.getDescription()),
                record.getStatus(),
                record.getCages().stream().map(SectorRecordMapper::toDomain).toList(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    private static CageRecord toRecord(Cage cage) {
        return new CageRecord(
                cage.id().value(),
                cage.battery().value(),
                cage.number().value(),
                cage.birdCount().value(),
                cage.status(),
                cage.deactivatedWithSector(),
                cage.createdAt(),
                cage.updatedAt());
    }

    private static Cage toDomain(CageRecord record) {
        return Cage.restore(
                CageId.of(record.getId()),
                new Battery(record.getBattery()),
                new CageNumber(record.getNumber()),
                new BirdCount(record.getBirdCount()),
                record.getStatus(),
                record.isDeactivatedWithSector(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    private static String descriptionOf(Sector sector) {
        return sector.description().map(SectorDescription::value).orElse(null);
    }
}
