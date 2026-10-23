package dev.prasadgaikwad.springaiapps.eval;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dev.prasadgaikwad.springaiapps.common.BadRequestException;
import dev.prasadgaikwad.springaiapps.common.eval.Criterion;
import dev.prasadgaikwad.springaiapps.common.eval.EvalCase;
import dev.prasadgaikwad.springaiapps.common.eval.EvalHarness;
import dev.prasadgaikwad.springaiapps.common.eval.EvalReport;

/**
 * Orchestrates an Eval run: generates answers for pairs with a blank
 * {@code answer} via the configured {@code ChatModel}, grades everything with the
 * shared {@link EvalHarness}, diffs against the previous run for regressions, and
 * keeps an in-memory history of runs.
 */
@Service
public class EvalRunService {

    private final ChatClient chatClient;
    private final EvalHarness evalHarness;
    private final Deque<EvalReport> history = new ArrayDeque<>();
    private final Map<String, EvalReport> runs = new LinkedHashMap<>();

    public EvalRunService(ChatModel chatModel, EvalHarness evalHarness) {
        this.chatClient = ChatClient.create(chatModel);
        this.evalHarness = evalHarness;
    }

    /**
     * Runs the harness and stores the report; returns the run with
     * {@link EvalReport#regressionsAgainst} computed from the most recent previous run.
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
        for (CriterionSpec spec : request.criteria()) {
            criteria.add(spec.toCriterion());
        }

        List<EvalCase> cases = new ArrayList<>(request.pairs().size());
        for (int i = 0; i < request.pairs().size(); i++) {
            EvalRunRequest.QAPair pair = request.pairs().get(i);
            String question = pair.question();
            if (!StringUtils.hasText(question)) {
                throw new BadRequestException("Pair " + i + " requires a non-blank 'question'");
            }
            String answer = pair.answer();
            if (!StringUtils.hasText(answer)) {
                answer = generateAnswer(question);
            }
            String id = "case-" + i;
            cases.add(new EvalCase(id, question, answer));
        }

        EvalReport report = evalHarness.run(cases, criteria);
        List<String> regressions = List.of();
        EvalReport baseline = history.peekLast();
        if (baseline != null) {
            regressions = report.regressionsAgainst(baseline);
        }

        String runId = UUID.randomUUID().toString();
        runs.put(runId, report);
        history.addLast(report);
        return new EvalRunResponse(runId, report, regressions);
    }

    /** Lists stored runs in insertion order. */
    public List<EvalRunSummary> listRuns() {
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
    public EvalRunResponse getRun(String runId) {
        EvalReport report = runs.get(runId);
        if (report == null) {
            throw new dev.prasadgaikwad.springaiapps.common.ResourceNotFoundException("Eval run not found: " + runId);
        }
        return new EvalRunResponse(runId, report, List.of());
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
            throw new BadRequestException("Failed to generate answer for evaluation: " + ex.getMessage());
        }
    }
}
