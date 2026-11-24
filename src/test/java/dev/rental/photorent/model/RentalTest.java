package dev.rental.photorent.model;

import dev.rental.photorent.TestData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RentalTest {

    private final Equipment camera = TestData.equipment(1L, EquipmentStatus.RENTED, EquipmentCondition.GOOD);
    private final CustomerUser customer = TestData.customer(1L);

    @Test
    void shouldDetectOverlapWhenPeriodsIntersect() {
        Rental rental = rental(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 15), RentalStatus.BOOKED);

        assertThat(rental.overlapsWith(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20))).isTrue();
    }

    @Test
    void shouldDetectOverlapWhenOtherPeriodIsInside() {
        Rental rental = rental(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30), RentalStatus.BOOKED);

        assertThat(rental.overlapsWith(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12))).isTrue();
    }

    @Test
    void shouldDetectOverlapWhenPeriodsTouchOnSameDay() {
        Rental rental = rental(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), RentalStatus.BOOKED);

        assertThat(rental.overlapsWith(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 10))).isTrue();
    }

    @Test
    void shouldNotOverlapWhenPeriodsAreSeparate() {
        Rental rental = rental(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), RentalStatus.BOOKED);

        assertThat(rental.overlapsWith(LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 10))).isFalse();
    }

    @Test
    void shouldBeOverdueWhenActiveAndPastEndDate() {
        Rental rental = rental(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10), RentalStatus.ACTIVE);

        assertThat(rental.isOverdue(LocalDate.of(2026, 9, 20))).isTrue();
    }

    @Test
    void shouldNotBeOverdueOnTheLastDay() {
        Rental rental = rental(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10), RentalStatus.ACTIVE);

        assertThat(rental.isOverdue(LocalDate.of(2026, 9, 10))).isFalse();
    }

    @Test
    void shouldNotBeOverdueWhenNotActive() {
        Rental rental = rental(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10), RentalStatus.BOOKED);

        assertThat(rental.isOverdue(LocalDate.of(2026, 9, 20))).isFalse();
    }

    private Rental rental(LocalDate start, LocalDate end, RentalStatus status) {
        return TestData.rental(1L, camera, customer, start, end, status);
    }
}
