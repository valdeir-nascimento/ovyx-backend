package io.github.ovyx.identity.application.caretaker;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.identity.domain.model.Role;

/**
 * Linha da lista e da pesquisa de responsaveis.
 *
 * <p>Montada direto da consulta, sem passar por agregado (principio V). Nao tem lugar para a senha
 * nem para o hash, em nenhuma hipotese.
 */
public record CaretakerSummary(
        CaretakerId id, String fullName, String email, String mobilePhone, Role role, CaretakerStatus status) {}
