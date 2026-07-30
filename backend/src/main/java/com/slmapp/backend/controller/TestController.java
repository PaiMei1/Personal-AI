package com.slmapp.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Test", description = "Sanity-check endpoints")
public class TestController {

    @GetMapping("/api/test/public")
    public Map<String, String> publicPing() {
        return Map.of("message", "pong - no auth needed");
    }

    @GetMapping("/api/test/secure")
    public Map<String, String> securePing(Authentication authentication) {
        return Map.of(
                "message", "pong - you are authenticated",
                "user", authentication.getName()
        );
    }
}