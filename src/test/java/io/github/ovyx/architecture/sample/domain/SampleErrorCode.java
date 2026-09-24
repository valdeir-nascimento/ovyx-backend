package io.github.ovyx.architecture.sample.domain;

import io.github.ovyx.shared.domain.ErrorCode;

/** Codigos do dominio de amostra do autoteste da suite de arquitetura. */
public enum SampleErrorCode implements ErrorCode {
    VALIDATION_FAILED,
    VALUE_REQUIRED;

    @Override
    public String code() {
        return name();
    }
}
