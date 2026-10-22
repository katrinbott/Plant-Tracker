package org.example.plants.service;

import org.example.plants.model.Plant;
import org.example.plants.model.Room;
import org.example.plants.repository.PlantRepository;
import org.example.plants.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock RoomRepository roomRepository;
    @Mock PlantRepository plantRepository;
    @InjectMocks RoomService roomService;

    @Test
    void createRoom_trimsName() {
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Room result = roomService.createRoom("  Living room  ");

        assertThat(result.getName()).isEqualTo("Living room");
    }

    @Test
    void nameTaken_ignoresCaseAndSurroundingSpace() {
        when(roomRepository.existsByNameIgnoreCase("Kitchen")).thenReturn(true);

        assertThat(roomService.nameTaken("  Kitchen ")).isTrue();
    }

    @Test
    void renameRoom_returnsNullWhenRoomNotFound() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(roomService.renameRoom(99L, "Attic")).isNull();
        verify(roomRepository, never()).save(any());
    }

    @Test
    void renameRoom_savesTrimmedName() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(new Room("Kitchen")));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Room result = roomService.renameRoom(1L, " Pantry ");

        assertThat(result.getName()).isEqualTo("Pantry");
    }

    @Test
    void deleteRoom_deletesWhenRoomIsEmpty() {
        when(roomRepository.existsById(1L)).thenReturn(true);
        when(plantRepository.countByRoom_Id(1L)).thenReturn(0L);

        assertThat(roomService.deleteRoom(1L)).isTrue();
        verify(roomRepository).deleteById(1L);
    }

    @Test
    void deleteRoom_refusesWhenRoomStillHasPlants() {
        when(roomRepository.existsById(1L)).thenReturn(true);
        when(plantRepository.countByRoom_Id(1L)).thenReturn(4L);

        assertThat(roomService.deleteRoom(1L)).isFalse();
        verify(roomRepository, never()).deleteById(any());
    }

    @Test
    void deleteRoom_returnsFalseWhenRoomNotFound() {
        when(roomRepository.existsById(99L)).thenReturn(false);

        assertThat(roomService.deleteRoom(99L)).isFalse();
        verify(roomRepository, never()).deleteById(any());
    }
}
