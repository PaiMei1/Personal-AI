package com.slmapp.backend.controller;

import com.slmapp.backend.slm.SlmClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/slm")
public class SlmTestController {

    private final SlmClient slmClient;

    public SlmTestController(SlmClient slmClient) {
        this.slmClient = slmClient;
    }

    @PostMapping
    public String ask(@RequestParam String prompt) {
        return slmClient.complete("You are a helpful assistant.", prompt, null);
    }
}