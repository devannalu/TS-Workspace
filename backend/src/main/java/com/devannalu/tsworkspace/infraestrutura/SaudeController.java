package com.devannalu.tsworkspace.infraestrutura;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SaudeController {
    private final MarcadorInfraestruturaRepository markers;

    public SaudeController(MarcadorInfraestruturaRepository markers) { this.markers = markers; }

    @GetMapping("/api/v1/health")
    public ResponseEntity<HealthResponse> health() {
        boolean ready = markers.findById(1).map(marker -> "java-foundation".equals(marker.getNome())).orElse(false);
        return ResponseEntity.status(ready ? 200 : 503).body(new HealthResponse(ready ? "UP" : "DOWN"));
    }

    public record HealthResponse(String status) { }
}
