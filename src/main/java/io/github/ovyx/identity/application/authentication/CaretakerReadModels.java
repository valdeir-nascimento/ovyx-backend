package io.github.ovyx.identity.application.authentication;

import io.github.ovyx.identity.domain.model.CaretakerId;
import java.util.Optional;

/**
 * Porta de leitura da funcionalidade de acesso.
 *
 * <p>Fica em {@code application}, e nao em {@code domain}, de proposito: consulta para tela nao e
 * conceito de dominio. O lado de escrita tem porta propria
 * ({@code identity.domain.port.CaretakerRepository}), que devolve agregados; esta devolve modelos
 * de leitura montados direto da consulta, sem passar por agregado (principio V).
 *
 * <p>A implementacao vive em {@code infrastructure}.
 */
public interface CaretakerReadModels {

    Optional<AuthenticatedCaretaker> findAuthenticatedById(CaretakerId id);
}
