package io.github.ovyx.identity.domain.port;

import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import java.util.Optional;

/**
 * Porta de saida para persistencia da raiz de agregado {@code Caretaker}.
 *
 * <p>Serve o <strong>lado de escrita</strong> do CQRS: devolve agregados, nao modelos de leitura.
 * Consulta para tela tem porta propria, declarada na funcionalidade de {@code application} que a usa
 * (principio V).
 *
 * <p>Contem so o que a Historia 1 usa. As verificacoes de unicidade de e-mail, celular e CPF entram
 * com a Historia 2, por TDD, junto com o cadastro que as exige.
 */
public interface CaretakerRepository {

    /** Grava o agregado, criando ou atualizando conforme a identidade ja exista. */
    void save(Caretaker caretaker);

    Optional<Caretaker> findById(CaretakerId id);

    /**
     * Localiza o responsavel cujo e-mail ou celular e exatamente o identificador informado.
     *
     * <p>O identificador chega ja na forma canonica de {@code AccessIdentifier}. Esta porta nao
     * normaliza nada por conta propria, de proposito: normalizar aqui de um jeito e na contencao de
     * outro foi o que abriu a brecha de tentativas ilimitadas.
     *
     * <p>Devolve tambem responsaveis inativos, para que o tratador registre a causa certa na
     * auditoria. Quando o identificador corresponde a um ativo e a um inativo — possivel, porque a
     * unicidade vale so entre ativos (FR-016) —, o ativo tem precedencia.
     */
    Optional<Caretaker> findByEmailOrMobilePhone(String canonicalIdentifier);

    /** Quantos administradores ativos existem. Decide se a semeadura do administrador e necessaria. */
    long countActiveAdministrators();
}
