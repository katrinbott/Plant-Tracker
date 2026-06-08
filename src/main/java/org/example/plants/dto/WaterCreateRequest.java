package org.example.plants.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class WaterCreateRequest {

    public record WateringRequest(
            @NotNull Long plantId,
            @Positive Integer amountMl,
            String note) {}
}
