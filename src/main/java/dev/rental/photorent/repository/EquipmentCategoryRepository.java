package dev.rental.photorent.repository;

import dev.rental.photorent.model.EquipmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EquipmentCategoryRepository extends JpaRepository<EquipmentCategory, String> {

    // блокирует строку до конца транзакции (параллельные запросы идут по очереди)
    @Modifying
    @Query("update EquipmentCategory c set c.lastSerialNumber = c.lastSerialNumber + 1 "
            + "where c.serialPrefix = :prefix")
    int incrementSerialCounter(@Param("prefix") String prefix);

    @Query("select c.lastSerialNumber from EquipmentCategory c where c.serialPrefix = :prefix")
    Optional<Integer> findSerialCounter(@Param("prefix") String prefix);
}

