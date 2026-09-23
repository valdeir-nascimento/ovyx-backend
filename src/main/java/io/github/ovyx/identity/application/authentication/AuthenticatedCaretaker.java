package io.github.ovyx.identity.application.authentication;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.Role;

/**
 * Modelo de leitura do responsavel autenticado.
 *
 * <p>Construido para a necessidade da tela — casca autenticada e decisao de navegacao — e nao a
 * partir do agregado (principio V). Nao tem campo de senha nem de hash, e por isso nao ha como o
 * hash escapar ate a borda HTTP por descuido.
 */
public record AuthenticatedCaretaker(CaretakerId id, String fullName, Role role, boolean mustChangePassword) {}
