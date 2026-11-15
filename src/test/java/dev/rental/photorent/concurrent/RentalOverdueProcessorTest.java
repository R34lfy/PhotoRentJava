package dev.rental.photorent.concurrent;

import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCategory;
import dev.rental.photorent.model.EquipmentCondition;
import dev.rental.photorent.model.EquipmentStatus;

import dev.rental.photorent.model.CustomerUser;

import dev.rental.photorent.model.Rental;
import dev.rental.photorent.model.RentalStatus;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RentalOverdueProcessorTest {

    private final Equipment camera = new Equipment(1L, "Sony A7 IV", "Sony", "Full-frame mirrorless",
            EquipmentCategory.CAMERA, "CAM-00001", BigDecimal.valueOf(50),
            EquipmentStatus.RENTED, EquipmentCondition.NEW);
    private final CustomerUser customer = new CustomerUser(1L, "john_doe", "john@example.com");

    @Test
    void shouldMarkPastDueActiveRentalAsOverdue() {
        RentalOverdueProcessor processor = new RentalOverdueProcessor(2);
        Rental overdueRental = new Rental(1L, camera, customer,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10), RentalStatus.ACTIVE);
        LocalDate today = LocalDate.of(2026, 9, 20);

        processor.processOverdueRentals(List.of(overdueRental), today);

        assertThat(overdueRental.getStatus()).isEqualTo(RentalStatus.OVERDUE);
    }

    @Test
    void shouldNotChangeStatusOfRentalNotYetDue() {
        RentalOverdueProcessor processor = new RentalOverdueProcessor(2);
        Rental futureRental = new Rental(1L, camera, customer,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), RentalStatus.ACTIVE);
        LocalDate today = LocalDate.of(2026, 9, 20);

        processor.processOverdueRentals(List.of(futureRental), today);

        assertThat(futureRental.getStatus()).isEqualTo(RentalStatus.ACTIVE);
    }

    @Test
    void shouldNotChangeStatusOfRentalThatIsNotActive() {
        RentalOverdueProcessor processor = new RentalOverdueProcessor(2);
        Rental bookedRental = new Rental(1L, camera, customer,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10), RentalStatus.BOOKED);
        LocalDate today = LocalDate.of(2026, 9, 20);

        processor.processOverdueRentals(List.of(bookedRental), today);

        assertThat(bookedRental.getStatus()).isEqualTo(RentalStatus.BOOKED);
    }

    @Test
    void shouldHandleEmptyListWithoutErrors() {
        RentalOverdueProcessor processor = new RentalOverdueProcessor(4);

        processor.processOverdueRentals(List.of(), LocalDate.of(2026, 9, 20));
    }

    @Test
    void shouldCorrectlyProcessLargeNumberOfRentalsAcrossMultipleThreads() {
        RentalOverdueProcessor processor = new RentalOverdueProcessor(4);
        List<Rental> rentals = new ArrayList<>();
        for (long i = 1; i <= 100; i++) {
            LocalDate endDate = (i % 2 == 0) ? LocalDate.of(2026, 9, 1) : LocalDate.of(2026, 12, 31);
            rentals.add(new Rental(i, camera, customer, LocalDate.of(2026, 8, 1), endDate, RentalStatus.ACTIVE));
        }
        LocalDate today = LocalDate.of(2026, 9, 20);

        processor.processOverdueRentals(rentals, today);

        long overdueCount = rentals.stream().filter(r -> r.getStatus() == RentalStatus.OVERDUE).count();
        long stillActiveCount = rentals.stream().filter(r -> r.getStatus() == RentalStatus.ACTIVE).count();
        assertThat(overdueCount).isEqualTo(50);
        assertThat(stillActiveCount).isEqualTo(50);
    }

    @Test
    void shouldRejectInvalidThreadCount() {
        assertThatThrownBy(() -> new RentalOverdueProcessor(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

