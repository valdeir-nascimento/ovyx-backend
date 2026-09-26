package io.github.ovyx.production.domain.model;

import java.util.List;

/**
 * O setor como o production precisa dele (R-004): o nome, se esta ativo e as gaiolas ativas, por bateria
 * e numero. E a resposta da porta {@code FarmStructure}, com as palavras do production, e nao o agregado
 * do farm.
 *
 * @param id o setor
 * @param name o nome, para as telas
 * @param active se o setor esta ativo; num setor inativo, os relatorios so consultam (FR-020)
 * @param activeCages as gaiolas ativas, por bateria e numero; um setor inativo nao tem nenhuma
 */
public record FarmSector(SectorId id, String name, boolean active, List<FarmCage> activeCages) {

    public FarmSector {
        activeCages = List.copyOf(activeCages);
    }
}
