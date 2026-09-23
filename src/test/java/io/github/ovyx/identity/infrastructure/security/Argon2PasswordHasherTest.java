package io.github.ovyx.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Testes da protecao de senha (FR-021).
 *
 * <p>Parametros reduzidos de proposito: o custo real do Argon2id e calibrado em T122 para a maquina
 * de producao, e aqui so atrasaria a suite.
 */
@DisplayName("Argon2PasswordHasher")
class Argon2PasswordHasherTest {

    private static final Argon2Properties FAST_FOR_TESTS = new Argon2Properties(16, 32, 1, 1024, 1);
    private static final String PASSWORD = "GranjaNorte2026";

    private final Argon2PasswordHasher hasher = new Argon2PasswordHasher(FAST_FOR_TESTS);

    @Test
    @DisplayName("writes the hash with the algorithm prefix")
    void givenRawPassword_whenHashing_thenPrefixTheAlgorithm() {
        // given
        String raw = PASSWORD;

        // when
        PasswordHash hash = hasher.hash(raw);

        // then
        assertThat(hash.value()).startsWith("{argon2}");
        assertThat(hash.algorithm()).isEqualTo("argon2");
    }

    @Test
    @DisplayName("never stores the raw password")
    void givenRawPassword_whenHashing_thenNeverKeepItInTheHash() {
        // given
        String raw = PASSWORD;

        // when
        PasswordHash hash = hasher.hash(raw);

        // then
        assertThat(hash.value()).doesNotContain(PASSWORD);
    }

    @Test
    @DisplayName("accepts the correct password")
    void givenCorrectPassword_whenMatching_thenAccept() {
        // given
        PasswordHash hash = hasher.hash(PASSWORD);

        // when
        boolean matches = hasher.matches(PASSWORD, hash);

        // then
        assertThat(matches).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"granjanorte2026", "PosturaAviario2027", ""})
    @DisplayName("rejects a wrong password")
    void givenWrongPassword_whenMatching_thenRefuse(String wrong) {
        // given
        PasswordHash hash = hasher.hash(PASSWORD);

        // when
        boolean matches = hasher.matches(wrong, hash);

        // then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("uses a per-password salt so the same password yields different hashes")
    void givenTheSamePasswordHashedTwice_whenComparing_thenProduceDifferentHashesThatBothMatch() {
        // given
        // E o que falta no legado, que grava SHA-2 nu: la, duas pessoas com a mesma senha
        // tem exatamente o mesmo hash, e uma tabela pre-calculada quebra as duas de uma vez.
        String raw = PASSWORD;

        // when
        PasswordHash first = hasher.hash(raw);
        PasswordHash second = hasher.hash(raw);

        // then
        assertThat(first.value()).isNotEqualTo(second.value());
        assertThat(hasher.matches(PASSWORD, first)).isTrue();
        assertThat(hasher.matches(PASSWORD, second)).isTrue();
    }

    @Test
    @DisplayName("refuses a null password instead of throwing")
    void givenNullPassword_whenMatching_thenRefuseWithoutThrowing() {
        // given
        PasswordHash hash = hasher.hash(PASSWORD);

        // when
        boolean matches = hasher.matches(null, hash);

        // then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("refuses a null hash instead of throwing")
    void givenNullHash_whenMatching_thenRefuseWithoutThrowing() {
        // given
        PasswordHash missing = null;

        // when
        boolean matches = hasher.matches(PASSWORD, missing);

        // then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("still verifies bcrypt hashes kept registered as a migration path")
    void givenBcryptHash_whenMatching_thenStillVerifyIt() {
        // given
        // BCrypt nao e usado para gravar, mas permanece registrado no DelegatingPasswordEncoder:
        // e o que permitiria trocar de algoritmo no futuro sem invalidar senha existente.
        PasswordHash bcryptHash = PasswordHash.of("{bcrypt}" + new BCryptPasswordEncoder(4).encode(PASSWORD));

        // when
        boolean matches = hasher.matches(PASSWORD, bcryptHash);

        // then
        assertThat(matches).isTrue();
    }
}
