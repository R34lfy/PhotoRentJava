package dev.rental.photorent.repository;

import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCategory;
import dev.rental.photorent.model.EquipmentCondition;
import dev.rental.photorent.model.EquipmentStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryEquipmentRepositoryTest {

    private EquipmentRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEquipmentRepository();
    }

    @Test
    void shouldAddAndFindEquipmentById() {
        Equipment camera = createCamera(1L, "Sony A7 IV");

        repository.add(camera);
        Optional<Equipment> found = repository.findById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Sony A7 IV");
    }

    @Test
    void shouldReturnEmptyOptionalWhenEquipmentNotFound() {
        Optional<Equipment> found = repository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindEquipmentByCategory() {
        repository.add(createCamera(1L, "Sony A7 IV"));
        repository.add(createLens(2L, "Sony 24-70mm GM"));

        List<Equipment> cameras = repository.findByCategory(EquipmentCategory.CAMERA);

        assertThat(cameras).hasSize(1);
        assertThat(cameras.get(0).getName()).isEqualTo("Sony A7 IV");
    }

    @Test
    void shouldFindEquipmentByPartialName() {
        repository.add(createCamera(1L, "Sony A7 IV"));
        repository.add(createCamera(2L, "Canon EOS R5"));

        List<Equipment> results = repository.findByNameContaining("sony");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void shouldDeleteEquipmentById() {
        repository.add(createCamera(1L, "Sony A7 IV"));

        boolean deleted = repository.deleteById(1L);

        assertThat(deleted).isTrue();
        assertThat(repository.findById(1L)).isEmpty();
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistentEquipment() {
        boolean deleted = repository.deleteById(999L);

        assertThat(deleted).isFalse();
    }

    private Equipment createCamera(Long id, String name) {
        return new Equipment(id, name, "Sony", "Full-frame mirrorless camera",
                EquipmentCategory.CAMERA, "SN-" + id, BigDecimal.valueOf(50), EquipmentStatus.AVAILABLE, EquipmentCondition.NEW);
    }

    private Equipment createLens(Long id, String name) {
        return new Equipment(id, name, "Sony", "Zoom lens",
                EquipmentCategory.LENS, "SN-" + id, BigDecimal.valueOf(30), EquipmentStatus.AVAILABLE, EquipmentCondition.GOOD);
    }
}

