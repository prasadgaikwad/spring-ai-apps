package dev.prasadgaikwad.springaiapps.eval;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dev.prasadgaikwad.springaiapps.common.BadRequestException;
import dev.prasadgaikwad.springaiapps.common.UpstreamServiceException;
import dev.prasadgaikwad.springaiapps.common.UpstreamTimeoutException;
import dev.prasadgaikwad.springaiapps.common.eval.Criterion;
import dev.prasadgaikwad.springaiapps.common.eval.EvalCase;
import dev.prasadgaikwad.springaiapps.common.eval.EvalHarness;
import dev.prasadgaikwad.springaiapps.common.eval.EvalReport;

/**
 * Orchestrates an Eval run: generates answers for pairs with a blank
 * {@code answer} with bounded parallelism via the configured {@code ChatModel}, grades
 * everything with the shared {@link EvalHarness}, diffs against the previous run for
 * regressions, and keeps a bounded, thread-safe in-memory history of runs. Input order
 * is preserved regardless of generation completion order.
 */
@Service
public class EvalRunService {

    private final ChatClient chatClient;
    private final EvalHarness evalHarness;
    private final int maxRuns;
    private final int generationConcurrency;
    private final Map<String, EvalReport> runs = new LinkedHashMap<>();

    public EvalRunService(ChatModel chatModel, EvalHarness evalHarness,
            @Value("${ai.eval.max-runs:100}") int maxRuns,
            @Value("${ai.eval.generation-concurrency:4}") int generationConcurrency) {
        this.chatClient = ChatClient.create(chatModel);
        this.evalHarness = evalHarness;
        this.maxRuns = maxRuns > 0 ? maxRuns : 100;
        this.generationConcurrency = generationConcurrency > 0 ? generationConcurrency : 4;
    }

    /**
     * Runs the harness and stores the report; returns the run with
     * {@link EvalReport#regressionsAgainst} computed from the most recent previous run.
     * Generation and grading run outside the history lock; history mutation is synchronized.
     */
    public EvalRunResponse run(EvalRunRequest request) {
        if (request == null || request.pairs() == null || request.criteria() == null) {
            throw new BadRequestException("'pairs' and 'criteria' are required");
        }
        if (request.pairs().isEmpty()) {
            throw new BadRequestException("'pairs' must not be empty");
        }
        if (request.criteria().isEmpty()) {
            throw new BadRequestException("'criteria' must not be empty");
        }

        List<Criterion> criteria = new ArrayList<>();
        for (int i = 0; i < request.criteria().size(); i++) {
            CriterionSpec spec = request.criteria().get(i);
            if (spec == null) {
                throw new BadRequestException("Criteria " + i + " must not be null");
            }
            criteria.add(spec.toCriterion());
        }

        int size = request.pairs().size();
        String[] questions = new String[size];
        String[] answers = new String[size];
        List<Integer> blankIndices = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            EvalRunRequest.QAPair pair = request.pairs().get(i);
            if (pair == null) {
                throw new BadRequestException("Pair " + i + " must not be null");
            }
            String question = pair.question();
            if (!StringUtils.hasText(question)) {
                throw new BadRequestException("Pair " + i + " requires a non-blank 'question'");
            }
            questions[i] = question;
            answers[i] = pair.answer();
            if (!StringUtils.hasText(answers[i])) {
                blankIndices.add(i);
            }
        }

        generateAnswers(questions, answers, blankIndices);

        List<EvalCase> cases = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            cases.add(new EvalCase("case-" + i, questions[i], answers[i] == null ? "" : answers[i]));
        }

        EvalReport report = evalHarness.run(cases, criteria);
        synchronized (this) {
            List<String> regressions = List.of();
            EvalReport baseline = lastReport();
            if (baseline != null) {
                regressions = report.regressionsAgainst(baseline);
            }
            String runId = UUID.randomUUID().toString();
            runs.put(runId, report);
            evictIfNeeded();
            return new EvalRunResponse(runId, report, regressions);
        }
    }

    /** Lists stored runs in insertion order (oldest to newest within retained window). */
    public synchronized List<EvalRunSummary> listRuns() {
        List<EvalRunSummary> summaries = new ArrayList<>(runs.size());
        for (Map.Entry<String, EvalReport> entry : runs.entrySet()) {
            EvalReport report = entry.getValue();
            EvalReport.Summary summary = report.summary();
            summaries.add(new EvalRunSummary(entry.getKey(), summary.totalCases(), summary.passedCases(),
                    summary.passRate()));
        }
        return summaries;
    }

    /** Fetches a stored run by id. */
    public synchronized EvalRunResponse getRun(String runId) {
        EvalReport report = runs.get(runId);
        if (report == null) {
            throw new dev.prasadgaikwad.springaiapps.common.ResourceNotFoundException("Eval run not found: " + runId);
        }
        return new EvalRunResponse(runId, report, List.of());
    }

    /** Clears retained run history. */
    public synchronized void clearHistory() {
        runs.clear();
    }

    private EvalReport lastReport() {
        if (runs.isEmpty()) {
            return null;
        }
        EvalReport last = null;
        for (EvalReport report : runs.values()) {
            last = report;
        }
        return last;
    }

    private void evictIfNeeded() {
        while (runs.size() > maxRuns) {
            Iterator<String> it = runs.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
            else {
                break;
            }
        }
    }

    private void generateAnswers(String[] questions, String[] answers, List<Integer> blankIndices) {
        if (blankIndices.isEmpty()) {
            return;
        }
        ExecutorService executor = Executors.newFixedThreadPool(this.generationConcurrency);
        try {
            List<CompletableFuture<String>> futures = new ArrayList<>(blankIndices.size());
            for (int index : blankIndices) {
                final int questionIndex = index;
                futures.add(CompletableFuture.supplyAsync(() -> generateAnswer(questions[questionIndex]), executor));
            }
            for (int futureIndex = 0; futureIndex < futures.size(); futureIndex++) {
                answers[blankIndices.get(futureIndex)] = await(futures.get(futureIndex));
            }
        }
        finally {
            executor.shutdown();
        }
    }

    private static String await(CompletableFuture<String> future) {
        try {
            return future.get();
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new UpstreamServiceException("Evaluation model call interrupted", ex);
        }
        catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new UpstreamServiceException("Evaluation model call failed", cause);
        }
    }

    private String generateAnswer(String question) {
        try {
            String answer = chatClient.prompt()
                    .user(question)
                    .call()
                    .content();
            return answer == null ? "" : answer;
        }
        catch (Exception ex) {
            if (hasTimeoutCause(ex)) {
                throw new UpstreamTimeoutException("Evaluation model call timed out", ex);
            }
            throw new UpstreamServiceException("Evaluation model call failed", ex);
        }
    }

    private static boolean hasTimeoutCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof TimeoutException) {
                return true;
            }
            Throwable cause = current.getCause();
            current = cause == current ? null : cause;
        }
        return false;
    }
}
