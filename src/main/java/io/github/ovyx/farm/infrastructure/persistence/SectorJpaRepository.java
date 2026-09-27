package io.github.ovyx.farm.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data da tabela {@code sector}.
 *
 * <p>Detalhe de infraestrutura: quem o dominio conhece e {@code SectorRepository}, implementada por
 * {@code JpaSectorRepository} sobre esta interface.
 */
interface SectorJpaRepository extends JpaRepository<SectorRecord, UUID> {

    /**
     * Se outro setor ativo usa o nome, com a mesma comparacao do indice unico parcial
     * {@code ux_sector_name_active}: {@code lower(name)} entre os ativos.
     */
    @Query(
            value = """
                    select exists(
                        select 1 from sector
                         where lower(name) = lower(:name) and status = 'ACTIVE' and id <> :exceptId)
                    """,
            nativeQuery = true)
    boolean existsActiveNamed(@Param("name") String name, @Param("exceptId") UUID exceptId);
}
