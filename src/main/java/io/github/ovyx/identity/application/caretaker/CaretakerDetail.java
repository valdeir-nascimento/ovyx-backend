package io.github.ovyx.identity.application.caretaker;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.identity.domain.model.Role;
import java.time.Instant;

/**
 * Um responsavel na tela de detalhe e de edicao: o resumo, mais o CPF e as datas.
 *
 * <p>Montado direto da consulta, sem passar por agregado (principio V). Nao tem lugar para a senha
 * nem para o hash, em nenhuma hipotese.
 */
public record CaretakerDetail(
        CaretakerId id,
        String fullName,
        String cpf,
        String email,
        String mobilePhone,
        Role role,
        CaretakerStatus status,
        Instant createdAt,
        Instant updatedAt) {}
