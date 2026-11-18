package dev.rental.photorent.repository;

import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCondition;
import dev.rental.photorent.model.EquipmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findByModelCategorySerialPrefix(String serialPrefix);

    List<Equipment> findByStatus(EquipmentStatus status);

    List<Equipment> findByCondition(EquipmentCondition condition);

    List<Equipment> findByModelNameContainingIgnoreCase(String namePart);

    List<Equipment> findByModelNameContainingIgnoreCaseAndStatus(String namePart, EquipmentStatus status);

    // блокировка нужна, чтобы два бронирования одной техники шли по очереди
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Equipment e where e.id = :id")
    Optional<Equipment> findByIdForUpdate(@Param("id") Long id);
}

