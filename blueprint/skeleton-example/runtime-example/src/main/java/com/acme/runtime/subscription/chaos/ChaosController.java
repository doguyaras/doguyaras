package com.acme.runtime.subscription.chaos;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * YALNIZ TEST: /internal/test/chaos. Allowlist'te yalniz "test" aktorune acik (subscription-app.yml); bean yalniz
 * runtime.chaos.enabled=true iken vardir, uretimde uc 404'tur ve kural default-deny'i bozmaz.
 */
@RestController
@RequestMapping("/internal/test/chaos")
@ConditionalOnProperty(name = "runtime.chaos.enabled", havingValue = "true")
public class ChaosController {

    private final ChaosState state;

    public ChaosController(ChaosState state) { this.state = state; }

    @PutMapping
    public ChaosState.Settings set(@RequestBody ChaosState.Settings settings) {
        state.set(settings);
        return state.settings();
    }

    @DeleteMapping
    public ChaosState.Settings clear() {
        state.set(ChaosState.Settings.NONE);
        return state.settings();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() { return state.stats(); }

    @PostMapping("/stats/reset")
    public Map<String, Object> resetStats() {
        state.resetStats();
        return state.stats();
    }

    @GetMapping("/traces")
    public Map<String, String> trace(@RequestParam String path) {
        String tp = state.tracesByPath.get(path);
        return tp == null ? Map.of() : Map.of("traceparent", tp);
    }
}
