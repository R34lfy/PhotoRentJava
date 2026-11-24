package dev.rental.photorent.model;

import dev.rental.photorent.TestData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EquipmentTest {

    @Test
    void shouldBeAvailableWhenStatusAvailableAndConditionGood() {
        assertThat(equipment(EquipmentStatus.AVAILABLE, EquipmentCondition.GOOD).isAvailableForRental()).isTrue();
    }

    @Test
    void shouldNotBeAvailableWhenRented() {
        assertThat(equipment(EquipmentStatus.RENTED, EquipmentCondition.GOOD).isAvailableForRental()).isFalse();
    }

    @Test
    void shouldNotBeAvailableWhenInMaintenance() {
        assertThat(equipment(EquipmentStatus.MAINTENANCE, EquipmentCondition.GOOD).isAvailableForRental()).isFalse();
    }

    @Test
    void shouldNotBeAvailableWhenNeedsRepair() {
        assertThat(equipment(EquipmentStatus.AVAILABLE, EquipmentCondition.NEEDS_REPAIR).isAvailableForRental()).isFalse();
    }

    @Test
    void shouldNotBeAvailableWhenDecommissioned() {
        assertThat(equipment(EquipmentStatus.AVAILABLE, EquipmentCondition.DECOMMISSIONED).isAvailableForRental()).isFalse();
    }

    @Test
    void shouldBeUsableRegardlessOfCurrentStatus() {
        assertThat(equipment(EquipmentStatus.RENTED, EquipmentCondition.GOOD).isUsable()).isTrue();
    }

    @Test
    void shouldNotBeUsableWhenNeedsRepair() {
        assertThat(equipment(EquipmentStatus.AVAILABLE, EquipmentCondition.NEEDS_REPAIR).isUsable()).isFalse();
    }

    @Test
    void shouldNotBeUsableWhenDecommissioned() {
        assertThat(equipment(EquipmentStatus.AVAILABLE, EquipmentCondition.DECOMMISSIONED).isUsable()).isFalse();
    }

    @Test
    void shouldBeEqualWhenIdsMatch() {
        Equipment first = TestData.equipment(1L, EquipmentStatus.AVAILABLE, EquipmentCondition.NEW);
        Equipment second = TestData.equipment(1L, EquipmentStatus.RENTED, EquipmentCondition.WORN);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldNotBeEqualWhenIdsDiffer() {
        assertThat(TestData.equipment(1L, EquipmentStatus.AVAILABLE, EquipmentCondition.NEW))
                .isNotEqualTo(TestData.equipment(2L, EquipmentStatus.AVAILABLE, EquipmentCondition.NEW));
    }

    @Test
    void shouldNotBeEqualWhenIdIsNotAssignedYet() {
        EquipmentModel model = TestData.model(1L, "Sony", "A7 IV");
        Equipment first = new Equipment(model, "CAM-00001", EquipmentStatus.AVAILABLE, EquipmentCondition.NEW);
        Equipment second = new Equipment(model, "CAM-00001", EquipmentStatus.AVAILABLE, EquipmentCondition.NEW);

        assertThat(first).isNotEqualTo(second);
    }

    private Equipment equipment(EquipmentStatus status, EquipmentCondition condition) {
        return TestData.equipment(1L, status, condition);
    }
}
