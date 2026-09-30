package dev.rental.photorent.service;

import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCategory;
import dev.rental.photorent.model.EquipmentCondition;
import dev.rental.photorent.model.EquipmentStatus;
import dev.rental.photorent.model.EquipmentNote;
import dev.rental.photorent.repository.EquipmentRepository;
import dev.rental.photorent.repository.EquipmentNoteRepository;
import dev.rental.photorent.repository.InMemoryEquipmentRepository;
import dev.rental.photorent.repository.InMemoryEquipmentNoteRepository;

import dev.rental.photorent.model.User;
import dev.rental.photorent.model.AdminUser;
import dev.rental.photorent.model.ManagerUser;
import dev.rental.photorent.model.CustomerUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EquipmentServiceTest {

    private EquipmentService equipmentService;
    private AdminUser admin;
    private ManagerUser manager;

    @BeforeEach
    void setUp() {
        EquipmentRepository equipmentRepository = new InMemoryEquipmentRepository();
        EquipmentNoteRepository noteRepository = new InMemoryEquipmentNoteRepository();
        equipmentService = new EquipmentService(equipmentRepository, noteRepository);
        admin = new AdminUser(1L, "admin1", "admin1@example.com");
        manager = new ManagerUser(2L, "manager1", "manager1@example.com");
    }

    @Test
    void shouldFindBestAvailableEquipmentAmongDuplicates() {
        equipmentService.addEquipment(createCamera(1L, "Sony A7 IV", EquipmentCondition.WORN));
        equipmentService.addEquipment(createCamera(2L, "Sony A7 IV", EquipmentCondition.NEW));
        equipmentService.addEquipment(createCamera(3L, "Sony A7 IV", EquipmentCondition.GOOD));

        Optional<Equipment> best = equipmentService.findBestAvailableByName("Sony A7 IV");

        assertThat(best).isPresent();
        assertThat(best.get().getId()).isEqualTo(2L);
        assertThat(best.get().getCondition()).isEqualTo(EquipmentCondition.NEW);
    }

    @Test
    void shouldNotSuggestEquipmentThatNeedsRepair() {
        Equipment brokenCamera = createCamera(1L, "Sony A7 IV", EquipmentCondition.NEEDS_REPAIR);
        equipmentService.addEquipment(brokenCamera);

        Optional<Equipment> best = equipmentService.findBestAvailableByName("Sony A7 IV");

        assertThat(best).isEmpty();
    }

    @Test
    void shouldUpdateConditionAndCreateAuditNote() {
        Equipment camera = createCamera(1L, "Sony A7 IV", EquipmentCondition.WORN);
        equipmentService.addEquipment(camera);

        equipmentService.updateCondition(1L, EquipmentCondition.GOOD, manager, "Customer cleaned the battery latch");

        assertThat(camera.getCondition()).isEqualTo(EquipmentCondition.GOOD);
        List<EquipmentNote> notes = equipmentService.getNotesFor(1L);
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).getText()).contains("WORN", "GOOD", "Customer cleaned the battery latch");
    }

    @Test
    void shouldRejectConditionChangeWithoutReason() {
        Equipment camera = createCamera(1L, "Sony A7 IV", EquipmentCondition.WORN);
        equipmentService.addEquipment(camera);

        assertThatThrownBy(() -> equipmentService.updateCondition(1L, EquipmentCondition.GOOD, manager, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectDecommissioningByNonAdmin() {
        Equipment camera = createCamera(1L, "Sony A7 IV", EquipmentCondition.NEEDS_REPAIR);
        equipmentService.addEquipment(camera);

        assertThatThrownBy(() -> equipmentService.updateCondition(
                1L, EquipmentCondition.DECOMMISSIONED, manager, "Motor completely burned out"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldAllowDecommissioningByAdmin() {
        Equipment camera = createCamera(1L, "Sony A7 IV", EquipmentCondition.NEEDS_REPAIR);
        equipmentService.addEquipment(camera);

        equipmentService.updateCondition(1L, EquipmentCondition.DECOMMISSIONED, admin, "Motor completely burned out");

        assertThat(camera.getCondition()).isEqualTo(EquipmentCondition.DECOMMISSIONED);
        assertThat(camera.getStatus()).isEqualTo(EquipmentStatus.RETIRED);
    }

    @Test
    void shouldGenerateSequentialSerialNumbersPerCategory() {
        String first = equipmentService.generateSerialNumber(EquipmentCategory.CAMERA);
        String second = equipmentService.generateSerialNumber(EquipmentCategory.CAMERA);
        String firstLens = equipmentService.generateSerialNumber(EquipmentCategory.LENS);

        assertThat(first).isEqualTo("CAM-00001");
        assertThat(second).isEqualTo("CAM-00002");
        assertThat(firstLens).isEqualTo("LEN-00001");
    }

    private Equipment createCamera(Long id, String name, EquipmentCondition condition) {
        return new Equipment(id, name, "Sony", "Full-frame mirrorless",
                EquipmentCategory.CAMERA, "CAM-" + id, BigDecimal.valueOf(50),
                EquipmentStatus.AVAILABLE, condition);
    }
}

