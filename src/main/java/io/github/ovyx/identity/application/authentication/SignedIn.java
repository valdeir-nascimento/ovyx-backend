package io.github.ovyx.identity.application.authentication;

import io.github.ovyx.identity.domain.model.CaretakerId;

/**
 * Quem entrou, e contra qual geracao de sessao a senha foi conferida (R-022 da 012).
 *
 * <p>A sessao nasce com esta geracao, e nao com a que uma consulta posterior leria. Se a senha for redefinida pelo
 * link entre a conferencia e a consulta, a entrada feita com a senha antiga nao ganha a geracao nova: a consulta a
 * recusa, e a sessao nao chega a abrir.
 *
 * @param caretakerId o responsavel autenticado
 * @param sessionGeneration a geracao de sessao do responsavel quando a senha foi conferida
 */
public record SignedIn(CaretakerId caretakerId, int sessionGeneration) {}
