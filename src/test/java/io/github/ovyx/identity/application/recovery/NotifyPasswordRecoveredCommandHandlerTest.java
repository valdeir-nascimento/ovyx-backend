package io.github.ovyx.identity.application.recovery;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.application.recovery.RecoveryFakes.RecordingIdentityMailer;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.fixtures.InMemoryCaretakerRepository;
import io.github.ovyx.shared.application.Result;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O aviso da redefinição por e-mail (FR-012 da 012), depois que a redefinição confirmou. */
@DisplayName("NotifyPasswordRecoveredCommandHandler")
class NotifyPasswordRecoveredCommandHandlerTest {

    private static final Instant RECOVERED_AT = Instant.parse("2026-10-02T12:05:00Z");

    private final InMemoryCaretakerRepository repository = new InMemoryCaretakerRepository();
    private final RecordingIdentityMailer mailer = new RecordingIdentityMailer();
    private final NotifyPasswordRecoveredCommandHandler handler =
            new NotifyPasswordRecoveredCommandHandler(repository, mailer);

    @Test
    @DisplayName("mails the notice to the account, with the name and the instant of the reset")
    void givenRecoveredPassword_whenNotifying_thenMailTheAccountWithTheNameAndTheInstant() {
        // given
        Caretaker marina = aCaretaker().withFullName("Marina Costa").withEmail("marina.costa@ovyx.com.br").build();
        repository.save(marina);

        // when
        Result<Void> result = handler.handle(new NotifyPasswordRecoveredCommand(marina.id(), RECOVERED_AT));

        // then
        assertThat(result.isFailure()).isFalse();
        PasswordRecoveredMail mail = mailer.notices().getFirst();
        assertThat(mail.to()).isEqualTo("marina.costa@ovyx.com.br");
        assertThat(mail.fullName()).isEqualTo("Marina Costa");
        assertThat(mail.recoveredAt()).isEqualTo(RECOVERED_AT);
    }

    @Test
    @DisplayName("mails nothing when the caretaker no longer exists")
    void givenCaretakerThatNoLongerExists_whenNotifying_thenMailNothing() {
        // given
        CaretakerId nobody = CaretakerId.generate();

        // when
        Result<Void> result = handler.handle(new NotifyPasswordRecoveredCommand(nobody, RECOVERED_AT));

        // then
        assertThat(result.isFailure()).isFalse();
        assertThat(mailer.notices()).isEmpty();
    }
}
