package com.ai.plug.core.spec.mrtr;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the round-limit guard in {@link MrtrDriver#resume}:
 * a misbehaving client that retries indefinitely must not be able to
 * grow the session unbounded. After the limit is exceeded the session
 * is abandoned and a {@link MrtrRoundLimitExceededException} is raised
 * so the wrapper's stale-token branch can recover cleanly.
 *
 * @author han
 * @time 2026/8/3
 */
class MrtrSafetyLimitsTest {

    @AfterEach
    void clearOverride() {
        MrtrSafetyLimits.setMaxRoundsForTests(-1);
    }

    @Test
    void resume_withinLimit_proceedsNormally() {
        MrtrSafetyLimits.setMaxRoundsForTests(3);
        InMemoryMrtrSessionStore store = new InMemoryMrtrSessionStore();
        MrtrDriver.Started s1 = MrtrDriver.start(store,
            ctx -> MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.ElicitationInputRequest.create("q1",
                    Map.of("type", "object"))))),
            Map.of());

        // 2 more rounds — within limit.
        MrtrDriver.Resumed r1 = MrtrDriver.resume(store, s1.requestState(),
            MrtrTypes.InputResponses.of(Map.of("a", "1")),
            ctx -> MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.ElicitationInputRequest.create("q2",
                    Map.of("type", "object"))))),
            Map.of());
        assertThat(r1.completed()).isFalse();

        MrtrDriver.Resumed r2 = MrtrDriver.resume(store, s1.requestState(),
            MrtrTypes.InputResponses.of(Map.of("a", "2")),
            ctx -> MrtrDriver.done("final"),
            Map.of());
        assertThat(r2.completed()).isTrue();
        assertThat(r2.finalResult()).isEqualTo("final");
    }

    @Test
    void resume_exceedingLimit_throwsAndAbandons() {
        MrtrSafetyLimits.setMaxRoundsForTests(2);
        InMemoryMrtrSessionStore store = new InMemoryMrtrSessionStore();
        MrtrDriver.Started s1 = MrtrDriver.start(store,
            ctx -> MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.ElicitationInputRequest.create("q",
                    Map.of("type", "object"))))),
            Map.of());

        // Round 2 — within limit (limit=2, nextRound=2).
        MrtrDriver.resume(store, s1.requestState(),
            MrtrTypes.InputResponses.of(Map.of("a", "1")),
            ctx -> MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.ElicitationInputRequest.create("q",
                    Map.of("type", "object"))))),
            Map.of());

        // Round 3 — exceeds limit (nextRound=3 > 2) → throw + abandon.
        assertThatThrownBy(() -> MrtrDriver.resume(store, s1.requestState(),
            MrtrTypes.InputResponses.of(Map.of("a", "2")),
            ctx -> MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.ElicitationInputRequest.create("q",
                    Map.of("type", "object"))))),
            Map.of()))
            .isInstanceOf(MrtrRoundLimitExceededException.class)
            .hasMessageContaining("limit=2");

        assertThat(store.activeCount()).isZero();
        assertThat(store.get(s1.requestState())).isEmpty();
    }

    @Test
    void maxRounds_defaultIsEight() {
        MrtrSafetyLimits.setMaxRoundsForTests(-1);
        assertThat(MrtrSafetyLimits.maxRounds()).isEqualTo(8);
    }

    @Test
    void maxRounds_invalidPropertyFallsBackToDefault() {
        MrtrSafetyLimits.setMaxRoundsForTests(-1);
        String previous = System.setProperty(MrtrSafetyLimits.MAX_ROUNDS_PROPERTY, "not-a-number");
        try {
            assertThat(MrtrSafetyLimits.maxRounds()).isEqualTo(8);
        } finally {
            if (previous == null) {
                System.clearProperty(MrtrSafetyLimits.MAX_ROUNDS_PROPERTY);
            } else {
                System.setProperty(MrtrSafetyLimits.MAX_ROUNDS_PROPERTY, previous);
            }
        }
    }
}