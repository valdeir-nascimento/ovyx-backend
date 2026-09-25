package io.github.ovyx.identity.presentation.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.shared.presentation.AccessRoles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O nome do perfil que as regras de rota exigem é o mesmo que a sessão carrega (R-008).
 *
 * <p>As rotas de cada contexto pedem o perfil pela constante do {@code shared}, e a sessão guarda o
 * nome do {@link Role} do {@code identity}. Renomeado de um lado só, a regra deixava de casar e o
 * administrador perdia o acesso a tudo, sem nenhum outro teste de domínio acusar.
 */
@DisplayName("Access roles alignment")
class AccessRolesAlignmentTest {

    @Test
    @DisplayName("the administrator role required by the routes is the one the session carries")
    void givenAdministratorRole_whenComparingWithTheSharedConstant_thenFindTheSameName() {
        // given
        Role administrator = Role.ADMINISTRATOR;

        // when
        String required = AccessRoles.ADMINISTRATOR;

        // then
        assertThat(required).isEqualTo(administrator.name());
    }
}
