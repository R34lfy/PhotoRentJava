package dev.rental.photorent.repository;

import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCategory;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository {

    Equipment add(Equipment equipment);

    Optional<Equipment> findById(Long id);

    List<Equipment> findByCategory(EquipmentCategory category);

    List<Equipment> findByNameContaining(String namePart);

    List<Equipment> findAll();

    boolean deleteById(Long id);
}

