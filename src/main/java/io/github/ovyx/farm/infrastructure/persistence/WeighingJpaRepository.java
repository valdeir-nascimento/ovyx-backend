package io.github.ovyx.farm.infrastructure.persistence;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WeighingJpaRepository extends JpaRepository<WeighingRecord, UUID> {

    /**
     * Se a gaiola tem outra pesagem valida no dia, com a mesma condicao do indice unico parcial
     * {@code ux_weighing_cage_day}: so as validas contam.
     */
    @Query(
            value = """
                    select exists(
                        select 1 from weighing
                         where cage_id = :cageId and weighed_on = :day and status = 'VALID' and id <> :exceptId)
                    """,
            nativeQuery = true)
    boolean existsValidOn(
            @Param("cageId") UUID cageId, @Param("day") LocalDate day, @Param("exceptId") UUID exceptId);
}
