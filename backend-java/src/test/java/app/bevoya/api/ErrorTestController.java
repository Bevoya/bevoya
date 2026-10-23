package app.bevoya.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoint that always fails. It exists only on the test classpath. */
@RestController
class ErrorTestController {

    @GetMapping("/test-only/boom")
    String boom() {
        throw new IllegalStateException("private text that must never reach a client");
    }
}
