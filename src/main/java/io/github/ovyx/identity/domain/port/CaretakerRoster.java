package io.github.ovyx.identity.domain.port;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.valueobject.Cpf;
import io.github.ovyx.identity.domain.valueobject.Email;
import io.github.ovyx.identity.domain.valueobject.MobilePhone;

/**
 * O que um responsavel consulta sobre os demais para guardar as regras que valem entre eles.
 *
 * <p>Unicidade do CPF, de e-mail e de celular entre ativos (FR-016) e o ultimo administrador ativo
 * (FR-019) nao cabem num agregado isolado: dependem dos outros responsaveis. O agregado continua
 * sendo quem decide e recusa (principio II); esta porta so responde as perguntas dele, como o
 * {@link PasswordHasher} responde as de senha.
 *
 * <p>Somente leitura, de proposito: por ela o agregado nao altera nenhum outro responsavel. O
 * {@link CaretakerRepository} a estende, e e ele que o tratador entrega ao agregado.
 */
public interface CaretakerRoster {

    /** Se outro responsavel, ativo ou nao, ja tem este CPF: a mesma pessoa nao existe duas vezes. */
    boolean isCpfTakenByAnother(Cpf cpf, CaretakerId self);

    /** Se outro responsavel ativo ja usa este e-mail como identificador de acesso. */
    boolean isEmailTakenByAnotherActive(Email email, CaretakerId self);

    /** Se outro responsavel ativo ja usa este celular como identificador de acesso. */
    boolean isMobilePhoneTakenByAnotherActive(MobilePhone mobilePhone, CaretakerId self);

    /** Quantos administradores ativos existem, incluindo quem pergunta. */
    long countActiveAdministrators();
}
