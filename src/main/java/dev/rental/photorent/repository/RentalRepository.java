package dev.rental.photorent.repository;

import dev.rental.photorent.model.Rental;
import dev.rental.photorent.model.RentalStatus;

import java.util.List;
import java.util.Optional;

public interface RentalRepository {

    Rental add(Rental rental);

    Optional<Rental> findById(Long id);

    List<Rental> findByCustomerId(Long customerId);

    List<Rental> findByEquipmentId(Long equipmentId);

    List<Rental> findByStatus(RentalStatus status);

    List<Rental> findAll();

    boolean deleteById(Long id);
}

