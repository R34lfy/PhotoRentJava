package dev.rental.photorent.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EquipmentTest {

    @Test
    void shouldBeAvailableWhenStatusAvailableAndConditionGood() {
        Equipment camera = createCamera(EquipmentStatus.AVAILABLE, EquipmentCondition.GOOD);

        assertThat(camera.isAvailableForRental()).isTrue();
    }

    @Test
    void shouldNotBeAvailableWhenStatusIsRented() {
        Equipment camera = createCamera(EquipmentStatus.RENTED, EquipmentCondition.GOOD);

        assertThat(camera.isAvailableForRental()).isFalse();
    }

    @Test
    void shouldNotBeAvailableWhenConditionNeedsRepair() {
        Equipment camera = createCamera(EquipmentStatus.AVAILABLE, EquipmentCondition.NEEDS_REPAIR);

        assertThat(camera.isAvailableForRental()).isFalse();
    }

    @Test
    void shouldNotBeAvailableWhenConditionIsDecommissioned() {
        Equipment camera = createCamera(EquipmentStatus.AVAILABLE, EquipmentCondition.DECOMMISSIONED);

        assertThat(camera.isAvailableForRental()).isFalse();
    }

    @Test
    void shouldBeBookableRegardlessOfCurrentStatus() {
        Equipment camera = createCamera(EquipmentStatus.RENTED, EquipmentCondition.GOOD);

        assertThat(camera.isUsableForRental()).isTrue();
    }

    @Test
    void shouldNotBeBookableWhenNeedsRepair() {
        Equipment camera = createCamera(EquipmentStatus.AVAILABLE, EquipmentCondition.NEEDS_REPAIR);

        assertThat(camera.isUsableForRental()).isFalse();
    }

    @Test
    void shouldNotBeBookableWhenDecommissioned() {
        Equipment camera = createCamera(EquipmentStatus.AVAILABLE, EquipmentCondition.DECOMMISSIONED);

        assertThat(camera.isUsableForRental()).isFalse();
    }

    private Equipment createCamera(EquipmentStatus status, EquipmentCondition condition) {
        return new Equipment(1L, "Sony A7 IV", "Sony", "Full-frame mirrorless",
                EquipmentCategory.CAMERA, "CAM-00001", BigDecimal.valueOf(50), status, condition);
    }
}

