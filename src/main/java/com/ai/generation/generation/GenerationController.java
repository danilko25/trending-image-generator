package com.ai.generation.generation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/generation")
@RequiredArgsConstructor
public class GenerationController {

    private final GenerationService generatorService;

    @PostMapping("/start")
    public ResponseEntity<HttpStatus> startGeneration(@RequestBody @Valid GenerationRequest request) {
        generatorService.startGeneration(request);
        return ResponseEntity.status(201).build();
    }
}
