package dev.prasadgaikwad.springaiapps.rag;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.prasadgaikwad.springaiapps.common.BadRequestException;

/**
 * Ingestion REST surface for the {@code ai-rag} Demo module.
 *
 * <p>Accepts either a resource location ({@code source}) or inline text ({@code text})
 * and runs it through the ETL pipeline. {@link IngestResponse} reports how many chunks
 * were embedded and stored.
 */
@RestController
@RequestMapping("/api/rag")
public class IngestController {

    private final DocumentIngestionService ingestionService;
    private final ResourceLoader resourceLoader;

    public IngestController(DocumentIngestionService ingestionService, ResourceLoader resourceLoader) {
        this.ingestionService = ingestionService;
        this.resourceLoader = resourceLoader;
    }

    @PostMapping("/ingest")
    @ResponseStatus(HttpStatus.OK)
    public IngestResponse ingest(@RequestBody IngestRequest request) {
        if (StringUtils.hasText(request.text())) {
            return ingestionService.ingestText(request.text());
        }
        Resource resource = resourceLoader.getResource(request.source());
        if (!resource.exists()) {
            throw new BadRequestException("Source resource not found: " + request.source());
        }
        return ingestionService.ingestResource(resource);
    }
}
