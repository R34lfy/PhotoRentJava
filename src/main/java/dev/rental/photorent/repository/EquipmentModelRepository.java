package dev.rental.photorent.repository;

import dev.rental.photorent.model.EquipmentModel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentModelRepository extends JpaRepository<EquipmentModel, Long> {

    boolean existsByBrandIgnoreCaseAndNameIgnoreCase(String brand, String name);

    @Override
    @EntityGraph(attributePaths = "category")
    List<EquipmentModel> findAll();

    @EntityGraph(attributePaths = "category")
    List<EquipmentModel> findByCategorySerialPrefix(String serialPrefix);
}

