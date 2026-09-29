package dev.rental.photorent.service;

import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCategory;
import dev.rental.photorent.repository.EquipmentRepository;

import java.util.List;
import java.util.Optional;

public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
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

    public List<Equipment> getAll() {
        return equipmentRepository.findAll();
    }

    public boolean removeById(Long id) {
        return equipmentRepository.deleteById(id);
    }
}

