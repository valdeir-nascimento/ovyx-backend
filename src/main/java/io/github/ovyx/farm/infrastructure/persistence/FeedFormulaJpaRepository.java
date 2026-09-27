package io.github.ovyx.farm.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio Spring Data da tabela {@code feed_formula}. */
interface FeedFormulaJpaRepository extends JpaRepository<FeedFormulaRecord, UUID> {

    /**
     * Se outra formula usa o nome, com a mesma comparacao do indice unico {@code ux_feed_formula_name}:
     * {@code lower(name)} entre todas, ativas e inativas.
     */
    @Query(
            value = """
                    select exists(
                        select 1 from feed_formula where lower(name) = lower(:name) and id <> :exceptId)
                    """,
            nativeQuery = true)
    boolean existsNamed(@Param("name") String name, @Param("exceptId") UUID exceptId);
}
