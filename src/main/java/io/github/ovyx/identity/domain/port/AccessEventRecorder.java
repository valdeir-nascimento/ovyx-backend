package io.github.ovyx.identity.domain.port;

import io.github.ovyx.identity.domain.model.AccessEvent;

/**
 * Porta de saida para a trilha de auditoria de acesso (FR-006).
 *
 * <p>Somente inclusao: nao ha operacao de leitura, alteracao ou remocao nesta porta. Auditoria que
 * pode ser alterada pelo proprio sistema nao serve de auditoria.
 */
public interface AccessEventRecorder {

    void record(AccessEvent event);
}
