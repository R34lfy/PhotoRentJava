package dev.rental.photorent.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RentalTest {

    private final Equipment camera = new Equipment(1L, "Sony A7 IV", "Sony", "Full-frame mirrorless",
            EquipmentCategory.CAMERA, "CAM-00001", BigDecimal.valueOf(50),
            EquipmentStatus.AVAILABLE, EquipmentCondition.NEW);
    private final CustomerUser customer = new CustomerUser(1L, "john_doe", "john@example.com");

    @Test
    void shouldDetectOverlapWhenPeriodsIntersect() {
        Rental rental = new Rental(1L, camera, customer,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 15), RentalStatus.BOOKED);

        boolean overlaps = rental.overlapsWith(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20));

        assertThat(overlaps).isTrue();
    }

    @Test
    void shouldDetectNoOverlapWhenPeriodsAreSeparate() {
        Rental rental = new Rental(1L, camera, customer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), RentalStatus.BOOKED);

        boolean overlaps = rental.overlapsWith(LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 10));

        assertThat(overlaps).isFalse();
    }

    @Test
    void shouldDetectOverlapWhenPeriodsTouchOnSameDay() {
        Rental rental = new Rental(1L, camera, customer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), RentalStatus.BOOKED);

        boolean overlaps = rental.overlapsWith(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 10));

        assertThat(overlaps).isTrue();
    }

    @Test
    void shouldBeOverdueWhenActiveAndPastEndDate() {
        Rental rental = new Rental(1L, camera, customer,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10), RentalStatus.ACTIVE);

        boolean overdue = rental.isOverdue(LocalDate.of(2026, 9, 20));

        assertThat(overdue).isTrue();
    }

    @Test
    void shouldNotBeOverdueWhenStillWithinPeriod() {
        Rental rental = new Rental(1L, camera, customer,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), RentalStatus.ACTIVE);

        boolean overdue = rental.isOverdue(LocalDate.of(2026, 9, 20));

        assertThat(overdue).isFalse();
    }

    @Test
    void shouldNotBeOverdueWhenNotActive() {
        Rental rental = new Rental(1L, camera, customer,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10), RentalStatus.BOOKED);

        boolean overdue = rental.isOverdue(LocalDate.of(2026, 9, 20));

        assertThat(overdue).isFalse();
    }
}

