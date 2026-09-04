package com.casava.demo.ai;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String sessionId, @NotBlank String message) {}
