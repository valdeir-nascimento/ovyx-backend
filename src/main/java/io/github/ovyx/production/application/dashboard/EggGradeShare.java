package io.github.ovyx.production.application.dashboard;

import java.math.BigDecimal;

/**
 * Uma classe de ovos do periodo, com a quantidade e a porcentagem sobre os coletados, com uma casa.
 *
 * @param grade {@code standard} ou o nome da classe em {@code EggGrades} ({@code small}, {@code jumbo},
 *     {@code dirty}, {@code cracked}, {@code bloodSpot}, {@code abnormal})
 */
public record EggGradeShare(String grade, int count, BigDecimal percent) {}
