package io.github.ovyx.identity.domain.model;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalCodeOf;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.domain.FakePasswordHasher;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.identity.domain.valueobject.PasswordRecovery;
import io.github.ovyx.identity.domain.valueobject.RecoveryToken;
import io.github.ovyx.identity.fixtures.InMemoryCaretakerRepository;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A recuperacao da senha no agregado (feature 012), sem mock de dominio: a emissao do link, a redefinicao por ele, a
 * anulacao pelas outras trocas de senha e o limite de pedidos.
 */
@DisplayName("Caretaker password recovery")
class CaretakerRecoveryTest {

    private static final RecoveryToken FIRST = RecoveryToken.of("3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx");
    private static final RecoveryToken SECOND = RecoveryToken.of("Zz9-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx");

    private final PasswordHasher hasher = new FakePasswordHasher();
    private final FixedClock clock = FixedClock.at("2026-10-02T12:00:00Z");

    private CaretakerTestDataBuilder aValidCaretaker() {
        return aCaretaker().withHasher(hasher).withClock(clock);
    }

    // ---------------------------------------------------------------- emissão (US1)

    @Test
    @DisplayName("issues a link that keeps only the digest of the code and expires in thirty minutes")
    void givenActiveCaretaker_whenIssuingRecovery_thenKeepTheDigestAndExpireInThirtyMinutes() {
        // given
        Caretaker caretaker = aValidCaretaker().build();

        // when
        caretaker.issuePasswordRecovery(FIRST, clock);

        // then
        PasswordRecovery recovery = caretaker.passwordRecovery();
        assertThat(recovery.tokenHash()).isEqualTo(FIRST.hash());
        assertThat(recovery.expiresAt()).isEqualTo(Instant.parse("2026-10-02T12:30:00Z"));
    }

    @Test
    @DisplayName("replaces the pending link with the new one, so only the newest is valid")
    void givenPendingLink_whenIssuingAnother_thenOnlyTheNewestIsValid() {
        // given
        Caretaker caretaker = aValidCaretaker().build();
        caretaker.issuePasswordRecovery(FIRST, clock);
        clock.advance(Duration.ofMinutes(5));

        // when
        caretaker.issuePasswordRecovery(SECOND, clock);

        // then
        PasswordRecovery recovery = caretaker.passwordRecovery();
        assertThat(recovery.isValidFor(SECOND.hash(), clock.instant())).isTrue();
        assertThat(recovery.isValidFor(FIRST.hash(), clock.instant())).isFalse();
        assertThat(recovery.expiresAt()).isEqualTo(Instant.parse("2026-10-02T12:35:00Z"));
    }

    @Test
    @DisplayName("refuses to issue a link to an inactive caretaker, changing nothing")
    void givenInactiveCaretaker_whenIssuingRecovery_thenRefuseAsUnavailableAndChangeNothing() {
        // given
        Caretaker caretaker = aValidCaretaker().buildInactive();

        // when
        Object code = refusalCodeOf(() -> caretaker.issuePasswordRecovery(FIRST, clock));

        // then
        assertThat(code).isEqualTo(IdentityErrorCode.CARETAKER_UNAVAILABLE);
        assertThat(caretaker.passwordRecovery()).isNull();
    }

    @Test
    @DisplayName("does not change the instant of the last change of the registration")
    void givenActiveCaretaker_whenIssuingRecovery_thenKeepTheUpdatedAt() {
        // given
        Caretaker caretaker = aValidCaretaker().build();
        Instant registeredAt = caretaker.updatedAt();
        clock.advance(Duration.ofHours(1));

        // when
        caretaker.issuePasswordRecovery(FIRST, clock);

        // then
        assertThat(caretaker.updatedAt()).isEqualTo(registeredAt);
    }

    // ---------------------------------------------------------------- redefinição (US2)

    private static final String NEW_PASSWORD = "PosturaAviario2027";

    private Caretaker withLink() {
        Caretaker caretaker = aValidCaretaker().build();
        caretaker.issuePasswordRecovery(FIRST, clock);
        return caretaker;
    }

    @Test
    @DisplayName("recovers the password by the link: new hash, no provisional password, no link, next session generation")
    void givenValidLink_whenRecoveringThePassword_thenChangeItAndClearTheLinkAndRevokeTheSessions() {
        // given
        Caretaker caretaker = aValidCaretaker().withPendingPasswordChange().build();
        caretaker.issuePasswordRecovery(FIRST, clock);
        clock.advance(Duration.ofMinutes(10));

        // when
        caretaker.recoverPassword(FIRST.value(), NEW_PASSWORD, hasher, clock);

        // then
        assertThat(caretaker.authenticate(NEW_PASSWORD, hasher)).isTrue();
        assertThat(caretaker.mustChangePassword()).isFalse();
        assertThat(caretaker.passwordRecovery()).isNull();
        assertThat(caretaker.sessionGeneration()).isEqualTo(1);
        assertThat(caretaker.updatedAt()).isEqualTo(Instant.parse("2026-10-02T12:10:00Z"));
    }

    @Test
    @DisplayName("refuses to recover with the same link twice")
    void givenLinkAlreadyUsed_whenRecoveringAgain_thenRefuseAsInvalidLink() {
        // given
        Caretaker caretaker = withLink();
        caretaker.recoverPassword(FIRST.value(), NEW_PASSWORD, hasher, clock);

        // when
        Object code = refusalCodeOf(() -> caretaker.recoverPassword(FIRST.value(), "OutraSenha2028", hasher, clock));

        // then
        assertThat(code).isEqualTo(IdentityErrorCode.RECOVERY_LINK_INVALID);
        assertThat(caretaker.authenticate(NEW_PASSWORD, hasher)).isTrue();
        assertThat(caretaker.sessionGeneration()).isEqualTo(1);
    }

    @Test
    @DisplayName("refuses an expired link, changing nothing")
    void givenExpiredLink_whenRecovering_thenRefuseAsInvalidLinkAndChangeNothing() {
        // given
        Caretaker caretaker = withLink();
        clock.advance(Duration.ofMinutes(30));

        // when
        Object code = refusalCodeOf(() -> caretaker.recoverPassword(FIRST.value(), NEW_PASSWORD, hasher, clock));

        // then
        assertThat(code).isEqualTo(IdentityErrorCode.RECOVERY_LINK_INVALID);
        assertThat(caretaker.authenticate(NEW_PASSWORD, hasher)).isFalse();
        assertThat(caretaker.passwordRecovery()).isNotNull();
        assertThat(caretaker.sessionGeneration()).isZero();
    }

    @Test
    @DisplayName("refuses another code, a code outside the format and a caretaker without a link")
    void givenWrongCodeOrNoLink_whenRecovering_thenRefuseAsInvalidLink() {
        // given
        Caretaker withLink = withLink();
        Caretaker withoutLink = aValidCaretaker().build();

        // when / then
        assertThat(refusalCodeOf(() -> withLink.recoverPassword(SECOND.value(), NEW_PASSWORD, hasher, clock)))
                .isEqualTo(IdentityErrorCode.RECOVERY_LINK_INVALID);
        assertThat(refusalCodeOf(() -> withLink.recoverPassword("curto", NEW_PASSWORD, hasher, clock)))
                .isEqualTo(IdentityErrorCode.RECOVERY_LINK_INVALID);
        assertThat(refusalCodeOf(() -> withoutLink.recoverPassword(FIRST.value(), NEW_PASSWORD, hasher, clock)))
                .isEqualTo(IdentityErrorCode.RECOVERY_LINK_INVALID);
        assertThat(withLink.passwordRecovery()).isNotNull();
    }

    @Test
    @DisplayName("refuses the link of a caretaker who was deactivated after receiving it")
    void givenCaretakerDeactivatedAfterTheLink_whenRecovering_thenRefuseAsInvalidLink() {
        // given
        Caretaker active = withLink();
        Caretaker inactive = Caretaker.restore(
                active.id(),
                active.fullName(),
                active.cpf(),
                active.email(),
                active.mobilePhone(),
                active.passwordHash(),
                active.role(),
                CaretakerStatus.INACTIVE,
                active.mustChangePassword(),
                active.themePreference(),
                active.passwordRecovery(),
                active.recoveryAllowance(),
                active.sessionGeneration(),
                active.createdAt(),
                active.updatedAt());

        // when
        Object code = refusalCodeOf(() -> inactive.recoverPassword(FIRST.value(), NEW_PASSWORD, hasher, clock));

        // then
        assertThat(code).isEqualTo(IdentityErrorCode.RECOVERY_LINK_INVALID);
    }

    @Test
    @DisplayName("refuses a new password outside the policy with every violation at once, keeping the link valid")
    void givenPasswordOutsideThePolicy_whenRecovering_thenRefuseWithEveryViolationAndKeepTheLink() {
        // given
        Caretaker caretaker = withLink();

        // when
        Map<String, String> details = detailsOf(() -> caretaker.recoverPassword(FIRST.value(), "curta", hasher, clock));

        // then
        assertThat(details).containsOnlyKeys("newPassword");
        assertThat(details.get("newPassword"))
                .contains("ao menos 12 caracteres")
                .contains("ao menos um dígito");
        assertThat(caretaker.passwordRecovery().isValidFor(FIRST.hash(), clock.instant())).isTrue();
    }

    @Test
    @DisplayName("checks the link without changing anything")
    void givenValidLink_whenChecking_thenAcceptItAndChangeNothing() {
        // given
        Caretaker caretaker = withLink();

        // when
        caretaker.checkRecovery(FIRST.value(), clock);

        // then
        assertThat(caretaker.passwordRecovery().tokenHash()).isEqualTo(FIRST.hash());
        assertThat(caretaker.sessionGeneration()).isZero();
    }

    @Test
    @DisplayName("refuses to check another code or an expired link")
    void givenWrongOrExpiredLink_whenChecking_thenRefuseAsInvalidLink() {
        // given
        Caretaker caretaker = withLink();

        // when / then
        assertThat(refusalCodeOf(() -> caretaker.checkRecovery(SECOND.value(), clock)))
                .isEqualTo(IdentityErrorCode.RECOVERY_LINK_INVALID);
        clock.advance(Duration.ofMinutes(31));
        assertThat(refusalCodeOf(() -> caretaker.checkRecovery(FIRST.value(), clock)))
                .isEqualTo(IdentityErrorCode.RECOVERY_LINK_INVALID);
    }

    // ---------------------------------------------------------------- anulação (FR-007)

    @Test
    @DisplayName("clears the pending link when the caretaker changes the own password")
    void givenPendingLink_whenChangingTheOwnPassword_thenClearTheLink() {
        // given
        Caretaker caretaker = withLink();

        // when
        caretaker.changeOwnPassword(CaretakerTestDataBuilder.DEFAULT_PASSWORD, NEW_PASSWORD, hasher, clock);

        // then
        assertThat(caretaker.passwordRecovery()).isNull();
    }

    @Test
    @DisplayName("clears the pending link when the caretaker is deactivated")
    void givenPendingLink_whenDeactivating_thenClearTheLink() {
        // given
        Caretaker caretaker = withLink();

        // when
        caretaker.deactivate(new InMemoryCaretakerRepository(), clock);

        // then
        assertThat(caretaker.passwordRecovery()).isNull();
    }

    @Test
    @DisplayName("clears the pending link when the initial administrator is restored")
    void givenPendingLink_whenRestoringAsInitialAdministrator_thenClearTheLink() {
        // given
        Caretaker caretaker = withLink();

        // when
        caretaker.restoreAsInitialAdministrator(NEW_PASSWORD, hasher, new InMemoryCaretakerRepository(), clock);

        // then
        assertThat(caretaker.passwordRecovery()).isNull();
    }

    // ---------------------------------------------------------------- limite (US3)

    private static final RecoveryToken THIRD = RecoveryToken.of("Yy8-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx");
    private static final RecoveryToken FOURTH = RecoveryToken.of("Xx7-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx");

    @Test
    @DisplayName("refuses the fourth link in the same hour, keeping the third one valid")
    void givenThreeLinksInTheHour_whenIssuingTheFourth_thenRefuseAsLimitReachedAndKeepTheThird() {
        // given
        Caretaker caretaker = aValidCaretaker().build();
        caretaker.issuePasswordRecovery(FIRST, clock);
        clock.advance(Duration.ofMinutes(20));
        caretaker.issuePasswordRecovery(SECOND, clock);
        clock.advance(Duration.ofMinutes(20));
        caretaker.issuePasswordRecovery(THIRD, clock);
        clock.advance(Duration.ofMinutes(19));

        // when
        Object code = refusalCodeOf(() -> caretaker.issuePasswordRecovery(FOURTH, clock));

        // then
        assertThat(code).isEqualTo(IdentityErrorCode.RECOVERY_LIMIT_REACHED);
        assertThat(caretaker.passwordRecovery().isValidFor(THIRD.hash(), clock.instant())).isTrue();
        assertThat(caretaker.recoveryAllowance().requestsInWindow()).isEqualTo(3);
    }

    @Test
    @DisplayName("opens another hour once the first one is over, counting one again")
    void givenThreeLinksInAnHourThatIsOver_whenIssuingAnother_thenOpenANewHourCountingOne() {
        // given
        Caretaker caretaker = aValidCaretaker().build();
        caretaker.issuePasswordRecovery(FIRST, clock);
        caretaker.issuePasswordRecovery(SECOND, clock);
        caretaker.issuePasswordRecovery(THIRD, clock);
        clock.advance(Duration.ofHours(1));

        // when
        caretaker.issuePasswordRecovery(FOURTH, clock);

        // then
        assertThat(caretaker.passwordRecovery().isValidFor(FOURTH.hash(), clock.instant())).isTrue();
        assertThat(caretaker.recoveryAllowance().windowStartedAt()).isEqualTo(Instant.parse("2026-10-02T13:00:00Z"));
        assertThat(caretaker.recoveryAllowance().requestsInWindow()).isEqualTo(1);
    }

    @Test
    @DisplayName("starts the hour at the first request, and not at the full hour of the clock")
    void givenFirstRequestInTheMiddleOfAnHour_whenCountingTheNextOnes_thenCountFromThatRequest() {
        // given
        clock.advance(Duration.ofMinutes(50));
        Caretaker caretaker = aValidCaretaker().build();
        caretaker.issuePasswordRecovery(FIRST, clock);
        clock.advance(Duration.ofMinutes(20));
        caretaker.issuePasswordRecovery(SECOND, clock);
        caretaker.issuePasswordRecovery(THIRD, clock);

        // when
        Object code = refusalCodeOf(() -> caretaker.issuePasswordRecovery(FOURTH, clock));

        // then
        assertThat(code).isEqualTo(IdentityErrorCode.RECOVERY_LIMIT_REACHED);
        assertThat(caretaker.recoveryAllowance().windowStartedAt()).isEqualTo(Instant.parse("2026-10-02T12:50:00Z"));
    }

    // ---------------------------------------------------------------- link pendente (revisão, rodada 2)

    @Test
    @DisplayName("holds only the newest link, until it is used or annulled")
    void givenLinksIssuedUsedAndAnnulled_whenAskingWhichIsPending_thenHoldOnlyTheNewestStillValid() {
        // given
        Caretaker caretaker = aValidCaretaker().build();
        caretaker.issuePasswordRecovery(FIRST, clock);
        caretaker.issuePasswordRecovery(SECOND, clock);

        // when / then
        assertThat(caretaker.holdsPendingRecovery(FIRST)).isFalse();
        assertThat(caretaker.holdsPendingRecovery(SECOND)).isTrue();
        caretaker.changeOwnPassword(CaretakerTestDataBuilder.DEFAULT_PASSWORD, NEW_PASSWORD, hasher, clock);
        assertThat(caretaker.holdsPendingRecovery(SECOND)).isFalse();
    }
}
