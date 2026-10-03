package io.github.ovyx;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base dos testes de integracao: sobe um PostgreSQL 18.6 real e aplica as migracoes Flyway.
 *
 * <p>O principio VI da constituicao proibe banco em memoria fingindo ser PostgreSQL. H2 em modo de
 * compatibilidade nao reproduz indice parcial, {@code timestamptz} nem o comportamento
 * transacional do PostgreSQL — justamente onde moram os erros que so apareceriam em producao.
 *
 * <p>O contentor e um singleton de JVM: sobe uma vez e e compartilhado por todas as classes de
 * teste. Nao e parado explicitamente — o Ryuk, contentor sentinela do Testcontainers, remove tudo
 * ao fim da execucao.
 *
 * <p>O servidor SMTP tambem e um singleton: o GreenMail, em processo, numa porta livre (feature
 * 012). Ele recebe de verdade os e-mails que a aplicacao envia, e cada teste que os confere limpa
 * a caixa antes, com {@code MAIL.purgeEmailFromAllMailboxes()}.
 *
 * <p>Nota de versao: o Spring Boot 4.1.1 gerencia Testcontainers 2.0.5, cuja classe vive em
 * {@code org.testcontainers.postgresql} — e nao em {@code org.testcontainers.containers}, como na
 * linha 1.x.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18.6"))
                    .withDatabaseName("ovyx")
                    .withUsername("ovyx")
                    .withPassword("ovyx");

    protected static final GreenMail MAIL = new GreenMail(ServerSetupTest.SMTP.dynamicPort());

    /** O remetente e o endereco da aplicacao que os e-mails dos testes trazem. */
    protected static final String MAIL_FROM = "ovyx@ovyx.test";

    protected static final String APP_URL = "http://localhost:4200";

    static {
        POSTGRES.start();
        MAIL.start();
    }

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @DynamicPropertySource
    static void registerMail(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> MAIL.getSmtp().getPort());
        registry.add("spring.mail.username", () -> "");
        registry.add("spring.mail.password", () -> "");
        registry.add("ovyx.mail.from", () -> MAIL_FROM);
        registry.add("ovyx.mail.app-url", () -> APP_URL);
    }
}
