package io.github.ovyx.identity.domain.valueobject;

public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("hash de senha nao pode ser vazio");
        }
        if (!value.startsWith("{") || value.indexOf('}') < 2) {
            throw new IllegalArgumentException(
                "hash de senha precisa carregar o prefixo do algoritmo, no formato {algoritmo}conteudo");
        }
    }

    public static PasswordHash of(String value) {
        return new PasswordHash(value);
    }

    public String algorithm() {
        return value.substring(1, value.indexOf('}'));
    }

    @Override
    public String toString() {
        return "****";
    }
}
