package io.github.ovyx.identity.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Testes do objeto de valor que guarda a senha protegida. */
@DisplayName("PasswordHash")
class PasswordHashTest {

    @Test
    @DisplayName("accepts a hash with an algorithm prefix")
    void acceptsHashWithAlgorithmPrefix() {
        PasswordHash hash = PasswordHash.of("{argon2}$argon2id$v=19$m=65536,t=3,p=1$abc$def");

        assertThat(hash.algorithm()).isEqualTo("argon2");
    }

    @Test
    @DisplayName("rejects a blank hash")
    void rejectsBlankHash() {
        assertThatThrownBy(() -> PasswordHash.of("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PasswordHash.of(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects a hash without an algorithm prefix")
    void rejectsHashWithoutAlgorithmPrefix() {
        // E exatamente o formato do legado: SHA-2 nu, sem identificacao de algoritmo, sem
        // caminho de migracao possivel.
        assertThatThrownBy(() -> PasswordHash.of("5e884898da28047151d0e56f8dc6292773603d0d"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PasswordHash.of("{}vazio")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("never exposes the hash in toString")
    void neverExposesTheHashInToString() {
        PasswordHash hash = PasswordHash.of("{argon2}conteudo-secreto");

        assertThat(hash.toString()).isEqualTo("****");
        assertThat(hash.toString()).doesNotContain("conteudo-secreto");
    }

    @Test
    @DisplayName("has value equality")
    void hasValueEquality() {
        assertThat(PasswordHash.of("{argon2}abc")).isEqualTo(PasswordHash.of("{argon2}abc"));
        assertThat(PasswordHash.of("{argon2}abc")).isNotEqualTo(PasswordHash.of("{argon2}xyz"));
    }
}
