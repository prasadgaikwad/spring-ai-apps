package dev.prasadgaikwad.springaiapps.structured;

import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused unit tests for the validation half of the demo — the POJO constraints are pure
 * logic and worth exercising directly, independent of any model call.
 */
class ProfileValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void wellFormedCandidateHasNoViolations() {
        CandidateProfile profile = new CandidateProfile(
                "Ada Lovelace", "ada@example.com", "Engineer", 10,
                List.of("Java", "Spring"), List.of(new CandidateProfile.JobHistory("Analytical Engines", "Lead", 2010, 2020)));

        assertThat(validator.validate(profile)).isEmpty();
    }

    @Test
    void malformedCandidateReportsFieldViolations() {
        CandidateProfile profile = new CandidateProfile(
                "", "not-an-email", "", 99, List.of(), List.of());

        assertThat(validator.validate(profile))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("fullName", "email", "currentRole", "yearsOfExperience", "topSkills");
    }

    @Test
    void orderQuantityMustBePositive() {
        CustomerOrder order = new CustomerOrder("Ada", "Widget", 0, "1 Main St", List.of());
        assertThat(validator.validate(order))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("quantity");
    }

    @Test
    void validOrderPasses() {
        CustomerOrder order = new CustomerOrder("Ada", "Widget", 3, "1 Main St", List.of("gift wrap"));
        assertThat(validator.validate(order)).isEmpty();
    }
}
