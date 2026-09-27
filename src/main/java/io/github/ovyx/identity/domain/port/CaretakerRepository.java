package io.github.ovyx.identity.domain.port;

import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.valueobject.Cpf;
import java.util.Optional;

/**
 * Porta de saida para persistencia da raiz de agregado {@code Caretaker}.
 *
 * <p>Serve o <strong>lado de escrita</strong> do CQRS: devolve agregados, nao modelos de leitura.
 * Consulta para tela tem porta propria, declarada na funcionalidade de {@code application} que a usa
 * (principio V).
 *
 * <p>Estende {@link CaretakerRoster}: e o mesmo repositorio que responde ao agregado as perguntas
 * sobre os demais responsaveis — unicidade e ultimo administrador —, sem que o agregado ganhe
 * acesso a gravacao.
 */
public interface CaretakerRepository extends CaretakerRoster {

    /**
     * Grava o agregado, criando ou atualizando conforme a identidade ja exista.
     *
     * <p>A escrita concorrente so e detectada porque o agregado foi carregado por este repositorio na
     * mesma transacao em que e gravado: o adaptador aplica o resultado sobre a linha carregada, e o
     * banco confere a versao dela. Quem gravar uma copia carregada em outra transacao perde essa
     * verificacao sem aviso — a copia vencida grava por cima.
     */
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

    /**
     * Localiza o responsavel com o CPF, ativo ou nao.
     *
     * <p>Ha no maximo um: o CPF nao se repete entre responsaveis, em nenhuma situacao (FR-016).
     */
    Optional<Caretaker> findByCpf(Cpf cpf);
}
