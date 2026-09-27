package io.github.ovyx.architecture.sample.domain;

/** Interface do dominio cuja implementacao recusa, para o autoteste da suite de arquitetura. */
public interface RegistrationPolicy {

    String check(String raw);
}
