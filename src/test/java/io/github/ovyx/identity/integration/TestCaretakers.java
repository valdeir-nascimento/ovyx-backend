package io.github.ovyx.identity.integration;

import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cria responsaveis de apoio para os testes de integracao.
 *
 * <p>Cada chamada gera um responsavel novo, com e-mail, celular e CPF proprios. O banco dos testes e
 * compartilhado por todas as classes: um responsavel fixo, reutilizado por testes que trocam senha
 * ou inativam a conta, deixaria o resultado dependente da ordem de execucao.
 */
public final class TestCaretakers {

    private TestCaretakers() {}

    public static Caretaker register(
            CaretakerRepository repository, PasswordHasher hasher, Clock clock, Role role, String password) {
        return register(repository, hasher, clock, role, password, false);
    }

    /** Variante para quem precisa de um responsavel com a troca de senha pendente (FR-025). */
    public static Caretaker register(
            CaretakerRepository repository,
            PasswordHasher hasher,
            Clock clock,
            Role role,
            String password,
            boolean mustChangePassword) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Caretaker caretaker = Caretaker.register(
                "Responsável de Teste " + suffix,
                randomValidCpf(),
                "teste." + suffix + "@ovyx.com.br",
                randomMobilePhone(),
                password,
                role,
                mustChangePassword,
                hasher,
                clock);

        repository.save(caretaker);
        return caretaker;
    }

    /** CPF aleatorio com digitos verificadores corretos. */
    public static String randomValidCpf() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int[] digits = new int[11];
        do {
            for (int i = 0; i < 9; i++) {
                digits[i] = random.nextInt(10);
            }
        } while (allEqual(digits));

        digits[9] = checkDigit(digits, 9);
        digits[10] = checkDigit(digits, 10);

        StringBuilder cpf = new StringBuilder(11);
        for (int digit : digits) {
            cpf.append(digit);
        }
        return cpf.toString();
    }

    private static int checkDigit(int[] digits, int position) {
        int sum = 0;
        int weight = position + 1;
        for (int i = 0; i < position; i++) {
            sum += digits[i] * weight--;
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static boolean allEqual(int[] digits) {
        for (int i = 1; i < 9; i++) {
            if (digits[i] != digits[0]) {
                return false;
            }
        }
        return true;
    }

    /** Celular aleatorio com DDD 91 e 11 digitos. */
    private static String randomMobilePhone() {
        return "919" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }
}
