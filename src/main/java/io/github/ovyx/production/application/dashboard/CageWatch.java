package io.github.ovyx.production.application.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma gaiola ativa do setor, com os numeros crus dos alertas (R-007 da 006).
 *
 * @param code a bateria, o hifen e o numero com ao menos dois digitos ("B-07")
 * @param removedToday as mortes mais os descartes lancados hoje
 * @param recentEggs os ovos dos ultimos relatorios em que a producao da gaiola foi lancada, ate 3
 * @param recentBirds as aves da gaiola nesses relatorios, somadas
 * @param recentReports quantos desses relatorios existem, ate 3
 * @param lastWeighedOn o dia da ultima pesagem valida; {@code null} sem pesagem
 * @param lastWeight o peso medio da ultima pesagem valida; {@code null} sem pesagem
 */
public record CageWatch(
        UUID cageId,
        String code,
        int removedToday,
        int recentEggs,
        int recentBirds,
        int recentReports,
        LocalDate lastWeighedOn,
        BigDecimal lastWeight) {}
