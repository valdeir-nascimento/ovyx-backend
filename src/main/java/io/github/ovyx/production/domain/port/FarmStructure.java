package io.github.ovyx.production.domain.port;

import io.github.ovyx.production.domain.model.FarmSector;
import io.github.ovyx.production.domain.model.SectorId;
import java.util.Optional;

/**
 * A estrutura da granja, como o production precisa dela (R-004): uma camada anticorrupcao.
 *
 * <p>O production nao depende do codigo do farm (R-008 da 002). Esta porta diz, com as palavras do
 * production, o que ele precisa saber de um setor; o adaptador responde lendo as tabelas do farm, e um
 * dia pode responder chamando a API dele, sem o dominio mudar.
 */
public interface FarmStructure {

    /** O setor, com a situacao e as gaiolas ativas, ou nenhum, se nao existe. */
    Optional<FarmSector> sectorOf(SectorId sectorId);
}
