package org.example.plants.controller;

import jakarta.validation.Valid;
import org.example.plants.dto.RoomCreateRequest;
import org.example.plants.dto.RoomStats;
import org.example.plants.model.Room;
import org.example.plants.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public List<Room> getAllRooms() {
        return roomService.getAllRooms();
    }

    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody @Valid RoomCreateRequest request) {
        if (roomService.nameTaken(request.name())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(request.name()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Room> renameRoom(@PathVariable Long id,
                                           @RequestBody @Valid RoomCreateRequest request) {
        if (roomService.nameTakenByOtherRoom(request.name(), id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Room room = roomService.renameRoom(id, request.name());
        return room != null ? ResponseEntity.ok(room) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        if (!roomService.roomExists(id)) return ResponseEntity.notFound().build();
        // A plant always belongs to a room, so its plants have to be moved first.
        if (roomService.plantCount(id) > 0) return ResponseEntity.status(HttpStatus.CONFLICT).build();
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public List<RoomStats> getRoomStats() {
        return roomService.getRoomStats();
    }
}
