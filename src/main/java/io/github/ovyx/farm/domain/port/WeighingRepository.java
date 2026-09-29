package io.github.ovyx.farm.domain.port;

import io.github.ovyx.farm.domain.model.Weighing;
import io.github.ovyx.farm.domain.model.WeighingId;
import java.util.Optional;

/** O repositorio da raiz {@link Weighing} (R-003 da 005), que tambem responde pelas datas ja pesadas. */
public interface WeighingRepository extends WeighingRoster {

    /** A pesagem, valida ou anulada. */
    Optional<Weighing> findById(WeighingId id);

    /**
     * Grava a pesagem.
     *
     * <p>A carga e a gravacao valem juntas dentro da transacao do comando: se outro comando gravou a mesma
     * pesagem no meio-tempo, a versao nao confere e a gravacao e recusada.
     */
    void save(Weighing weighing);
}
