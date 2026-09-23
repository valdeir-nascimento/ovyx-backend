package io.github.ovyx.identity.infrastructure.security;

import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.identity.domain.valueobject.PasswordHash;

import java.util.Map;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adaptador de {@link PasswordHasher} sobre o Spring Security.
 *
 * <p>Usa {@link DelegatingPasswordEncoder} com Argon2id como padrao. O encoder grava o algoritmo
 * como prefixo no proprio hash ({@code {argon2}...}), o que permite trocar de algoritmo no futuro
 * sem invalidar as senhas existentes. BCrypt fica <strong>registrado</strong>, mas nao e usado para
 * gravar: serve para que um hash bcrypt eventual continue verificavel sem mudanca de codigo.
 *
 * <p>Nenhum algoritmo do legado esta registrado aqui. FR-024 proibe: o hash SHA-2 sem salt do sistema
 * antigo nao entra no sistema novo, e por isso nenhum responsavel do legado foi importado.
 */
@Component
public class Argon2PasswordHasher implements PasswordHasher {

    private static final String ARGON2 = "argon2";
    private static final String BCRYPT = "bcrypt";
    private static final int BCRYPT_STRENGTH = 12;

    private final PasswordEncoder encoder;

    public Argon2PasswordHasher(Argon2Properties properties) {
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(
            properties.saltLength(),
            properties.hashLength(),
            properties.parallelism(),
            properties.memoryKb(),
            properties.iterations()
        );

        this.encoder = new DelegatingPasswordEncoder(ARGON2, Map.of(ARGON2, argon2, BCRYPT, new BCryptPasswordEncoder(BCRYPT_STRENGTH)));
    }

    @Override
    public PasswordHash hash(String rawPassword) {
        return PasswordHash.of(encoder.encode(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
        if (rawPassword == null || hash == null) {
            return false;
        }
        return encoder.matches(rawPassword, hash.value());
    }

}
