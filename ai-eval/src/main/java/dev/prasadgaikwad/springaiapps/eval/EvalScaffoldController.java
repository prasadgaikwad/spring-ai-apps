package dev.prasadgaikwad.springaiapps.eval;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Placeholder REST surface for the {@code ai-eval} Demo module.
 *
 * <p>Replaced by the in-app Eval harness flow in later work (issue #5), which
 * will reuse the shared harness in {@code ai-common}.
 */
@RestController
@RequestMapping("/api/eval")
public class EvalScaffoldController {

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ai-eval", "ok");
    }

    public record PingResponse(String module, String status) {
    }
}
