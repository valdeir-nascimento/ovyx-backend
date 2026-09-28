package io.github.ovyx.production.application.dashboard;

/** Os quatro indicadores do painel: a producao, a produtividade, o custo de racao e o custo por ovo. */
public record Indicators(Indicator production, Indicator layingRate, Indicator feedCost, Indicator costPerEgg) {}
