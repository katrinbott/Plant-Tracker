package org.example.plants.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// No amount: a round rarely gives every plant the same volume, so an amount is only
// recorded on single watering events.
public record BulkWateringRequest(
        @NotEmpty List<Long> plantIds,
        String note,
        boolean fertilizer) {}
