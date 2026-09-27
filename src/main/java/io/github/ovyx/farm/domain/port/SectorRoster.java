package io.github.ovyx.farm.domain.port;

import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.valueobject.SectorName;

/**
 * O que um setor consulta sobre os demais para guardar a regra que vale entre eles.
 *
 * <p>O nome unico entre os setores ativos (FR-002) nao cabe num agregado isolado: depende dos outros
 * setores. O agregado continua sendo quem decide e recusa (principio II); esta porta so responde a
 * pergunta dele, como o {@code CaretakerRoster} do identity.
 *
 * <p>Somente leitura, de proposito: por ela o agregado nao altera nenhum outro setor. O
 * {@link SectorRepository} a estende, e e ele que o tratador entrega ao agregado.
 */
public interface SectorRoster {

    /** Se outro setor ativo, que nao {@code exceptId}, ja usa o nome, comparado sem maiusculas. */
    boolean anotherActiveSectorNamed(SectorName name, SectorId exceptId);
}
