package org.example.plants.controller;

import org.example.plants.model.WateringEvent;
import org.example.plants.service.WateringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WateringController.class)
class WateringControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean WateringService wateringService;

    private WateringEvent event(Long plantId, Integer amountMl, String note) {
        WateringEvent e = new WateringEvent();
        e.setWateredAt(LocalDateTime.now());
        e.setAmountMl(amountMl);
        e.setNote(note);
        return e;
    }

    @Test
    void recordWatering_returns201WithBody() throws Exception {
        when(wateringService.recordWatering(1L, 200, "looked dry")).thenReturn(event(1L, 200, "looked dry"));

        mockMvc.perform(post("/watering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plantId\": 1, \"amountMl\": 200, \"note\": \"looked dry\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountMl").value(200))
                .andExpect(jsonPath("$.note").value("looked dry"));
    }

    @Test
    void recordWatering_returns400WhenPlantIdMissing() throws Exception {
        mockMvc.perform(post("/watering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountMl\": 200}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllWateringEvents_returnsList() throws Exception {
        when(wateringService.getAllWateringEvents()).thenReturn(List.of(event(1L, 100, null)));

        mockMvc.perform(get("/watering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amountMl").value(100));
    }

    @Test
    void getWateringHistoryForPlant_returnsFilteredList() throws Exception {
        when(wateringService.getWateringHistoryForPlant(2L)).thenReturn(List.of(event(2L, 150, "dry")));

        mockMvc.perform(get("/watering/plant/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amountMl").value(150));
    }

    @Test
    void deleteWateringEvent_returns204() throws Exception {
        mockMvc.perform(delete("/watering/5"))
                .andExpect(status().isNoContent());

        verify(wateringService).deleteWateringEvent(5L);
    }
}
