package io.github.ovyx.identity.infrastructure.persistence;

import io.github.ovyx.identity.domain.model.CaretakerStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data da tabela {@code caretaker}.
 *
 * <p>Detalhe de infraestrutura: quem o dominio conhece e {@code CaretakerRepository}, implementada
 * por {@code JpaCaretakerRepository} sobre esta interface.
 */
interface CaretakerJpaRepository extends JpaRepository<CaretakerRecord, UUID> {

    /**
     * Localiza por igualdade exata de e-mail ou celular, com o ativo antes do inativo.
     *
     * <p>A precedencia importa porque a unicidade vale apenas entre ativos (FR-016): o mesmo
     * e-mail pode pertencer a um inativo antigo e a um ativo atual.
     */
    @Query(
            """
            select c from CaretakerRecord c
            where c.email = :identifier or c.mobilePhone = :identifier
            order by case when c.status = io.github.ovyx.identity.domain.model.CaretakerStatus.ACTIVE
                          then 0 else 1 end, c.createdAt desc
            """)
    List<CaretakerRecord> findByEmailOrMobilePhone(@Param("identifier") String identifier);

    /**
     * Os administradores ativos, travados ate o fim da transacao.
     *
     * <p>Com a trava, duas transacoes que decidem sobre o ultimo administrador se enfileiram: a
     * segunda espera a primeira, e o PostgreSQL reavalia a condicao nas linhas que ela alterou. Quem
     * inativou um ja nao conta.
     */
    @Query(
            value = "select id from caretaker where role = 'ADMINISTRATOR' and status = 'ACTIVE' for update",
            nativeQuery = true)
    List<UUID> lockActiveAdministrators();

    Optional<CaretakerRecord> findByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, UUID id);

    boolean existsByEmailAndStatusAndIdNot(String email, CaretakerStatus status, UUID id);

    boolean existsByMobilePhoneAndStatusAndIdNot(String mobilePhone, CaretakerStatus status, UUID id);
}
