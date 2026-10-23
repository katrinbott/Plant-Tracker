package org.example.plants.service;

import org.example.plants.dto.PlantAnalytics;
import org.example.plants.dto.RoomPlant;
import org.example.plants.dto.RoomStats;
import org.example.plants.model.Plant;
import org.example.plants.model.Room;
import org.example.plants.repository.PlantRepository;
import org.example.plants.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RoomService {
    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository roomRepository;
    private final PlantRepository plantRepository;
    private final AnalyticsService analyticsService;

    public RoomService(RoomRepository roomRepository, PlantRepository plantRepository,
                       AnalyticsService analyticsService) {
        this.roomRepository = roomRepository;
        this.plantRepository = plantRepository;
        this.analyticsService = analyticsService;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAllByOrderByNameAsc();
    }

    public boolean nameTaken(String name) {
        return roomRepository.existsByNameIgnoreCase(name.trim());
    }

    public boolean nameTakenByOtherRoom(String name, Long id) {
        return roomRepository.existsByNameIgnoreCaseAndIdNot(name.trim(), id);
    }

    public Room createRoom(String name) {
        return roomRepository.save(new Room(name.trim()));
    }

    public Room renameRoom(Long id, String name) {
        Room room = roomRepository.findById(id).orElse(null);
        if (room == null) return null;
        room.setName(name.trim());
        return roomRepository.save(room);
    }

    public boolean roomExists(Long id) {
        return roomRepository.existsById(id);
    }

    public long plantCount(Long roomId) {
        return plantRepository.countByRoom_Id(roomId);
    }

    /** Only deletes an empty room; a plant always belongs to one, so its plants must move first. */
    public boolean deleteRoom(Long id) {
        if (!roomRepository.existsById(id) || plantCount(id) > 0) return false;
        roomRepository.deleteById(id);
        log.info("Deleted room with id {}.", id);
        return true;
    }

    public List<RoomStats> getRoomStats() {
        return getAllRooms().stream().map(this::statsFor).toList();
    }

    // Per-plant numbers come from AnalyticsService so the interval maths lives in one
    // place. That is a query per plant, which is fine at this size.
    private RoomStats statsFor(Room room) {
        List<Plant> plants = plantRepository.findByRoom_Id(room.getId());
        List<RoomPlant> roomPlants = new ArrayList<>();
        List<Double> plantMedians = new ArrayList<>();
        Long daysSinceLast = null;
        double averageTotal = 0;
        int averageCount = 0;

        for (Plant plant : plants) {
            PlantAnalytics analytics = analyticsService.getAnalyticsForPlant(plant.getId());
            Long since = analytics.daysSinceLastWatering();
            Double average = analytics.averageDaysBetweenWaterings();
            Double median = analytics.medianDaysBetweenWaterings();

            // The room was last watered whenever its most recently watered plant was.
            if (since != null && (daysSinceLast == null || since < daysSinceLast)) {
                daysSinceLast = since;
            }
            // Each plant counts once, however often it is watered: the room average answers
            // how often watering happens here, and a cactus is part of that rhythm too.
            if (average != null) {
                averageTotal += average;
                averageCount++;
            }
            if (median != null) {
                plantMedians.add(median);
            }
            roomPlants.add(new RoomPlant(plant.getId(), plant.getName(), since,
                    median, analytics.overdue()));
        }

        // Thirstiest first, so the plants at risk of being forgotten head the list. A plant
        // with no watering recorded sorts above every number: "never" is the longest ago.
        roomPlants.sort(Comparator.comparing(RoomPlant::daysSinceLastWatering,
                Comparator.nullsFirst(Comparator.reverseOrder())));
        Double roomAverage = averageCount == 0 ? null : averageTotal / averageCount;
        // The median of the plants' medians, for the same reason: one number per plant. It is
        // reported alongside the average because overdue is judged by a median, so a card
        // showing only the average explained its own warnings in the wrong units.
        Double roomMedian = plantMedians.isEmpty() ? null : Medians.of(plantMedians);
        return new RoomStats(room.getId(), room.getName(), plants.size(),
                daysSinceLast, roomAverage, roomMedian, roomPlants);
    }
}
