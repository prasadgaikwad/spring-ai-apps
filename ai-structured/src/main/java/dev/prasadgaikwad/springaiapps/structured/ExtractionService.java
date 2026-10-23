package dev.prasadgaikwad.springaiapps.structured;

import java.util.List;

import jakarta.validation.Validator;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dev.prasadgaikwad.springaiapps.common.BadRequestException;

/**
 * Turns free text into a typed POJO with Spring AI's structured output, then validates
 * the result against the POJO's bean-validation constraints.
 *
 * <p>The two halves of the demo are visible here: {@code entity(Class)} asks the model
 * for a typed object (Spring AI attaches the JSON-schema instructions), and {@link Validator}
 * checks that what came back actually satisfies the constraints — a model can produce a
 * parseable POJO that is still invalid, which is exactly what the app must catch.
 */
@Service
public class ExtractionService {

    private final ChatClient chatClient;
    private final Validator validator;

    public ExtractionService(ChatModel chatModel, Validator validator) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("You are an extraction engine. Read the user's text and return ONLY the fields "
                        + "requested, using the schema. Do not invent facts that are not present; leave fields "
                        + "empty when the text does not state them.")
                .build();
        this.validator = validator;
    }

    /** Extracts a {@link CandidateProfile} from free text and validates it. */
    public <T> ExtractedResult<T> extract(String text, Class<T> type) {
        if (!StringUtils.hasText(text)) {
            throw new BadRequestException("'text' must not be blank");
        }
        T value;
        try {
            value = chatClient.prompt().user(text).call().entity(type);
        }
        catch (Exception ex) {
            throw new BadRequestException("Model could not produce a structured response: " + ex.getMessage());
        }
        if (value == null) {
            throw new BadRequestException("Model returned no structured response");
        }
        List<String> violations = validator.validate(value).stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .toList();
        return new ExtractedResult<>(value, violations);
    }
}
