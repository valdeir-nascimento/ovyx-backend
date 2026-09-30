package io.github.ovyx.shared.application.spreadsheet;

/**
 * O formato do Excel de uma celula numerica (R-010 da 007). O valor chega arredondado como na tela; o
 * formato so diz como o Excel o mostra, com a virgula decimal e o ponto de milhar do portugues.
 */
public enum CellFormat {
    /** Ovos, aves, classes, mortes: inteiro com milhar. */
    COUNT,
    /** Peso medio e consumo por ave, em gramas com uma casa. */
    GRAMS,
    /** Consumo de racao, em quilos com uma casa, como a aba Racao (004). */
    KILOGRAMS,
    /** Custo de racao e preco, em reais com duas casas. */
    MONEY,
    /** Custo por ovo, em reais com tres casas. */
    MONEY_3,
    /** Produtividade e taxas, em porcentagem com duas casas; gravada como fracao. */
    PERCENT_2,
    /** Classificacao e variacao, em porcentagem com uma casa; gravada como fracao. */
    PERCENT_1,
    /** Variacao da produtividade, em pontos percentuais com duas casas e o sinal. */
    POINTS
}
