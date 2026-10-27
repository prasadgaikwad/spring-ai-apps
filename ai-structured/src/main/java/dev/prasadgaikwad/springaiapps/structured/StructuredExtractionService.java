package dev.prasadgaikwad.springaiapps.structured;

import org.springframework.stereotype.Service;

import dev.prasadgaikwad.springaiapps.common.BadRequestException;

/**
 * Dispatches a {@link ExtractionRequest} to {@link ExtractionService} for the requested
 * target POJO type, keeping the type→class mapping in one place.
 */
@Service
public class StructuredExtractionService {

    private final ExtractionService extractionService;

    public StructuredExtractionService(ExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    public ExtractedResult<?> extract(ExtractionRequest request) {
        if (request == null || !org.springframework.util.StringUtils.hasText(request.type())) {
            throw new BadRequestException("'type' must not be blank");
        }
        return switch (request.type()) {
            case "candidate" -> extractionService.extract(request.text(), CandidateProfile.class);
            case "order" -> extractionService.extract(request.text(), CustomerOrder.class);
            default -> throw new BadRequestException(
                    "Unknown extraction type: " + request.type() + " (expected one of candidate, order)");
        };
    }
}
