package dev.prasadgaikwad.springaiapps.agents;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the minimal Thymeleaf chat UI (one of the two demos with a UI, per the spec).
 */
@Controller
public class ChatController {

    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }
}
