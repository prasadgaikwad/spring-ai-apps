package dev.prasadgaikwad.springaiapps.rag;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Thymeleaf chat surface for the {@code ai-rag} Demo module.
 *
 * <p>Renders the minimal chat page; the page itself asks questions through the REST
 * contract ({@code POST /api/rag/query}), so the UI and API share one query path.
 */
@Controller
public class ChatController {

    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }
}
