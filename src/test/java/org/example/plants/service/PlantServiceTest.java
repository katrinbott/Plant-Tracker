package org.example.plants.service;

import org.example.plants.dto.PlantCreateRequest;
import org.example.plants.exception.RoomNotFoundException;
import org.example.plants.model.Plant;
import org.example.plants.model.PlantImage;
import org.example.plants.model.Room;
import org.example.plants.repository.PlantImageRepository;
import org.example.plants.repository.PlantRepository;
import org.example.plants.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlantServiceTest {

    @Mock PlantRepository plantRepository;
    @Mock PlantImageRepository plantImageRepository;
    @Mock RoomRepository roomRepository;
    @InjectMocks PlantService plantService;

    // JUnit creates a real temporary directory for each test and deletes it afterwards
    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        // Simulate the @Value injection that Spring would normally do
        ReflectionTestUtils.setField(plantService, "uploadDir", tempDir.toString());
    }

    @Test
    void createPlant_throwsWhenRoomDoesNotExist() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> plantService.createPlant(
                new PlantCreateRequest("Monstera", null, null, 99L)))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("Room 99 does not exist.");

        verify(plantRepository, never()).save(any());
    }

    @Test
    void createPlant_persistsAllFields() {
        when(roomRepository.findById(2L)).thenReturn(Optional.of(new Room("Living room")));
        when(plantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Plant result = plantService.createPlant(new PlantCreateRequest("Monstera", "Monstera deliciosa", "windowsill", 2L));

        assertThat(result.getRoom().getName()).isEqualTo("Living room");
        assertThat(result.getName()).isEqualTo("Monstera");
        assertThat(result.getSpecies()).isEqualTo("Monstera deliciosa");
        assertThat(result.getLocation()).isEqualTo("windowsill");
    }

    @Test
    void setRoom_assignsRoomAndSaves() {
        Plant plant = new Plant();
        when(plantRepository.findById(1L)).thenReturn(Optional.of(plant));
        when(roomRepository.findById(3L)).thenReturn(Optional.of(new Room("Kitchen")));
        when(plantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Plant result = plantService.setRoom(1L, 3L);

        assertThat(result.getRoom().getName()).isEqualTo("Kitchen");
        verify(plantRepository).save(plant);
    }

    // No room stub: an unknown plant returns before the room is looked up at all.
    @Test
    void setRoom_returnsNullWhenPlantNotFound() {
        when(plantRepository.findById(99L)).thenReturn(Optional.empty());

        Plant result = plantService.setRoom(99L, 3L);

        assertThat(result).isNull();
        verify(plantRepository, never()).save(any());
    }

    // Moving a plant into a room that is gone must leave the plant where it was,
    // not unassign it.
    @Test
    void setRoom_throwsWhenRoomNotFound() {
        when(plantRepository.findById(1L)).thenReturn(Optional.of(new Plant()));
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> plantService.setRoom(1L, 99L))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("Room 99 does not exist.");

        verify(plantRepository, never()).save(any());
    }

    @Test
    void updateLocation_savesTrimmedLocation() {
        when(plantRepository.findById(1L)).thenReturn(Optional.of(new Plant()));
        when(plantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Plant result = plantService.updateLocation(1L, "  Balcony ");

        assertThat(result.getLocation()).isEqualTo("Balcony");
    }

    @Test
    void updateLocation_clearsLocationWhenBlank() {
        Plant plant = new Plant();
        plant.setLocation("Kitchen");
        when(plantRepository.findById(1L)).thenReturn(Optional.of(plant));
        when(plantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Plant result = plantService.updateLocation(1L, "   ");

        assertThat(result.getLocation()).isNull();
    }

    @Test
    void updateLocation_returnsNullWhenPlantNotFound() {
        when(plantRepository.findById(99L)).thenReturn(Optional.empty());

        Plant result = plantService.updateLocation(99L, "Balcony");

        assertThat(result).isNull();
        verify(plantRepository, never()).save(any());
    }

    @Test
    void addImage_savesImageWithNote() throws IOException {
        when(plantRepository.existsById(1L)).thenReturn(true);
        when(plantImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        PlantImage result = plantService.addImage(1L, file, "chopped back due to pests");

        assertThat(result.getPlantId()).isEqualTo(1L);
        assertThat(result.getNote()).isEqualTo("chopped back due to pests");
        assertThat(result.getImagePath()).endsWith(".jpg");
    }

    @Test
    void addImage_doesNotSetNoteWhenBlank() throws IOException {
        when(plantRepository.existsById(1L)).thenReturn(true);
        when(plantImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1});

        PlantImage result = plantService.addImage(1L, file, "   ");

        assertThat(result.getNote()).isNull();
    }

    @Test
    void deleteImage_returnsFalseWhenImageDoesNotBelongToPlant() throws IOException {
        when(plantImageRepository.existsByIdAndPlantId(5L, 1L)).thenReturn(false);

        boolean result = plantService.deleteImage(1L, 5L);

        assertThat(result).isFalse();
        verify(plantImageRepository, never()).deleteById(any());
    }
}
