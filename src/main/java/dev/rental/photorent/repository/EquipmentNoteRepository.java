package dev.rental.photorent.repository;

import dev.rental.photorent.model.EquipmentNote;

import java.util.List;

public interface EquipmentNoteRepository {

    EquipmentNote add(EquipmentNote note);

    List<EquipmentNote> findByEquipmentId(Long equipmentId);

    boolean deleteById(Long id);
}

