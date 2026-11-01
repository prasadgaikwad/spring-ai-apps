package dev.prasadgaikwad.springaiapps.decision;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.prasadgaikwad.springaiapps.common.BadRequestException;

/**
 * Demo REST surface for System One/Jev-style decisions.
 */
@RestController
@RequestMapping("/api/decision")
public class DecisionController {

    private final DecisionClient client;

    public DecisionController(DecisionClient client) {
        this.client = client;
    }

    @PostMapping("/decide")
    @ResponseStatus(HttpStatus.OK)
    public DecisionPrimitives.DecisionResponse decide(@RequestBody DecideRequest request) {
        if (request == null || request.questions() == null || request.questions().isEmpty()) {
            throw new BadRequestException("'questions' must not be empty");
        }
        DecisionPrimitives.DecisionState state = request.state() == null
                ? new DecisionPrimitives.DecisionState(java.util.Map.of())
                : new DecisionPrimitives.DecisionState(request.state());
        return client.decide(state, request.questions());
    }

    @GetMapping("/ping")
    @ResponseStatus(HttpStatus.OK)
    public PingResponse ping() {
        return new PingResponse("ai-decision", "ok");
    }

    public record DecideRequest(java.util.Map<String, Object> state,
                                List<DecisionPrimitives.DecisionQuestion> questions) {
    }

    public record PingResponse(String module, String status) {
    }
}
