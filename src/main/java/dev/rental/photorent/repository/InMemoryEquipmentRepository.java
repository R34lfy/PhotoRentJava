package dev.rental.photorent.repository;

import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCategory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryEquipmentRepository implements EquipmentRepository {

    private final Map<Long, Equipment> storage = new ConcurrentHashMap<>();

    @Override
    public Equipment add(Equipment equipment) {
        storage.put(equipment.getId(), equipment);
        return equipment;
    }

    @Override
    public Optional<Equipment> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Equipment> findByCategory(EquipmentCategory category) {
        return storage.values().stream()
                .filter(equipment -> equipment.getCategory() == category)
                .collect(Collectors.toList());
    }

    @Override
    public List<Equipment> findByNameContaining(String namePart) {
        String lowerCasePart = namePart.toLowerCase();
        return storage.values().stream()
                .filter(equipment -> equipment.getName().toLowerCase().contains(lowerCasePart))
                .collect(Collectors.toList());
    }

    @Override
    public List<Equipment> findAll() {
        return List.copyOf(storage.values());
    }

    @Override
    public boolean deleteById(Long id) {
        return storage.remove(id) != null;
    }
}

