package org.example.plants.dto;

import jakarta.validation.constraints.NotBlank;

public record PlantCreateRequest(
        @NotBlank String name,
        String species,
        String location) {}

