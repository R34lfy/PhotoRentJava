package dev.rental.photorent.repository;

import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long>, JpaSpecificationExecutor<Equipment> {

    // модель и категория подгружаются одним запросом, иначе DTO дочитывал бы их по одной
    @Override
    @EntityGraph(attributePaths = {"model", "model.category"})
    List<Equipment> findAll(Specification<Equipment> spec);

    @EntityGraph(attributePaths = {"model", "model.category"})
    List<Equipment> findByModelNameContainingIgnoreCaseAndStatus(String namePart, EquipmentStatus status);

    // блокировка нужна, чтобы два бронирования одной техники шли по очереди
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Equipment e where e.id = :id")
    Optional<Equipment> findByIdForUpdate(@Param("id") Long id);
}

