package dev.rental.photorent.service;

import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCategory;
import dev.rental.photorent.model.EquipmentCondition;
import dev.rental.photorent.model.EquipmentStatus;
import dev.rental.photorent.repository.EquipmentRepository;
import dev.rental.photorent.repository.InMemoryEquipmentRepository;

import dev.rental.photorent.model.CustomerUser;

import dev.rental.photorent.model.Rental;
import dev.rental.photorent.model.RentalStatus;
import dev.rental.photorent.repository.RentalRepository;
import dev.rental.photorent.repository.InMemoryRentalRepository;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RentalServiceTest {

    private RentalService rentalService;
    private RentalRepository rentalRepository;
    private Equipment camera;
    private CustomerUser firstCustomer;
    private CustomerUser secondCustomer;

    @BeforeEach
    void setUp() {
        rentalRepository = new InMemoryRentalRepository();
        EquipmentRepository equipmentRepository = new InMemoryEquipmentRepository();
        rentalService = new RentalService(rentalRepository, equipmentRepository);

        camera = new Equipment(1L, "Sony A7 IV", "Sony", "Full-frame mirrorless",
                EquipmentCategory.CAMERA, "CAM-00001", BigDecimal.valueOf(50),
                EquipmentStatus.AVAILABLE, EquipmentCondition.NEW);
        equipmentRepository.add(camera);

        firstCustomer = new CustomerUser(1L, "first_customer", "first@example.com");
        secondCustomer = new CustomerUser(2L, "second_customer", "second@example.com");
    }

    @Test
    void shouldBookRentalWhenEquipmentAvailableAndDatesFree() {
        Rental rental = rentalService.bookRental(1L, camera, firstCustomer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.BOOKED);
    }

    @Test
    void shouldRejectBookingWithStartDateAfterEndDate() {
        assertThatThrownBy(() -> rentalService.bookRental(1L, camera, firstCustomer,
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectBookingWhenEquipmentNeedsRepair() {
        camera.setCondition(EquipmentCondition.NEEDS_REPAIR);

        assertThatThrownBy(() -> rentalService.bookRental(1L, camera, firstCustomer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectBookingWithOverlappingDates() {
        rentalService.bookRental(1L, camera, firstCustomer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10));

        assertThatThrownBy(() -> rentalService.bookRental(2L, camera, secondCustomer,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 15)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldAllowBookingWithNonOverlappingDates() {
        rentalService.bookRental(1L, camera, firstCustomer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));

        Rental secondRental = rentalService.bookRental(2L, camera, secondCustomer,
                LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 10));

        assertThat(secondRental.getStatus()).isEqualTo(RentalStatus.BOOKED);
    }

    @Test
    void shouldIssueEquipmentAndUpdateStatuses() {
        Rental rental = rentalService.bookRental(1L, camera, firstCustomer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));

        rentalService.issueEquipment(rental.getId());

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(camera.getStatus()).isEqualTo(EquipmentStatus.RENTED);
    }

    @Test
    void shouldRejectIssuingAlreadyActiveRental() {
        Rental rental = rentalService.bookRental(1L, camera, firstCustomer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        rentalService.issueEquipment(rental.getId());

        assertThatThrownBy(() -> rentalService.issueEquipment(rental.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldReturnEquipmentAndUpdateStatuses() {
        Rental rental = rentalService.bookRental(1L, camera, firstCustomer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        rentalService.issueEquipment(rental.getId());

        rentalService.returnEquipment(rental.getId());

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.RETURNED);
        assertThat(camera.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    void shouldExtendActiveRentalWhenNoConflict() {
        Rental rental = rentalService.bookRental(1L, camera, firstCustomer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        rentalService.issueEquipment(rental.getId());

        rentalService.extendRental(rental.getId(), LocalDate.of(2026, 10, 10));

        assertThat(rental.getEndDate()).isEqualTo(LocalDate.of(2026, 10, 10));
    }

    @Test
    void shouldRejectExtensionWhenAnotherCustomerAlreadyBookedNextPeriod() {
        Rental rental = rentalService.bookRental(1L, camera, firstCustomer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        rentalService.issueEquipment(rental.getId());

        rentalService.bookRental(2L, camera, secondCustomer,
                LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 10));

        assertThatThrownBy(() -> rentalService.extendRental(rental.getId(), LocalDate.of(2026, 10, 8)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectExtensionWithEarlierDate() {
        Rental rental = rentalService.bookRental(1L, camera, firstCustomer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        rentalService.issueEquipment(rental.getId());

        assertThatThrownBy(() -> rentalService.extendRental(rental.getId(), LocalDate.of(2026, 10, 3)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectExtensionOfNonActiveRental() {
        Rental rental = rentalService.bookRental(1L, camera, firstCustomer,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));

        assertThatThrownBy(() -> rentalService.extendRental(rental.getId(), LocalDate.of(2026, 10, 10)))
                .isInstanceOf(IllegalStateException.class);
    }
}

