package io.github.ovyx.production.domain.model;

/**
 * Uma gaiola ativa do setor, como o production precisa dela para abrir o relatorio (R-004).
 *
 * @param id a gaiola no farm
 * @param battery a bateria, ja em maiusculas
 * @param number o numero na bateria
 * @param birdCount as aves da gaiola agora; o relatorio as fixa na abertura (R-003)
 */
public record FarmCage(CageId id, String battery, int number, int birdCount) {}
