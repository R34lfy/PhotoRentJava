package dev.rental.photorent.repository;

import dev.rental.photorent.model.EquipmentNote;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryEquipmentNoteRepository implements EquipmentNoteRepository {

    private final Map<Long, EquipmentNote> storage = new ConcurrentHashMap<>();

    @Override
    public EquipmentNote add(EquipmentNote note) {
        storage.put(note.getId(), note);
        return note;
    }

    @Override
    public List<EquipmentNote> findByEquipmentId(Long equipmentId) {
        return storage.values().stream()
                .filter(note -> note.getEquipment().getId().equals(equipmentId))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteById(Long id) {
        return storage.remove(id) != null;
    }
}

