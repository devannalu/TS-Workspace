package com.devannalu.tsworkspace.foundation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final SchemaMarkerRepository markers;

    public HealthController(SchemaMarkerRepository markers) { this.markers = markers; }

    @GetMapping("/api/v1/health")
    public ResponseEntity<HealthResponse> health() {
        boolean ready = markers.findById(1).map(marker -> "java-foundation".equals(marker.getName())).orElse(false);
        return ResponseEntity.status(ready ? 200 : 503).body(new HealthResponse(ready ? "UP" : "DOWN"));
    }

    public record HealthResponse(String status) { }
}
