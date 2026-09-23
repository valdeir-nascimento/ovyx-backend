package io.github.ovyx;

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

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
