package dev.prasadgaikwad.springaiapps.rag;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.prasadgaikwad.springaiapps.common.BadRequestException;

/**
 * Query REST surface for the {@code ai-rag} Demo module.
 *
 * <p>Runs the full retrieval → rerank → generate query path over the ingested chunks and
 * returns the grounded answer together with the chunks that produced it. This is the
 * endpoint the Thymeleaf chat UI calls, so the demo's only way to answer a question is
 * through this contract.
 */
@RestController
@RequestMapping("/api/rag")
public class QueryController {

    private final RagQueryService queryService;

    public QueryController(RagQueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping("/query")
    @ResponseStatus(HttpStatus.OK)
    public QueryResponse query(@RequestBody QueryRequest request) {
        if (!StringUtils.hasText(request.question())) {
            throw new BadRequestException("'question' must not be blank");
        }
        return queryService.ask(request.question().trim());
    }
}
