package io.github.ovyx.identity.domain.valueobject;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.domain.DomainException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * O codigo do link de recuperacao da senha (R-002 da 012): 256 bits aleatorios em Base64 URL, sem preenchimento, o
 * que da 43 caracteres. Vai no fragmento do endereco ({@code /redefinir-senha#codigo}).
 *
 * <p>O codigo e o segredo: so o {@link #hash() resumo} dele e guardado, e quem le o banco nao tem como montar o link
 * (FR-009). Por isso o {@link #toString()} nao o mostra, e nenhum registro o recebe.
 *
 * <p>O resumo e um SHA-256 sem sal. Para um segredo aleatorio desse tamanho basta: o Argon2 das senhas protege
 * segredos fracos, que se adivinham por dicionario, e aqui so custaria tempo.
 *
 * @param value o codigo, como vai no link
 */
public record RecoveryToken(String value) {

    /** 43 caracteres do alfabeto do Base64 URL: os 32 bytes do codigo, sem o {@code =} do preenchimento. */
    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    public RecoveryToken {
        Objects.requireNonNull(value, "value");
    }

    /**
     * Le o codigo que chegou no link.
     *
     * <p>O construtor nao confere o formato, como nos demais objetos de valor: ele serve a quem ja tem um codigo
     * valido em maos, como o gerador.
     *
     * @throws DomainException com {@code RECOVERY_LINK_INVALID}, quando o codigo esta fora do formato
     */
    public static RecoveryToken of(String raw) {
        if (raw == null || !FORMAT.matcher(raw).matches()) {
            // O codigo fora do formato e so mais um link que nao vale: a recusa e a mesma do usado e do vencido.
            throw new DomainException(
                    IdentityErrorCode.RECOVERY_LINK_INVALID, IdentityErrorCode.RECOVERY_LINK_INVALID_MESSAGE);
        }
        return new RecoveryToken(raw);
    }

    /** O resumo SHA-256 do codigo, em 64 caracteres hexadecimais minusculos: o que se guarda e se procura. */
    public String hash() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException absent) {
            // Toda JVM tem o SHA-256 (a especificacao do Java o exige); faltar e defeito do ambiente.
            throw new IllegalStateException("SHA-256 indisponível na JVM", absent);
        }
    }

    /** Nunca mostra o codigo. */
    @Override
    public String toString() {
        return "RecoveryToken[****]";
    }
}
