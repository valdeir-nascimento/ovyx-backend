package io.github.ovyx.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes da protecao de senha (FR-021).
 *
 * <p>Parametros reduzidos de proposito: o custo real do Argon2id e calibrado em T122 para a maquina
 * de producao, e aqui so atrasaria a suite.
 */
@DisplayName("Argon2PasswordHasher")
class Argon2PasswordHasherTest {

    private static final Argon2Properties FAST_FOR_TESTS = new Argon2Properties(16, 32, 1, 1024, 1);

    private final Argon2PasswordHasher hasher = new Argon2PasswordHasher(FAST_FOR_TESTS);

    @Test
    @DisplayName("writes the hash with the algorithm prefix")
    void writesHashWithAlgorithmPrefix() {
        PasswordHash hash = hasher.hash("GranjaNorte2026");

        assertThat(hash.value()).startsWith("{argon2}");
        assertThat(hash.algorithm()).isEqualTo("argon2");
    }

    @Test
    @DisplayName("never stores the raw password")
    void neverStoresTheRawPassword() {
        PasswordHash hash = hasher.hash("GranjaNorte2026");

        assertThat(hash.value()).doesNotContain("GranjaNorte2026");
    }

    @Test
    @DisplayName("accepts the correct password")
    void acceptsTheCorrectPassword() {
        PasswordHash hash = hasher.hash("GranjaNorte2026");

        assertThat(hasher.matches("GranjaNorte2026", hash)).isTrue();
    }

    @Test
    @DisplayName("rejects a wrong password")
    void rejectsTheWrongPassword() {
        PasswordHash hash = hasher.hash("GranjaNorte2026");

        assertThat(hasher.matches("granjanorte2026", hash)).isFalse();
        assertThat(hasher.matches("PosturaAviario2027", hash)).isFalse();
        assertThat(hasher.matches("", hash)).isFalse();
    }

    @Test
    @DisplayName("uses a per-password salt so the same password yields different hashes")
    void usesPerPasswordSalt() {
        // E o que falta no legado, que grava SHA-2 nu: la, duas pessoas com a mesma senha
        // tem exatamente o mesmo hash, e uma tabela pre-calculada quebra as duas de uma vez.
        PasswordHash first = hasher.hash("GranjaNorte2026");
        PasswordHash second = hasher.hash("GranjaNorte2026");

        assertThat(first.value()).isNotEqualTo(second.value());
        assertThat(hasher.matches("GranjaNorte2026", first)).isTrue();
        assertThat(hasher.matches("GranjaNorte2026", second)).isTrue();
    }

    @Test
    @DisplayName("tolerates null input")
    void toleratesNullInput() {
        PasswordHash hash = hasher.hash("GranjaNorte2026");

        assertThat(hasher.matches(null, hash)).isFalse();
        assertThat(hasher.matches("GranjaNorte2026", null)).isFalse();
    }

    @Test
    @DisplayName("still verifies bcrypt hashes kept registered as a migration path")
    void stillVerifiesBcryptHashes() {
        // BCrypt nao e usado para gravar, mas permanece registrado no DelegatingPasswordEncoder:
        // e o que permitiria trocar de algoritmo no futuro sem invalidar senha existente.
        String bcryptHash = "{bcrypt}" + new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4)
                .encode("GranjaNorte2026");

        assertThat(hasher.matches("GranjaNorte2026", PasswordHash.of(bcryptHash))).isTrue();
    }
}
