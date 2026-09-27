package io.github.ovyx.farm.domain.model;

/**
 * A situacao da pesagem (FR-006 da 005): valida, ou anulada. A anulada nao e apagada: fica guardada, com quem
 * a anulou e quando, e sai das leituras.
 */
public enum WeighingStatus {
    VALID,
    VOIDED
}
