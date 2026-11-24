package dev.rental.photorent.service;

import dev.rental.photorent.concurrent.RentalOverdueProcessor;
import dev.rental.photorent.model.CustomerUser;
import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentStatus;
import dev.rental.photorent.model.Rental;
import dev.rental.photorent.model.RentalStatus;
import dev.rental.photorent.repository.EquipmentRepository;
import dev.rental.photorent.repository.RentalRepository;
import dev.rental.photorent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentalService {

    private final RentalRepository rentalRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final RentalOverdueProcessor overdueProcessor;
    private final Clock clock;

    @Transactional
    public Rental bookRental(Long equipmentId, Long customerId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }
        if (startDate.isBefore(LocalDate.now(clock))) {
            throw new IllegalArgumentException("Start date must not be in the past");
        }

        // блокируем строку техники (параллельные брони одной техники идут по очереди)
        Equipment equipment = equipmentRepository.findByIdForUpdate(equipmentId)
                .orElseThrow(() -> new NoSuchElementException("Equipment not found: " + equipmentId));
        CustomerUser customer = userRepository.findCustomerById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));

        // для брони на будущее важна принципиальная пригодность, а не текущий статус
        if (!equipment.isUsable()) {
            throw new IllegalStateException("Equipment is not available for rental: " + equipment.getSerialNumber());
        }
        if (!rentalRepository.findOverlapping(equipmentId, RentalStatus.OPEN_STATUSES, startDate, endDate).isEmpty()) {
            throw new IllegalStateException("Equipment is already booked for the selected dates");
        }

        return rentalRepository.save(new Rental(equipment, customer, startDate, endDate, RentalStatus.BOOKED));
    }

    @Transactional
    public void issueEquipment(Long rentalId) {
        Rental rental = getRentalOrThrow(rentalId);
        if (rental.getStatus() != RentalStatus.BOOKED) {
            throw new IllegalStateException("Only BOOKED rentals can be issued, current status: " + rental.getStatus());
        }
        Equipment equipment = rental.getEquipment();
        // выдаём прямо сейчас, поэтому здесь важна именно текущая доступность
        if (!equipment.isAvailableForRental()) {
            throw new IllegalStateException("Equipment cannot be issued right now: " + equipment.getSerialNumber());
        }
        rental.setStatus(RentalStatus.ACTIVE);
        equipment.setStatus(EquipmentStatus.RENTED);
    }

    @Transactional
    public void returnEquipment(Long rentalId) {
        Rental rental = getRentalOrThrow(rentalId);
        if (rental.getStatus() != RentalStatus.ACTIVE && rental.getStatus() != RentalStatus.OVERDUE) {
            throw new IllegalStateException(
                    "Only ACTIVE or OVERDUE rentals can be returned, current status: " + rental.getStatus());
        }
        rental.setStatus(RentalStatus.RETURNED);

        // списанную технику (RETIRED) не возвращаем в оборот
        Equipment equipment = rental.getEquipment();
        if (equipment.getStatus() == EquipmentStatus.RENTED) {
            equipment.setStatus(EquipmentStatus.AVAILABLE);
        }
    }

    @Transactional
    public void extendRental(Long rentalId, LocalDate newEndDate) {
        Rental rental = getRentalOrThrow(rentalId);
        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE rentals can be extended, current status: " + rental.getStatus());
        }
        if (newEndDate == null || !newEndDate.isAfter(rental.getEndDate())) {
            throw new IllegalArgumentException("New end date must be after the current end date");
        }

        // у кого бронь раньше, тот и в приоритете (смотрим период со следующего дня после текущего конца)
        List<Rental> conflicts = rentalRepository.findOverlappingExcluding(
                rental.getEquipment().getId(), rental.getId(), RentalStatus.OPEN_STATUSES,
                rental.getEndDate().plusDays(1), newEndDate);
        if (!conflicts.isEmpty()) {
            LocalDate firstConflict = conflicts.stream()
                    .map(Rental::getStartDate)
                    .min(Comparator.naturalOrder())
                    .orElseThrow();
            throw new IllegalStateException(
                    "Cannot extend rental: equipment is already booked by another customer starting " + firstConflict);
        }

        rental.setEndDate(newEndDate);
        rentalRepository.saveAndFlush(rental);
    }

    // потоки ищут просроченные аренды параллельно, статус меняем уже в основном потоке;
    // если дата не указана, берём сегодняшнюю
    @Transactional
    public int markOverdueRentals(LocalDate today) {
        LocalDate checkDate = today != null ? today : LocalDate.now(clock);
        List<Rental> overdue = overdueProcessor.findOverdueRentals(
                rentalRepository.findByStatus(RentalStatus.ACTIVE), checkDate);
        overdue.forEach(rental -> rental.setStatus(RentalStatus.OVERDUE));
        return overdue.size();
    }

    // отменить можно только бронь, которую ещё не выдали (BOOKED)
    @Transactional
    public void cancelBooking(Long rentalId) {
        Rental rental = getRentalOrThrow(rentalId);
        if (rental.getStatus() != RentalStatus.BOOKED) {
            throw new IllegalStateException(
                    "Only BOOKED rentals can be cancelled, current status: " + rental.getStatus());
        }
        rentalRepository.delete(rental);
    }

    // любые фильтры можно не указывать (null), остальные комбинируются через AND
    public List<Rental> search(Long customerId, Long equipmentId, RentalStatus status) {
        Specification<Rental> spec = (root, query, cb) -> cb.conjunction();
        if (customerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId));
        }
        if (equipmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("equipment").get("id"), equipmentId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return rentalRepository.findAll(spec);
    }

    public Optional<Rental> getById(Long id) {
        return rentalRepository.findById(id);
    }

    private Rental getRentalOrThrow(Long rentalId) {
        return rentalRepository.findById(rentalId)
                .orElseThrow(() -> new NoSuchElementException("Rental not found: " + rentalId));
    }
}

