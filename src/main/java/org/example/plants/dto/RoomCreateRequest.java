package org.example.plants.dto;

import jakarta.validation.constraints.NotBlank;

public record RoomCreateRequest(@NotBlank String name) {}
