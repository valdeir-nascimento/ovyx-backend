package io.github.ovyx.farm.domain.port;

import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import java.util.Optional;

/**
 * Carrega e grava o agregado {@link Sector} inteiro, com as gaiolas dele (R-003).
 *
 * <p>Nao ha exclusao: setores e gaiolas sao inativados, e nunca apagados (FR-012).
 */
public interface SectorRepository extends SectorRoster {

    Optional<Sector> findById(SectorId id);

    /**
     * Grava o setor.
     *
     * <p>A carga e a gravacao valem juntas dentro da transacao do comando: se outro comando gravou o
     * mesmo setor no meio-tempo, a versao nao confere e a gravacao e recusada, em vez de a copia
     * vencida apagar a mais nova.
     */
    void save(Sector sector);
}
