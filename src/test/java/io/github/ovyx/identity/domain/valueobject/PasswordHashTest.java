package io.github.ovyx.identity.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Testes do objeto de valor que guarda a senha protegida. */
@DisplayName("PasswordHash")
class PasswordHashTest {

    @Test
    @DisplayName("accepts a hash with an algorithm prefix")
    void givenHashWithAlgorithmPrefix_whenCreating_thenExposeTheAlgorithm() {
        // given
        String prefixed = "{argon2}$argon2id$v=19$m=65536,t=3,p=1$abc$def";

        // when
        PasswordHash hash = PasswordHash.of(prefixed);

        // then
        assertThat(hash.algorithm()).isEqualTo("argon2");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"  "})
    @DisplayName("rejects a blank hash")
    void givenBlankHash_whenCreating_thenRejectIt(String raw) {
        // given — raw from @NullSource and @ValueSource

        // when
        ThrowingCallable creation = () -> PasswordHash.of(raw);

        // then
        assertThatThrownBy(creation).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"5e884898da28047151d0e56f8dc6292773603d0d", "{}vazio"})
    @DisplayName("rejects a hash without an algorithm prefix")
    void givenHashWithoutAlgorithmPrefix_whenCreating_thenRejectIt(String raw) {
        // given — raw from @ValueSource
        // O primeiro e exatamente o formato do legado: SHA-2 nu, sem identificacao de algoritmo, sem
        // caminho de migracao possivel.

        // when
        ThrowingCallable creation = () -> PasswordHash.of(raw);

        // then
        assertThatThrownBy(creation).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("never exposes the hash in toString")
    void givenStoredHash_whenPrintingIt_thenShowOnlyAMask() {
        // given
        PasswordHash hash = PasswordHash.of("{argon2}conteudo-secreto");

        // when
        String printed = hash.toString();

        // then
        assertThat(printed).isEqualTo("****").doesNotContain("conteudo-secreto");
    }

    @Test
    @DisplayName("is equal to another hash with the same content")
    void givenTwoHashesWithTheSameContent_whenComparing_thenTreatThemAsEqual() {
        // given
        PasswordHash stored = PasswordHash.of("{argon2}abc");

        // when
        PasswordHash sameContent = PasswordHash.of("{argon2}abc");

        // then
        assertThat(stored).isEqualTo(sameContent);
    }

    @Test
    @DisplayName("differs from a hash with different content")
    void givenTwoHashesWithDifferentContent_whenComparing_thenTreatThemAsDifferent() {
        // given
        PasswordHash stored = PasswordHash.of("{argon2}abc");

        // when
        PasswordHash otherContent = PasswordHash.of("{argon2}xyz");

        // then
        assertThat(stored).isNotEqualTo(otherContent);
    }
}
