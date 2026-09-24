package io.github.ovyx.identity.application.caretaker;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.shared.application.PageResponse;
import java.util.Optional;

/**
 * Porta de leitura da administracao de responsaveis.
 *
 * <p>Fica em {@code application}, e nao em {@code domain}: consulta para tela nao e conceito de
 * dominio. O lado de escrita tem porta propria ({@code CaretakerRepository}), que devolve
 * agregados; esta devolve modelos de leitura montados direto da consulta (principio V).
 */
public interface CaretakerDirectory {

    /**
     * Pesquisa por trecho do nome, sem distinguir maiusculas de minusculas, em ordem alfabetica.
     *
     * @param nameFragment trecho ja aparado; {@code null} para nao filtrar pelo nome
     * @param status situacao; {@code null} para as duas
     */
    PageResponse<CaretakerSummary> search(String nameFragment, CaretakerStatus status, int page, int size);

    Optional<CaretakerDetail> findDetail(CaretakerId id);
}
