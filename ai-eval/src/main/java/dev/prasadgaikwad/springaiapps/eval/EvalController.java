package dev.prasadgaikwad.springaiapps.eval;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface for the {@code ai-eval} Demo module.
 *
 * <ul>
 *   <li>{@code POST /api/eval/run} — grade pairs (generate answers if omitted)
 *   via the shared Eval harness</li>
 *   <li>{@code GET /api/eval/runs} — history of runs</li>
 *   <li>{@code GET /api/eval/runs/{runId}} — fetch a stored run</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/eval")
public class EvalController {

    private final EvalRunService service;

    public EvalController(EvalRunService service) {
        this.service = service;
    }

    @PostMapping("/run")
    @ResponseStatus(HttpStatus.OK)
    public EvalRunResponse run(@RequestBody EvalRunRequest request) {
        return service.run(request);
    }

    @GetMapping("/runs")
    @ResponseStatus(HttpStatus.OK)
    public List<EvalRunSummary> list() {
        return service.listRuns();
    }

    @GetMapping("/runs/{runId}")
    @ResponseStatus(HttpStatus.OK)
    public EvalRunResponse get(@PathVariable String runId) {
        return service.getRun(runId);
    }
}
