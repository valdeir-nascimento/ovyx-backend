package io.github.ovyx.identity.application.recovery;

import io.github.ovyx.identity.domain.port.RecoveryTokens;
import io.github.ovyx.identity.domain.valueobject.RecoveryToken;
import io.github.ovyx.shared.application.Command;
import io.github.ovyx.shared.application.DeferredCommands;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Dublês dos testes da recuperação de senha (feature 012), que guardam o que receberam para o teste inspecionar. */
final class RecoveryFakes {

    static final RecoveryToken FIRST_TOKEN = RecoveryToken.of("3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx");
    static final RecoveryToken SECOND_TOKEN = RecoveryToken.of("Zz9-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx");

    private RecoveryFakes() {}

    /** Os comandos adiados, guardados em vez de executados. */
    static final class RecordingDeferredCommands implements DeferredCommands {
        private final List<Command<?>> submitted = new ArrayList<>();

        @Override
        public void submit(Command<?> command) {
            submitted.add(command);
        }

        List<Command<?>> submitted() {
            return List.copyOf(submitted);
        }
    }

    /** O carteiro, que aceita ou recusa conforme o teste mandar, e guarda o que recebeu. */
    static final class RecordingIdentityMailer implements IdentityMailer {
        private final List<RecoveryLinkMail> links = new ArrayList<>();
        private final List<PasswordRecoveredMail> notices = new ArrayList<>();
        private boolean accepting = true;

        void refuse() {
            accepting = false;
        }

        @Override
        public boolean sendRecoveryLink(RecoveryLinkMail mail) {
            links.add(mail);
            return accepting;
        }

        @Override
        public boolean sendPasswordRecoveredNotice(PasswordRecoveredMail mail) {
            notices.add(mail);
            return accepting;
        }

        List<RecoveryLinkMail> links() {
            return List.copyOf(links);
        }

        List<PasswordRecoveredMail> notices() {
            return List.copyOf(notices);
        }
    }

    /** A contenção por origem, que bloqueia quando o teste mandar e conta as tentativas registradas. */
    static final class RecordingRecoveryThrottle implements io.github.ovyx.identity.domain.port.RecoveryThrottle {
        private final List<String> attempts = new ArrayList<>();
        private boolean blocked;

        void block() {
            blocked = true;
        }

        @Override
        public boolean isBlocked(String origin) {
            return blocked;
        }

        @Override
        public void registerAttempt(String origin) {
            attempts.add(origin);
        }

        List<String> attempts() {
            return List.copyOf(attempts);
        }
    }

    /** O gerador de códigos, que entrega os códigos dados, na ordem. */
    static final class SequenceRecoveryTokens implements RecoveryTokens {
        private final Deque<RecoveryToken> next = new ArrayDeque<>();

        SequenceRecoveryTokens(RecoveryToken... tokens) {
            next.addAll(List.of(tokens));
        }

        @Override
        public RecoveryToken issue() {
            return next.removeFirst();
        }
    }
}
