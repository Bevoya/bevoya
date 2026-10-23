package app.bevoya.api.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness check for Render. It deliberately touches no database, so a health
 * probe never wakes or loads the database.
 */
@RestController
public class HealthController {

    public record HealthResponse(String status) {
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok");
    }
}
