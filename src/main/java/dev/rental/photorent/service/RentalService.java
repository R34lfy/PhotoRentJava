package dev.rental.photorent.service;

import dev.rental.photorent.model.CustomerUser;
import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentStatus;
import dev.rental.photorent.model.Rental;
import dev.rental.photorent.model.RentalStatus;
import dev.rental.photorent.repository.EquipmentRepository;
import dev.rental.photorent.repository.RentalRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class RentalService {

    private final RentalRepository rentalRepository;
    private final EquipmentRepository equipmentRepository;

    public RentalService(RentalRepository rentalRepository, EquipmentRepository equipmentRepository) {
        this.rentalRepository = rentalRepository;
        this.equipmentRepository = equipmentRepository;
    }

    public synchronized Rental bookRental(Long rentalId, Equipment equipment, CustomerUser customer,
                                          LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }
        if (!equipment.isAvailableForRental()) {
            throw new IllegalStateException("Equipment is not available for rental: " + equipment.getName());
        }

        List<Rental> existingRentals = rentalRepository.findByEquipmentId(equipment.getId());
        boolean hasConflict = existingRentals.stream()
                .filter(rental -> rental.getStatus() == RentalStatus.BOOKED || rental.getStatus() == RentalStatus.ACTIVE)
                .anyMatch(rental -> rental.overlapsWith(startDate, endDate));

        if (hasConflict) {
            throw new IllegalStateException("Equipment is already booked for the selected dates");
        }

        Rental rental = new Rental(rentalId, equipment, customer, startDate, endDate, RentalStatus.BOOKED);
        return rentalRepository.add(rental);
    }

    public void issueEquipment(Long rentalId) {
        Rental rental = getRentalOrThrow(rentalId);
        if (rental.getStatus() != RentalStatus.BOOKED) {
            throw new IllegalStateException("Only BOOKED rentals can be issued, current status: " + rental.getStatus());
        }
        rental.setStatus(RentalStatus.ACTIVE);
        rental.getEquipment().setStatus(EquipmentStatus.RENTED);
    }

    public void returnEquipment(Long rentalId) {
        Rental rental = getRentalOrThrow(rentalId);
        if (rental.getStatus() != RentalStatus.ACTIVE && rental.getStatus() != RentalStatus.OVERDUE) {
            throw new IllegalStateException("Only ACTIVE or OVERDUE rentals can be returned, current status: " + rental.getStatus());
        }
        rental.setStatus(RentalStatus.RETURNED);
        rental.getEquipment().setStatus(EquipmentStatus.AVAILABLE);
    }

    public List<Rental> getRentalsByCustomer(Long customerId) {
        return rentalRepository.findByCustomerId(customerId);
    }

    public List<Rental> getActiveRentals() {
        return rentalRepository.findByStatus(RentalStatus.ACTIVE);
    }

    public Optional<Rental> getById(Long id) {
        return rentalRepository.findById(id);
    }

    private Rental getRentalOrThrow(Long rentalId) {
        return rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalStateException("Rental not found: " + rentalId));
    }
}

