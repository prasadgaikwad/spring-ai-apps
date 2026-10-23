package dev.prasadgaikwad.springaiapps.structured;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * The target POJO for typed extraction: a recruiter-ready candidate profile the model
 * must fill from free text. Field constraints express what "well-formed" means, so
 * validation is the demo's second half — the model produces a POJO, the app rejects a
 * malformed one.
 */
public record CandidateProfile(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank String currentRole,
        @Min(0) @Max(60) int yearsOfExperience,
        @NotEmpty List<@NotBlank String> topSkills,
        @Valid List<JobHistory> history) {

    /** One prior role in the candidate's history. */
    public record JobHistory(
            @NotBlank String company,
            @NotBlank String title,
            @Min(0) int startYear,
            @Min(0) int endYear) {
    }
}
