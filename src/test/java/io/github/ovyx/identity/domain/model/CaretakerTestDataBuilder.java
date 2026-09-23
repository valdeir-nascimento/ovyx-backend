package io.github.ovyx.identity.domain.model;

import io.github.ovyx.identity.domain.FakePasswordHasher;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Test Data Builder do agregado {@code Caretaker}.
 *
 * <p>Todo valor padrao e valido: o teste sobrescreve so o que importa para o cenario, e o resto fica
 * escondido de proposito. {@code aCaretaker().withCpf("12345678901").build()} diz que o CPF e a
 * unica coisa em jogo — o que a chamada direta a {@code Caretaker.register}, com nove argumentos
 * posicionais, obrigava o leitor a descobrir conferindo cada posicao.
 *
 * <p>Ha duas portas de entrada. {@link #aCaretaker()} tem dados fixos, para os testes unitarios.
 * {@link #aUniqueCaretaker()} gera e-mail, celular e CPF novos a cada chamada, para os testes de
 * integracao: o banco deles e compartilhado por todas as classes, e um responsavel fixo reutilizado
 * por testes que trocam senha ou inativam a conta deixaria o resultado dependente da ordem.
 */
public final class CaretakerTestDataBuilder {

    /** Senha padrao, dentro da politica. Exposta para os testes que precisam entrar com ela. */
    public static final String DEFAULT_PASSWORD = "GranjaNorte2026";

    private String fullName = "Maria Silva";
    private String cpf = "52998224725";
    private String email = "maria.silva@ovyx.com.br";
    private String mobilePhone = "91988887777";
    private String password = DEFAULT_PASSWORD;
    private Role role = Role.USER;
    private boolean mustChangePassword = false;
    private PasswordHasher hasher = new FakePasswordHasher();
    private Clock clock = FixedClock.at("2026-09-19T12:00:00Z");

    private CaretakerTestDataBuilder() {}

    /** Responsavel valido, com dados fixos. */
    public static CaretakerTestDataBuilder aCaretaker() {
        return new CaretakerTestDataBuilder();
    }

    /** Responsavel valido, com e-mail, celular e CPF que nenhum outro teste usa. */
    public static CaretakerTestDataBuilder aUniqueCaretaker() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return new CaretakerTestDataBuilder()
                .withFullName("Responsável de Teste " + suffix)
                .withCpf(randomValidCpf())
                .withEmail("teste." + suffix + "@ovyx.com.br")
                .withMobilePhone(randomMobilePhone());
    }

    public CaretakerTestDataBuilder withFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public CaretakerTestDataBuilder withCpf(String cpf) {
        this.cpf = cpf;
        return this;
    }

    public CaretakerTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public CaretakerTestDataBuilder withMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
        return this;
    }

    public CaretakerTestDataBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public CaretakerTestDataBuilder withRole(Role role) {
        this.role = role;
        return this;
    }

    /** Senha provisoria: o responsavel so pode trocar a senha, sair e se identificar (FR-025). */
    public CaretakerTestDataBuilder withPendingPasswordChange() {
        this.mustChangePassword = true;
        return this;
    }

    public CaretakerTestDataBuilder withHasher(PasswordHasher hasher) {
        this.hasher = hasher;
        return this;
    }

    public CaretakerTestDataBuilder withClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    /**
     * Cadastra pelo proprio agregado, e nao por atalho: um responsavel invalido recusa aqui, como
     * recusaria em producao.
     */
    public Caretaker build() {
        return Caretaker.register(fullName, cpf, email, mobilePhone, password, role, mustChangePassword, hasher, clock);
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
