package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.application.common.StatusFilter;
import java.time.LocalDate;

/**
 * Os filtros da lista de gaiolas, iguais na pagina e na exportacao.
 *
 * @param code trecho do codigo, sem distinguir maiusculas; {@code null} nao filtra
 * @param battery a bateria exata, em maiusculas; {@code null} nao filtra
 * @param status a situacao das gaiolas
 * @param pendingSince com o filtro "Pesagem pendente", o primeiro dia em que uma pesagem conta para a semana: so
 *     entram as gaiolas ativas sem pesagem valida desde ele (feature 010); {@code null} nao filtra
 */
public record CageFilter(String code, String battery, StatusFilter status, LocalDate pendingSince) {}
