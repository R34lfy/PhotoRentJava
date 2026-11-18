package dev.rental.photorent.concurrent;

import dev.rental.photorent.TestData;
import dev.rental.photorent.model.CustomerUser;
import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCondition;
import dev.rental.photorent.model.EquipmentStatus;
import dev.rental.photorent.model.Rental;
import dev.rental.photorent.model.RentalStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = RentalOverdueProcessor.class)
class RentalOverdueProcessorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

    @Autowired
    private RentalOverdueProcessor processor;

    private final Equipment camera =
            TestData.equipment(1L, EquipmentStatus.RENTED, EquipmentCondition.GOOD);
    private final CustomerUser customer = TestData.customer(1L);

    @Test
    void shouldFindPastDueActiveRental() {
        Rental pastDue = rental(1L, LocalDate.of(2026, 9, 10), RentalStatus.ACTIVE);

        assertThat(processor.findOverdueRentals(List.of(pastDue), TODAY)).containsExactly(pastDue);
    }

    @Test
    void shouldIgnoreRentalNotYetDue() {
        Rental future = rental(1L, LocalDate.of(2026, 12, 31), RentalStatus.ACTIVE);

        assertThat(processor.findOverdueRentals(List.of(future), TODAY)).isEmpty();
    }

    @Test
    void shouldIgnoreRentalsThatAreNotActive() {
        Rental booked = rental(1L, LocalDate.of(2026, 9, 10), RentalStatus.BOOKED);

        assertThat(processor.findOverdueRentals(List.of(booked), TODAY)).isEmpty();
    }

    @Test
    void shouldNotChangeRentalStatuses() {
        Rental pastDue = rental(1L, LocalDate.of(2026, 9, 10), RentalStatus.ACTIVE);

        processor.findOverdueRentals(List.of(pastDue), TODAY);

        assertThat(pastDue.getStatus()).isEqualTo(RentalStatus.ACTIVE);
    }

    @Test
    void shouldHandleEmptyList() {
        assertThat(processor.findOverdueRentals(List.of(), TODAY)).isEmpty();
    }

    @Test
    void shouldProcessLargeNumberOfRentalsAcrossThreads() {
        List<Rental> rentals = new ArrayList<>();
        for (long id = 1; id <= 100; id++) {
            LocalDate endDate = (id % 2 == 0) ? LocalDate.of(2026, 9, 1) : LocalDate.of(2026, 12, 31);
            rentals.add(rental(id, endDate, RentalStatus.ACTIVE));
        }

        List<Rental> overdue = processor.findOverdueRentals(rentals, TODAY);

        assertThat(overdue).hasSize(50);
        assertThat(overdue).allMatch(rental -> rental.getId() % 2 == 0);
    }

    @Test
    void shouldRejectInvalidThreadCount() {
        assertThatThrownBy(() -> new RentalOverdueProcessor(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Rental rental(Long id, LocalDate endDate, RentalStatus status) {
        return TestData.rental(id, camera, customer, LocalDate.of(2026, 8, 1), endDate, status);
    }
}
