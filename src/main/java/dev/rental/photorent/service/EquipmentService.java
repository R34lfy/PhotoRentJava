package dev.rental.photorent.service;

import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCategory;
import dev.rental.photorent.model.EquipmentCondition;
import dev.rental.photorent.model.EquipmentNote;
import dev.rental.photorent.model.EquipmentStatus;
import dev.rental.photorent.repository.EquipmentRepository;
import dev.rental.photorent.repository.EquipmentNoteRepository;

import dev.rental.photorent.model.User;
import dev.rental.photorent.model.AdminUser;


import java.util.List;
import java.util.Optional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentNoteRepository equipmentNoteRepository;
    private final Map<EquipmentCategory, AtomicInteger> serialCounters = new ConcurrentHashMap<>();
    private final AtomicLong noteIdCounter = new AtomicLong(0);

    public EquipmentService(EquipmentRepository equipmentRepository, EquipmentNoteRepository equipmentNoteRepository) {
        this.equipmentRepository = equipmentRepository;
        this.equipmentNoteRepository = equipmentNoteRepository;
    }

    public Equipment addEquipment(Equipment equipment) {
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment must not be null");
        }
        return equipmentRepository.add(equipment);
    }

    public Optional<Equipment> getById(Long id) {
        return equipmentRepository.findById(id);
    }

    public List<Equipment> getByCategory(EquipmentCategory category) {
        return equipmentRepository.findByCategory(category);
    }

    public List<Equipment> searchByName(String namePart) {
        return equipmentRepository.findByNameContaining(namePart);
    }

    public Optional<Equipment> findBestAvailableByName(String namePart) {
        return equipmentRepository.findByNameContaining(namePart).stream()
                .filter(Equipment::isAvailableForRental)
                .min(Comparator.comparing(Equipment::getCondition));
    }

    public List<Equipment> getAll() {
        return equipmentRepository.findAll();
    }

    public boolean removeById(Long id) {
        return equipmentRepository.deleteById(id);
    }

    public void updateCondition(Long equipmentId, EquipmentCondition newCondition, User staff, String reason) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalStateException("Equipment not found: " + equipmentId));

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason must be provided when changing equipment condition");
        }
        if (newCondition == EquipmentCondition.DECOMMISSIONED && !(staff instanceof AdminUser)) {
            throw new IllegalStateException("Only an administrator can mark equipment as decommissioned");
        }

        EquipmentCondition previousCondition = equipment.getCondition();
        equipment.setCondition(newCondition);

        if (newCondition == EquipmentCondition.DECOMMISSIONED) {
            equipment.setStatus(EquipmentStatus.RETIRED);
        }

        String noteText = String.format("Condition changed from %s to %s. Reason: %s",
                previousCondition, newCondition, reason);
        EquipmentNote note = new EquipmentNote(noteIdCounter.incrementAndGet(), equipment, staff, noteText, LocalDateTime.now());
        equipmentNoteRepository.add(note);
    }

    public EquipmentNote addNote(EquipmentNote note) {
        if (note == null) {
            throw new IllegalArgumentException("Note must not be null");
        }
        return equipmentNoteRepository.add(note);
    }

    public List<EquipmentNote> getNotesFor(Long equipmentId) {
        return equipmentNoteRepository.findByEquipmentId(equipmentId);
    }

    public String generateSerialNumber(EquipmentCategory category) {
        String prefix = switch (category) {
            case CAMERA -> "CAM";
            case LENS -> "LEN";
            case LIGHTING -> "LIG";
            case TRIPOD -> "TRI";
            case AUDIO -> "AUD";
            case BATTERY -> "BAT";
            case SDCARD -> "SDC";
        };

        int nextNumber = serialCounters
                .computeIfAbsent(category, key -> new AtomicInteger(0))
                .incrementAndGet();
        return prefix + "-" + String.format("%05d", nextNumber);
    }
}

