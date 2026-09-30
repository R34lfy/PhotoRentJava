package dev.rental.photorent.repository;

import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCategory;
import dev.rental.photorent.model.EquipmentCondition;
import dev.rental.photorent.model.EquipmentStatus;

import dev.rental.photorent.model.CustomerUser;

import dev.rental.photorent.model.Rental;
import dev.rental.photorent.model.RentalStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRentalRepositoryTest {

    private RentalRepository repository;
    private Equipment camera;
    private CustomerUser customer;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRentalRepository();
        camera = new Equipment(1L, "Sony A7 IV", "Sony", "Full-frame mirrorless",
                EquipmentCategory.CAMERA, "CAM-00001", BigDecimal.valueOf(50),
                EquipmentStatus.AVAILABLE, EquipmentCondition.NEW);
        customer = new CustomerUser(1L, "john_doe", "john@example.com");
    }

    @Test
    void shouldAddAndFindRentalById() {
        Rental rental = createRental(1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));

        repository.add(rental);
        Optional<Rental> found = repository.findById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getEquipment().getName()).isEqualTo("Sony A7 IV");
    }

    @Test
    void shouldFindRentalsByCustomerId() {
        repository.add(createRental(1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5)));

        List<Rental> rentals = repository.findByCustomerId(1L);

        assertThat(rentals).hasSize(1);
    }

    @Test
    void shouldFindRentalsByEquipmentId() {
        repository.add(createRental(1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5)));

        List<Rental> rentals = repository.findByEquipmentId(1L);

        assertThat(rentals).hasSize(1);
    }

    @Test
    void shouldFindRentalsByStatus() {
        repository.add(createRental(1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5)));

        List<Rental> booked = repository.findByStatus(RentalStatus.BOOKED);
        List<Rental> active = repository.findByStatus(RentalStatus.ACTIVE);

        assertThat(booked).hasSize(1);
        assertThat(active).isEmpty();
    }

    @Test
    void shouldDeleteRentalById() {
        repository.add(createRental(1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5)));

        boolean deleted = repository.deleteById(1L);

        assertThat(deleted).isTrue();
        assertThat(repository.findById(1L)).isEmpty();
    }

    private Rental createRental(Long id, LocalDate startDate, LocalDate endDate) {
        return new Rental(id, camera, customer, startDate, endDate, RentalStatus.BOOKED);
    }
}

