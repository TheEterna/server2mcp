package com.ai.plug.core.spec.mrtr;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the cross-round response merge semantics in
 * {@link MrtrDriver#resume}: when a multi-round conversation continues,
 * the handler's {@link MrtrDriver.RoundContext#accumulatedResponses()} must
 * reflect ALL prior rounds' answers, not just the latest one. This is what
 * lets a handler decide "if I already have address but not payment, ask for
 * payment" without the framework having to plumb per-kind schema tracking.
 *
 * @author han
 * @time 2026/8/3
 */
class MrtrResponseMergeTest {

    @Test
    void resume_mergesPriorResponses_acrossRounds() {
        InMemoryMrtrSessionStore store = new InMemoryMrtrSessionStore();
        MrtrDriver.Started s1 = MrtrDriver.start(store,
            ctx -> MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.ElicitationInputRequest.create("q",
                    Map.of("type", "object"))))),
            Map.of());

        // Round 2 — supplies street.
        MrtrDriver.resume(store, s1.requestState(),
            MrtrTypes.InputResponses.of(Map.of("street", "123 Main")),
            ctx -> MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.ElicitationInputRequest.create("q2",
                    Map.of("type", "object"))))),
            Map.of());

        // Round 3 — supplies city; handler should see BOTH street + city.
        MrtrDriver.resume(store, s1.requestState(),
            MrtrTypes.InputResponses.of(Map.of("city", "Springfield")),
            ctx -> {
                assertThat(ctx.accumulatedResponses())
                    .containsEntry("street", "123 Main")
                    .containsEntry("city", "Springfield");
                return MrtrDriver.done("ok");
            },
            Map.of());
    }

    @Test
    void resume_latestRoundAnswersOverridePrior() {
        // When the same key appears in two rounds, the latest wins (the
        // client is correcting a prior answer).
        InMemoryMrtrSessionStore store = new InMemoryMrtrSessionStore();
        MrtrDriver.Started s1 = MrtrDriver.start(store,
            ctx -> MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.ElicitationInputRequest.create("q",
                    Map.of("type", "object"))))),
            Map.of());

        MrtrDriver.resume(store, s1.requestState(),
            MrtrTypes.InputResponses.of(Map.of("street", "wrong")),
            ctx -> MrtrDriver.nextRound(MrtrTypes.InputRequiredResult.of(
                List.of(MrtrTypes.ElicitationInputRequest.create("q2",
                    Map.of("type", "object"))))),
            Map.of());

        MrtrDriver.resume(store, s1.requestState(),
            MrtrTypes.InputResponses.of(Map.of("street", "correct")),
            ctx -> {
                assertThat(ctx.accumulatedResponses())
                    .containsEntry("street", "correct");
                return MrtrDriver.done("ok");
            },
            Map.of());
    }
}