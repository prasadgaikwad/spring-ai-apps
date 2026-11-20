package dev.prasadgaikwad.springaiapps.decision;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.prasadgaikwad.springaiapps.common.BadRequestException;
import dev.prasadgaikwad.springaiapps.decision.DecisionPrimitives.ChoiceOption;
import dev.prasadgaikwad.springaiapps.decision.DecisionPrimitives.DecisionQuestion;
import dev.prasadgaikwad.springaiapps.decision.DecisionPrimitives.DecisionState;
import dev.prasadgaikwad.springaiapps.decision.DecisionPrimitives.QuestionType;

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
        Map<String, Object> stateValues = request.state() == null ? Map.of() : request.state();
        if (stateValues.containsValue(null)) {
            throw new BadRequestException("'state' must not contain null values");
        }
        for (int i = 0; i < request.questions().size(); i++) {
            validateQuestion(request.questions().get(i), i);
        }
        return client.decide(new DecisionState(stateValues), request.questions());
    }

    @GetMapping("/ping")
    @ResponseStatus(HttpStatus.OK)
    public PingResponse ping() {
        return new PingResponse("ai-decision", "ok");
    }

    private static void validateQuestion(DecisionQuestion question, int index) {
        if (question == null) {
            throw new BadRequestException("Question " + index + " must not be null");
        }
        if (!StringUtils.hasText(question.id())) {
            throw new BadRequestException("Question " + index + " requires a non-blank 'id'");
        }
        if (!StringUtils.hasText(question.prompt())) {
            throw new BadRequestException("Question " + index + " requires a non-blank 'prompt'");
        }
        if (question.type() == null) {
            throw new BadRequestException("Question " + index + " requires a 'type'");
        }
        List<ChoiceOption> choices = question.choices() == null ? List.of() : question.choices();
        if (question.type() == QuestionType.CHOICE && choices.isEmpty()) {
            throw new BadRequestException("Question " + index + " of type CHOICE requires non-empty 'choices'");
        }
        for (int i = 0; i < choices.size(); i++) {
            ChoiceOption choice = choices.get(i);
            if (choice == null || !StringUtils.hasText(choice.id()) || !StringUtils.hasText(choice.label())) {
                throw new BadRequestException("Question " + index + " choice " + i
                        + " requires non-blank 'id' and 'label'");
            }
        }
    }

    public record DecideRequest(Map<String, Object> state, List<DecisionQuestion> questions) {
    }

    public record PingResponse(String module, String status) {
    }
}
