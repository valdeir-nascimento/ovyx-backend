package io.github.ovyx.identity.application.recovery;

import static io.github.ovyx.identity.application.recovery.RecoveryFakes.FIRST_TOKEN;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.application.RecordingAccessEventRecorder;
import io.github.ovyx.identity.application.recovery.RecoveryFakes.RecordingIdentityMailer;
import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.model.AccessOutcome;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.fixtures.InMemoryCaretakerRepository;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O envio do e-mail do link (US1 da 012), depois que a emissão confirmou: o link já vale quando o e-mail sai, e a
 * emissão repetida pelo despachante não manda o e-mail duas vezes.
 */
@DisplayName("SendRecoveryLinkCommandHandler")
class SendRecoveryLinkCommandHandlerTest {

    private static final String ORIGIN = "203.0.113.42";
    private static final String EMAIL = "marina.costa@ovyx.com.br";

    private final InMemoryCaretakerRepository repository = new InMemoryCaretakerRepository();
    private final RecordingAccessEventRecorder recorder = new RecordingAccessEventRecorder();
    private final RecordingIdentityMailer mailer = new RecordingIdentityMailer();
    private final FixedClock clock = FixedClock.at("2026-10-02T12:00:00Z");
    private final SendRecoveryLinkCommandHandler handler =
            new SendRecoveryLinkCommandHandler(repository, mailer, recorder, clock);

    private Caretaker withLink() {
        Caretaker marina = aCaretaker().withFullName("Marina Costa").withEmail(EMAIL).build();
        marina.issuePasswordRecovery(FIRST_TOKEN, clock);
        repository.save(marina);
        return marina;
    }

    @Test
    @DisplayName("sends the link to the email of the account, with the name and the expiry, and records it")
    void givenIssuedLink_whenSending_thenMailTheAccountAndRecordTheLinkSent() {
        // given
        Caretaker marina = withLink();

        // when
        Result<Void> result = handler.handle(new SendRecoveryLinkCommand(marina.id(), FIRST_TOKEN, ORIGIN));

        // then
        assertThat(result.isFailure()).isFalse();
        RecoveryLinkMail mail = mailer.links().getFirst();
        assertThat(mail.to()).isEqualTo(EMAIL);
        assertThat(mail.fullName()).isEqualTo("Marina Costa");
        assertThat(mail.token()).isEqualTo(FIRST_TOKEN);
        assertThat(mail.expiresAt()).isEqualTo(marina.passwordRecovery().expiresAt());
        AccessEvent event = recorder.recorded().getFirst();
        assertThat(event.outcome()).isEqualTo(AccessOutcome.RECOVERY_LINK_SENT);
        assertThat(event.attemptedIdentifier()).isEqualTo(EMAIL);
        assertThat(event.caretakerId()).isEqualTo(marina.id());
        assertThat(event.origin()).isEqualTo(ORIGIN);
    }

    @Test
    @DisplayName("records the failure of the mail server and keeps the link issued")
    void givenMailServerThatRefuses_whenSending_thenRecordTheDeliveryFailureAndKeepTheLink() {
        // given
        Caretaker marina = withLink();
        mailer.refuse();

        // when
        Result<Void> result = handler.handle(new SendRecoveryLinkCommand(marina.id(), FIRST_TOKEN, ORIGIN));

        // then
        assertThat(result.isFailure()).isFalse();
        assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.RECOVERY_DELIVERY_FAILED);
        assertThat(repository.findById(marina.id()).orElseThrow().passwordRecovery()).isNotNull();
    }

    @Test
    @DisplayName("sends nothing when the caretaker no longer exists")
    void givenCaretakerThatNoLongerExists_whenSending_thenSendNothing() {
        // given
        CaretakerId nobody = CaretakerId.generate();

        // when
        Result<Void> result = handler.handle(new SendRecoveryLinkCommand(nobody, FIRST_TOKEN, ORIGIN));

        // then
        assertThat(result.isFailure()).isFalse();
        assertThat(mailer.links()).isEmpty();
    }

    @Test
    @DisplayName("never shows the code of the link in the text form of the command")
    void givenCommand_whenPrinting_thenHideTheCode() {
        // given
        SendRecoveryLinkCommand command = new SendRecoveryLinkCommand(CaretakerId.generate(), FIRST_TOKEN, ORIGIN);

        // when / then
        assertThat(command.toString()).doesNotContain(FIRST_TOKEN.value());
    }

    @Test
    @DisplayName("sends nothing when the link was annulled between the issue and the email")
    void givenLinkAnnulledBeforeTheEmail_whenSending_thenSendNothing() {
        // given
        Caretaker marina = withLink();
        marina.changeOwnPassword(
                io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.DEFAULT_PASSWORD,
                "PosturaAviario2027",
                new io.github.ovyx.identity.domain.FakePasswordHasher(),
                clock);
        repository.save(marina);

        // when
        Result<Void> result = handler.handle(new SendRecoveryLinkCommand(marina.id(), FIRST_TOKEN, ORIGIN));

        // then
        assertThat(result.isFailure()).isFalse();
        assertThat(mailer.links()).isEmpty();
        assertThat(recorder.recorded()).isEmpty();
    }

    @Test
    @DisplayName("sends nothing for a link already replaced by a newer request")
    void givenLinkReplacedByANewerOne_whenSendingTheOlder_thenSendNothing() {
        // given
        Caretaker marina = withLink();
        marina.issuePasswordRecovery(RecoveryFakes.SECOND_TOKEN, clock);
        repository.save(marina);

        // when
        Result<Void> result = handler.handle(new SendRecoveryLinkCommand(marina.id(), FIRST_TOKEN, ORIGIN));

        // then
        assertThat(result.isFailure()).isFalse();
        assertThat(mailer.links()).isEmpty();
    }
}
