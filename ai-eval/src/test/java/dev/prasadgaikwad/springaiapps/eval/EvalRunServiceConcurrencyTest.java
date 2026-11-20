package dev.prasadgaikwad.springaiapps.eval;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import dev.prasadgaikwad.springaiapps.common.eval.EvalHarness;
import dev.prasadgaikwad.springaiapps.common.eval.EvalReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies bounded parallel answer generation and input-order preservation. */
class EvalRunServiceConcurrencyTest {

    @Test
    void generatesBlankAnswersWithBoundedParallelismAndKeepsCaseOrder() throws Exception {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());

        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximumActive = new AtomicInteger();
        CountDownLatch firstWaveStarted = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        when(chatModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            String question = prompt.getUserMessage().getText();
            int current = active.incrementAndGet();
            maximumActive.accumulateAndGet(current, Math::max);
            firstWaveStarted.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            }
            finally {
                active.decrementAndGet();
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("generated:" + question))));
        });

        EvalRunService service = new EvalRunService(chatModel, new EvalHarness(), 10, 2);
        List<String> questions = List.of("q1", "q2", "q3", "q4");
        List<EvalRunRequest.QAPair> pairs = new ArrayList<>();
        for (String question : questions) {
            pairs.add(new EvalRunRequest.QAPair(question, null));
        }
        EvalRunRequest request = new EvalRunRequest(pairs, List.of(new CriterionSpec("contains", "generated")));

        ExecutorService caller = Executors.newSingleThreadExecutor();
        try {
            Future<EvalRunResponse> future = caller.submit(() -> service.run(request));
            assertThat(firstWaveStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(maximumActive.get()).isLessThanOrEqualTo(2);
            release.countDown();

            EvalRunResponse response = future.get(10, TimeUnit.SECONDS);
            assertThat(response.report().cases()).extracting(EvalReport.CaseReport::question)
                    .containsExactlyElementsOf(questions);
        }
        finally {
            release.countDown();
            caller.shutdownNow();
        }
        verify(chatModel, times(4)).call(any(Prompt.class));
    }
}
