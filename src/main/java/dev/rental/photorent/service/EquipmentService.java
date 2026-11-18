package dev.rental.photorent.service;

import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCategory;
import dev.rental.photorent.model.EquipmentCondition;
import dev.rental.photorent.model.EquipmentModel;
import dev.rental.photorent.model.EquipmentNote;
import dev.rental.photorent.model.EquipmentStatus;
import dev.rental.photorent.model.RentalStatus;
import dev.rental.photorent.model.User;
import dev.rental.photorent.model.UserRole;
import dev.rental.photorent.repository.EquipmentCategoryRepository;
import dev.rental.photorent.repository.EquipmentModelRepository;
import dev.rental.photorent.repository.EquipmentNoteRepository;
import dev.rental.photorent.repository.EquipmentRepository;
import dev.rental.photorent.repository.RentalRepository;
import dev.rental.photorent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)

public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentModelRepository modelRepository;
    private final EquipmentCategoryRepository categoryRepository;
    private final EquipmentNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final RentalRepository rentalRepository;

    @Transactional
    public EquipmentModel createModel(String categoryPrefix, String brand, String name,
                                      String description, BigDecimal dailyRate) {
        if (brand == null || brand.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Brand and name must not be blank");
        }
        if (dailyRate == null || dailyRate.signum() < 0) {
            throw new IllegalArgumentException("Daily rate must not be negative");
        }
        String cleanBrand = brand.trim();
        String cleanName = name.trim();

        EquipmentCategory category = categoryRepository.findById(categoryPrefix)
                .orElseThrow(() -> new NoSuchElementException("Category not found: " + categoryPrefix));
        if (modelRepository.existsByBrandIgnoreCaseAndNameIgnoreCase(cleanBrand, cleanName)) {
            throw new IllegalStateException("Model already exists: " + cleanBrand + " " + cleanName);
        }
        return modelRepository.save(new EquipmentModel(category, cleanBrand, cleanName, description, dailyRate));
    }

    @Transactional
    public Equipment addEquipment(Long modelId, EquipmentCondition condition) {
        if (condition == null || condition == EquipmentCondition.DECOMMISSIONED) {
            throw new IllegalArgumentException("New equipment cannot be decommissioned");
        }
        EquipmentModel model = modelRepository.findById(modelId)
                .orElseThrow(() -> new NoSuchElementException("Equipment model not found: " + modelId));

        String serialNumber = nextSerialNumber(model.getCategory().getSerialPrefix());
        return equipmentRepository.save(
                new Equipment(model, serialNumber, EquipmentStatus.AVAILABLE, condition));
    }

    public Optional<Equipment> getById(Long id) {
        return equipmentRepository.findById(id);
    }

    public List<Equipment> getByCategory(String serialPrefix) {
        return equipmentRepository.findByModelCategorySerialPrefix(serialPrefix);
    }

    public List<Equipment> searchByName(String namePart) {
        return equipmentRepository.findByModelNameContainingIgnoreCase(namePart);
    }

    public List<Equipment> getAll() {
        return equipmentRepository.findAll();
    }

    // среди подходящих экземпляров выбираем лучший по состоянию (порядок enum: NEW лучше всех)
    public Optional<Equipment> findBestAvailableByName(String namePart) {
        return equipmentRepository
                .findByModelNameContainingIgnoreCaseAndStatus(namePart, EquipmentStatus.AVAILABLE)
                .stream()
                .filter(Equipment::isAvailableForRental)
                .min(Comparator.comparing(Equipment::getCondition));
    }

    @Transactional
    public boolean removeById(Long id) {
        if (!equipmentRepository.existsById(id)) {
            return false;
        }
        equipmentRepository.deleteById(id);
        return true;
    }

    @Transactional
    public void updateCondition(Long equipmentId, EquipmentCondition newCondition,
                                Long staffId, String reason) {
        if (newCondition == null) {
            throw new IllegalArgumentException("New condition must be specified");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason must be provided when changing equipment condition");
        }
        // блокируем строку техники (параллельная бронь и списание идут по очереди)
        Equipment equipment = equipmentRepository.findByIdForUpdate(equipmentId)
                .orElseThrow(() -> new NoSuchElementException("Equipment not found: " + equipmentId));
        User staff = findStaff(staffId);

        EquipmentCondition previous = equipment.getCondition();
        if (previous == newCondition) {
            throw new IllegalArgumentException("Equipment is already in condition " + previous);
        }
        if (previous == EquipmentCondition.DECOMMISSIONED) {
            throw new IllegalStateException("Decommissioned equipment cannot be restored");
        }
        if (newCondition == EquipmentCondition.DECOMMISSIONED) {
            if (staff.getRole() != UserRole.ADMIN) {
                throw new IllegalStateException("Only an administrator can decommission equipment");
            }
            if (rentalRepository.existsByEquipmentIdAndStatusIn(equipmentId, RentalStatus.OPEN_STATUSES)) {
                throw new IllegalStateException(
                        "Cannot decommission equipment with unfinished rentals: " + equipment.getSerialNumber());
            }
        }

        equipment.setCondition(newCondition);
        if (newCondition == EquipmentCondition.DECOMMISSIONED) {
            equipment.setStatus(EquipmentStatus.RETIRED);
        }

        String text = String.format("Condition changed from %s to %s. Reason: %s",
                previous, newCondition, reason.trim());
        noteRepository.save(new EquipmentNote(equipment, staff, text));
    }
    @Transactional
    public EquipmentNote addNote(Long equipmentId, Long authorId, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Note text must not be blank");
        }
        Equipment equipment = findEquipment(equipmentId);
        User author = findStaff(authorId);
        return noteRepository.save(new EquipmentNote(equipment, author, text.trim()));
    }

    public List<EquipmentNote> getNotesFor(Long equipmentId) {
        return noteRepository.findByEquipmentIdOrderByCreatedAtDesc(equipmentId);
    }

    private String nextSerialNumber(String prefix) {
        categoryRepository.incrementSerialCounter(prefix);
        int number = categoryRepository.findSerialCounter(prefix)
                .orElseThrow(() -> new NoSuchElementException("Category not found: " + prefix));
        return String.format("%s-%05d", prefix, number);
    }

    private Equipment findEquipment(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Equipment not found: " + id));
    }

    private User findStaff(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
        if (user.getRole() == UserRole.CUSTOMER) {
            throw new IllegalStateException("Only staff can change equipment records");
        }
        return user;
    }
}

