package org.example.plants.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlantCreateRequest(
        @NotBlank String name,
        String species,
        String location,
        @NotNull Long roomId) {}
