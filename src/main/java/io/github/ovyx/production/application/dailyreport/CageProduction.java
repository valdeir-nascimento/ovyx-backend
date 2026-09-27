package io.github.ovyx.production.application.dailyreport;

/** A producao lancada numa gaiola do relatorio. */
public record CageProduction(int eggs, int small, int jumbo, int dirty, int cracked, int bloodSpot, int abnormal) {}
