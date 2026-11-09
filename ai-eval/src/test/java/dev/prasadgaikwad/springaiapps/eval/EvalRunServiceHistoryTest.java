package dev.prasadgaikwad.springaiapps.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import dev.prasadgaikwad.springaiapps.common.eval.EvalHarness;

/** Unit tests for bounded, thread-safe run history. */
class EvalRunServiceHistoryTest {

    @Test
    void evictsOldestRunsWhenMaxExceeded() {
        EvalRunService service = newService(2);
        EvalRunResponse first = service.run(requestWithAnswer("A"));
        EvalRunResponse second = service.run(requestWithAnswer("B"));
        EvalRunResponse third = service.run(requestWithAnswer("C"));

        List<EvalRunSummary> runs = service.listRuns();
        assertThat(runs).hasSize(2);
        assertThat(runs).extracting(EvalRunSummary::runId)
                .containsExactly(second.runId(), third.runId());

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> service.getRun(first.runId())))
                .isInstanceOf(dev.prasadgaikwad.springaiapps.common.ResourceNotFoundException.class);
        assertThat(service.getRun(third.runId()).runId()).isEqualTo(third.runId());
    }

    @Test
    void clearHistoryRemovesAllRetainedRuns() {
        EvalRunService service = newService(10);
        service.run(requestWithAnswer("A"));
        service.run(requestWithAnswer("B"));
        assertThat(service.listRuns()).hasSize(2);

        service.clearHistory();

        assertThat(service.listRuns()).isEmpty();
    }

    @Test
    void concurrentRunsRemainConsistentAndBounded() throws Exception {
        int maxRuns = 20;
        int tasks = 60;
        EvalRunService service = newService(maxRuns);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<EvalRunResponse>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < tasks; i++) {
                final int n = i;
                futures.add(executor.submit(() -> {
                    start.await();
                    return service.run(requestWithAnswer("answer-" + n + " Spring"));
                }));
            }
            start.countDown();
            for (Future<EvalRunResponse> future : futures) {
                assertThat(future.get(30, TimeUnit.SECONDS).runId()).isNotBlank();
            }
        }
        finally {
            executor.shutdownNow();
        }

        List<EvalRunSummary> runs = service.listRuns();
        assertThat(runs).hasSizeLessThanOrEqualTo(maxRuns);
        assertThat(runs).extracting(EvalRunSummary::runId).doesNotHaveDuplicates();
        for (EvalRunSummary summary : runs) {
            assertThat(service.getRun(summary.runId()).report().summary().totalCases()).isEqualTo(1);
        }
    }

    private static EvalRunService newService(int maxRuns) {
        return new EvalRunService(mock(ChatModel.class), new EvalHarness(), maxRuns);
    }

    private static EvalRunRequest requestWithAnswer(String answer) {
        return new EvalRunRequest(
                List.of(new EvalRunRequest.QAPair("What is this?", answer)),
                List.of(new CriterionSpec("contains", "Spring")));
    }
}
