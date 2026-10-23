package org.example.plants.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// No amount: a round rarely gives every plant the same volume, so an amount is only
// recorded on single watering events.
public record BulkWateringRequest(
        // @NotEmpty only checks the list itself, so the element constraint is what stops
        // a null id reaching the repository.
        @NotEmpty List<@NotNull Long> plantIds,
        String note,
        boolean fertilizer) {}
