package dev.rental.photorent.repository;

import dev.rental.photorent.model.Rental;
import dev.rental.photorent.model.RentalStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryRentalRepository implements RentalRepository {

    private final Map<Long, Rental> storage = new ConcurrentHashMap<>();

    @Override
    public Rental add(Rental rental) {
        storage.put(rental.getId(), rental);
        return rental;
    }

    @Override
    public Optional<Rental> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Rental> findByCustomerId(Long customerId) {
        return storage.values().stream()
                .filter(rental -> rental.getCustomer().getId().equals(customerId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Rental> findByEquipmentId(Long equipmentId) {
        return storage.values().stream()
                .filter(rental -> rental.getEquipment().getId().equals(equipmentId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Rental> findByStatus(RentalStatus status) {
        return storage.values().stream()
                .filter(rental -> rental.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Rental> findAll() {
        return List.copyOf(storage.values());
    }

    @Override
    public boolean deleteById(Long id) {
        return storage.remove(id) != null;
    }
}

