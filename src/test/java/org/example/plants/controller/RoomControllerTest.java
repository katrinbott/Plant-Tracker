package org.example.plants.controller;

import org.example.plants.dto.RoomPlant;
import org.example.plants.dto.RoomStats;
import org.example.plants.model.Room;
import org.example.plants.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RoomService roomService;

    @Test
    void getAllRooms_returnsList() throws Exception {
        when(roomService.getAllRooms()).thenReturn(List.of(new Room("Living room")));

        mockMvc.perform(get("/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Living room"));
    }

    @Test
    void getRoomStats_returnsCountsAverageAndPlants() throws Exception {
        when(roomService.getRoomStats()).thenReturn(List.of(new RoomStats(
                1L, "Living room", 2, 2L, 6.0,
                List.of(new RoomPlant(11L, "Pothos", 20L, 5.0, true),
                        new RoomPlant(10L, "Monstera", 2L, 7.0, false)))));

        mockMvc.perform(get("/rooms/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Living room"))
                .andExpect(jsonPath("$[0].plantCount").value(2))
                .andExpect(jsonPath("$[0].daysSinceLastWatering").value(2))
                .andExpect(jsonPath("$[0].averageDaysBetweenWaterings").value(6.0))
                .andExpect(jsonPath("$[0].plants[0].name").value("Pothos"))
                .andExpect(jsonPath("$[0].plants[0].daysSinceLastWatering").value(20))
                .andExpect(jsonPath("$[0].plants[0].overdue").value(true))
                .andExpect(jsonPath("$[0].plants[1].overdue").value(false));
    }

    @Test
    void getRoomStats_serialisesNullsForRoomWithNoWaterings() throws Exception {
        when(roomService.getRoomStats()).thenReturn(List.of(new RoomStats(
                1L, "Hallway", 1, null, null, List.of())));

        mockMvc.perform(get("/rooms/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].daysSinceLastWatering").isEmpty())
                .andExpect(jsonPath("$[0].averageDaysBetweenWaterings").isEmpty())
                .andExpect(jsonPath("$[0].plants").isEmpty());
    }

    @Test
    void createRoom_returns201WithBody() throws Exception {
        when(roomService.nameTaken("Balcony")).thenReturn(false);
        when(roomService.createRoom("Balcony")).thenReturn(new Room("Balcony"));

        mockMvc.perform(post("/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Balcony\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Balcony"));
    }

    @Test
    void createRoom_returns409WhenNameAlreadyExists() throws Exception {
        when(roomService.nameTaken("Kitchen")).thenReturn(true);

        mockMvc.perform(post("/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Kitchen\"}"))
                .andExpect(status().isConflict());

        verify(roomService, never()).createRoom(any());
    }

    @Test
    void createRoom_returns400WhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void renameRoom_returns200WithUpdatedRoom() throws Exception {
        when(roomService.nameTakenByOtherRoom("Pantry", 1L)).thenReturn(false);
        when(roomService.renameRoom(1L, "Pantry")).thenReturn(new Room("Pantry"));

        mockMvc.perform(patch("/rooms/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Pantry\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pantry"));
    }

    @Test
    void renameRoom_returns404WhenRoomNotFound() throws Exception {
        when(roomService.nameTakenByOtherRoom(eq("Attic"), eq(99L))).thenReturn(false);
        when(roomService.renameRoom(eq(99L), any())).thenReturn(null);

        mockMvc.perform(patch("/rooms/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Attic\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void renameRoom_returns409WhenAnotherRoomHasTheName() throws Exception {
        when(roomService.nameTakenByOtherRoom("Kitchen", 1L)).thenReturn(true);

        mockMvc.perform(patch("/rooms/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Kitchen\"}"))
                .andExpect(status().isConflict());

        verify(roomService, never()).renameRoom(any(), any());
    }

    @Test
    void deleteRoom_returns204WhenRoomIsEmpty() throws Exception {
        when(roomService.roomExists(1L)).thenReturn(true);
        when(roomService.plantCount(1L)).thenReturn(0L);
        when(roomService.deleteRoom(1L)).thenReturn(true);

        mockMvc.perform(delete("/rooms/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteRoom_returns409WhenRoomStillHasPlants() throws Exception {
        when(roomService.roomExists(1L)).thenReturn(true);
        when(roomService.plantCount(1L)).thenReturn(4L);

        mockMvc.perform(delete("/rooms/1"))
                .andExpect(status().isConflict());

        verify(roomService, never()).deleteRoom(any());
    }

    @Test
    void deleteRoom_returns404WhenRoomNotFound() throws Exception {
        when(roomService.roomExists(99L)).thenReturn(false);

        mockMvc.perform(delete("/rooms/99"))
                .andExpect(status().isNotFound());
    }
}
