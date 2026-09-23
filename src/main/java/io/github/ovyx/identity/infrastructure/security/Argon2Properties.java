package io.github.ovyx.identity.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parametros do Argon2id, configuraveis por ambiente.
 *
 * <p>Ficam fora do codigo porque o custo certo depende da maquina: o alvo e que um hash leve entre
 * 200 ms e 500 ms na maquina de producao (calibragem em T122). Em teste os valores sao reduzidos,
 * porque o custo deliberado tornaria a suite lenta sem acrescentar cobertura.
 *
 * <p>Os padroes abaixo seguem a recomendacao do OWASP para Argon2id.
 *
 * @param saltLength  bytes de salt por senha
 * @param hashLength  bytes do hash resultante
 * @param parallelism numero de faixas paralelas
 * @param memoryKb    memoria em KiB; e o parametro que mais encarece ataque com hardware dedicado
 * @param iterations  numero de passagens
 */
@ConfigurationProperties(prefix = "ovyx.security.argon2")
public record Argon2Properties(
        @DefaultValue("16") int saltLength,
        @DefaultValue("32") int hashLength,
        @DefaultValue("1") int parallelism,
        @DefaultValue("65536") int memoryKb,
        @DefaultValue("3") int iterations) {}
