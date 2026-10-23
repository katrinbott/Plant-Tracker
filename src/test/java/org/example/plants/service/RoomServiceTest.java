package org.example.plants.service;

import org.example.plants.dto.RoomPlant;
import org.example.plants.dto.PlantAnalytics;
import org.example.plants.dto.RoomStats;
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
    @Mock AnalyticsService analyticsService;
    @InjectMocks RoomService roomService;

    private Plant plant(Long id, String name) {
        Plant plant = new Plant();
        plant.setId(id);
        plant.setName(name);
        return plant;
    }

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

    @Test
    void getRoomStats_reportsCountRecencyAndAverage() {
        Room room = new Room("Living room");
        room.setId(1L);
        when(roomRepository.findAllByOrderByNameAsc()).thenReturn(List.of(room));
        when(plantRepository.findByRoom_Id(1L))
                .thenReturn(List.of(plant(10L, "Monstera"), plant(11L, "Pothos")));
        when(analyticsService.getAnalyticsForPlant(10L)).thenReturn(new PlantAnalytics(5, 7.0, 7.0, 2L, false));
        when(analyticsService.getAnalyticsForPlant(11L)).thenReturn(new PlantAnalytics(4, 5.0, 5.0, 4L, false));

        RoomStats stats = roomService.getRoomStats().get(0);

        assertThat(stats.name()).isEqualTo("Living room");
        assertThat(stats.plantCount()).isEqualTo(2);
        // The room counts as watered when its most recently watered plant was.
        assertThat(stats.daysSinceLastWatering()).isEqualTo(2L);
        assertThat(stats.averageDaysBetweenWaterings()).isEqualTo(6.0);
        assertThat(stats.medianDaysBetweenWaterings()).isEqualTo(6.0);
        assertThat(stats.plants()).extracting(RoomPlant::overdue).containsOnly(false);
    }

    // One number per plant on both sides, so a plant watered fifty times does not outvote a
    // cactus watered twice: the room's figures describe how often watering happens here.
    @Test
    void getRoomStats_roomMedianIsTheMedianOfThePlantMedians() {
        Room room = new Room("Living room");
        room.setId(1L);
        when(roomRepository.findAllByOrderByNameAsc()).thenReturn(List.of(room));
        when(plantRepository.findByRoom_Id(1L)).thenReturn(
                List.of(plant(10L, "Monstera"), plant(11L, "Pothos"), plant(12L, "Cactus")));
        // Medians 7, 4 and 40: the mean of them is 17, the median 7, so the two cannot be
        // confused for one another.
        when(analyticsService.getAnalyticsForPlant(10L)).thenReturn(new PlantAnalytics(5, 7.0, 7.0, 2L, false));
        when(analyticsService.getAnalyticsForPlant(11L)).thenReturn(new PlantAnalytics(9, 4.0, 4.0, 1L, false));
        when(analyticsService.getAnalyticsForPlant(12L)).thenReturn(new PlantAnalytics(3, 40.0, 40.0, 5L, false));

        RoomStats stats = roomService.getRoomStats().get(0);

        assertThat(stats.medianDaysBetweenWaterings()).isEqualTo(7.0);
        assertThat(stats.averageDaysBetweenWaterings()).isEqualTo(17.0);
    }

    @Test
    void getRoomStats_roomMedianAveragesTheTwoMiddlePlantsWhenThereIsAnEvenNumber() {
        Room room = new Room("Living room");
        room.setId(1L);
        when(roomRepository.findAllByOrderByNameAsc()).thenReturn(List.of(room));
        when(plantRepository.findByRoom_Id(1L))
                .thenReturn(List.of(plant(10L, "Monstera"), plant(11L, "Pothos")));
        when(analyticsService.getAnalyticsForPlant(10L)).thenReturn(new PlantAnalytics(5, 7.0, 8.0, 2L, false));
        when(analyticsService.getAnalyticsForPlant(11L)).thenReturn(new PlantAnalytics(4, 5.0, 5.0, 1L, false));

        assertThat(roomService.getRoomStats().get(0).medianDaysBetweenWaterings()).isEqualTo(6.5);
    }

    // A plant with too few waterings has no median of its own, so it cannot contribute one.
    @Test
    void getRoomStats_roomMedianSkipsPlantsWithoutOne() {
        Room room = new Room("Living room");
        room.setId(1L);
        when(roomRepository.findAllByOrderByNameAsc()).thenReturn(List.of(room));
        when(plantRepository.findByRoom_Id(1L))
                .thenReturn(List.of(plant(10L, "Monstera"), plant(11L, "Cutting")));
        when(analyticsService.getAnalyticsForPlant(10L)).thenReturn(new PlantAnalytics(5, 7.0, 7.0, 2L, false));
        when(analyticsService.getAnalyticsForPlant(11L)).thenReturn(new PlantAnalytics(1, null, null, 3L, false));

        assertThat(roomService.getRoomStats().get(0).medianDaysBetweenWaterings()).isEqualTo(7.0);
    }

    // Thirstiest first, whether or not a plant is flagged: the Rooms tab lists every plant
    // in the room, so the order has to put the longest-unwatered at the top regardless.
    @Test
    void getRoomStats_listsEveryPlantLongestUnwateredFirst() {
        Room room = new Room("Living room");
        room.setId(1L);
        when(roomRepository.findAllByOrderByNameAsc()).thenReturn(List.of(room));
        when(plantRepository.findByRoom_Id(1L)).thenReturn(
                List.of(plant(10L, "Monstera"), plant(11L, "Pothos"), plant(12L, "Basil")));
        when(analyticsService.getAnalyticsForPlant(10L)).thenReturn(new PlantAnalytics(5, 7.0, 7.0, 9L, true));
        when(analyticsService.getAnalyticsForPlant(11L)).thenReturn(new PlantAnalytics(4, 5.0, 5.0, 20L, true));
        when(analyticsService.getAnalyticsForPlant(12L)).thenReturn(new PlantAnalytics(6, 3.0, 3.0, 1L, false));

        RoomStats stats = roomService.getRoomStats().get(0);

        assertThat(stats.plants()).extracting(RoomPlant::name)
                .containsExactly("Pothos", "Monstera", "Basil");
    }

    // A plant with no watering recorded has nothing to compare, and "never" is longer ago
    // than any number of days, so it heads the list rather than falling off the end.
    @Test
    void getRoomStats_sortsANeverWateredPlantFirst() {
        Room room = new Room("Living room");
        room.setId(1L);
        when(roomRepository.findAllByOrderByNameAsc()).thenReturn(List.of(room));
        when(plantRepository.findByRoom_Id(1L))
                .thenReturn(List.of(plant(10L, "Monstera"), plant(11L, "Cutting")));
        when(analyticsService.getAnalyticsForPlant(10L)).thenReturn(new PlantAnalytics(5, 7.0, 7.0, 30L, true));
        when(analyticsService.getAnalyticsForPlant(11L)).thenReturn(new PlantAnalytics(0, null, null, null, false));

        RoomStats stats = roomService.getRoomStats().get(0);

        assertThat(stats.plants()).extracting(RoomPlant::name)
                .containsExactly("Cutting", "Monstera");
    }

    // Whether a plant is overdue is decided in AnalyticsService; see its tests. What
    // matters here is that the entry reports the median rather than the mean.
    @Test
    void getRoomStats_overdueEntryCarriesTheMedian() {
        Room room = new Room("Living room");
        room.setId(1L);
        when(roomRepository.findAllByOrderByNameAsc()).thenReturn(List.of(room));
        when(plantRepository.findByRoom_Id(1L)).thenReturn(List.of(plant(10L, "Monstera")));
        when(analyticsService.getAnalyticsForPlant(10L))
                .thenReturn(new PlantAnalytics(5, 11.25, 5.0, 8L, true));

        RoomStats stats = roomService.getRoomStats().get(0);

        assertThat(stats.plants()).extracting(RoomPlant::name).containsExactly("Monstera");
        assertThat(stats.plants().get(0).medianDaysBetweenWaterings()).isEqualTo(5.0);
    }

    @Test
    void getRoomStats_listsAPlantNotFlaggedOverdueWithoutTheFlag() {
        Room room = new Room("Kitchen");
        room.setId(2L);
        when(roomRepository.findAllByOrderByNameAsc()).thenReturn(List.of(room));
        when(plantRepository.findByRoom_Id(2L)).thenReturn(List.of(plant(20L, "Basil")));
        when(analyticsService.getAnalyticsForPlant(20L)).thenReturn(new PlantAnalytics(2, 7.0, 7.0, 7L, false));

        assertThat(roomService.getRoomStats().get(0).plants())
                .singleElement()
                .returns("Basil", RoomPlant::name)
                .returns(false, RoomPlant::overdue);
    }

    @Test
    void getRoomStats_reportsNullAverageWhenAPlantHasTooFewWaterings() {
        Room room = new Room("Balcony");
        room.setId(3L);
        when(roomRepository.findAllByOrderByNameAsc()).thenReturn(List.of(room));
        when(plantRepository.findByRoom_Id(3L)).thenReturn(List.of(plant(30L, "Lavender")));
        // One watering, so there is no average interval to be overdue against.
        when(analyticsService.getAnalyticsForPlant(30L)).thenReturn(new PlantAnalytics(1, null, null, 40L, false));

        RoomStats stats = roomService.getRoomStats().get(0);

        assertThat(stats.plants()).singleElement()
                .returns(false, RoomPlant::overdue)
                .returns(null, RoomPlant::medianDaysBetweenWaterings);
        assertThat(stats.averageDaysBetweenWaterings()).isNull();
        assertThat(stats.medianDaysBetweenWaterings()).isNull();
        assertThat(stats.daysSinceLastWatering()).isEqualTo(40L);
    }

    @Test
    void getRoomStats_reportsNullsForRoomWithNoWaterings() {
        Room room = new Room("Hallway");
        room.setId(4L);
        when(roomRepository.findAllByOrderByNameAsc()).thenReturn(List.of(room));
        when(plantRepository.findByRoom_Id(4L)).thenReturn(List.of(plant(40L, "Tomato")));
        when(analyticsService.getAnalyticsForPlant(40L)).thenReturn(new PlantAnalytics(0, null, null, null, false));

        RoomStats stats = roomService.getRoomStats().get(0);

        assertThat(stats.plantCount()).isEqualTo(1);
        assertThat(stats.daysSinceLastWatering()).isNull();
        assertThat(stats.averageDaysBetweenWaterings()).isNull();
        assertThat(stats.medianDaysBetweenWaterings()).isNull();
        assertThat(stats.plants()).singleElement()
                .returns(null, RoomPlant::daysSinceLastWatering);
    }
}
