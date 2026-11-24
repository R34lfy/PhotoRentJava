package dev.rental.photorent.service;

import dev.rental.photorent.TestData;
import dev.rental.photorent.model.AdminUser;
import dev.rental.photorent.model.CustomerUser;
import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCategory;
import dev.rental.photorent.model.EquipmentCondition;
import dev.rental.photorent.model.EquipmentModel;
import dev.rental.photorent.model.EquipmentNote;
import dev.rental.photorent.model.EquipmentStatus;
import dev.rental.photorent.model.ManagerUser;
import dev.rental.photorent.model.RentalStatus;
import dev.rental.photorent.model.User;
import dev.rental.photorent.repository.EquipmentCategoryRepository;
import dev.rental.photorent.repository.EquipmentModelRepository;
import dev.rental.photorent.repository.EquipmentNoteRepository;
import dev.rental.photorent.repository.EquipmentRepository;
import dev.rental.photorent.repository.RentalRepository;
import dev.rental.photorent.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = EquipmentService.class)
class EquipmentServiceTest {

    @Autowired
    private EquipmentService equipmentService;

    @MockitoBean
    private EquipmentRepository equipmentRepository;
    @MockitoBean
    private EquipmentModelRepository modelRepository;
    @MockitoBean
    private EquipmentCategoryRepository categoryRepository;
    @MockitoBean
    private EquipmentNoteRepository noteRepository;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private RentalRepository rentalRepository;

    private final AdminUser admin = TestData.admin(1L);
    private final ManagerUser manager = TestData.manager(2L);

    // подбор лучшего экземпляра

    @Test
    void shouldFindBestAvailableEquipmentAmongDuplicates() {
        Equipment worn = TestData.equipment(1L, EquipmentStatus.AVAILABLE, EquipmentCondition.WORN);
        Equipment brandNew = TestData.equipment(2L, EquipmentStatus.AVAILABLE, EquipmentCondition.NEW);
        Equipment good = TestData.equipment(3L, EquipmentStatus.AVAILABLE, EquipmentCondition.GOOD);
        when(equipmentRepository.findByModelNameContainingIgnoreCaseAndStatus("A7", EquipmentStatus.AVAILABLE))
                .thenReturn(List.of(worn, brandNew, good));

        Optional<Equipment> best = equipmentService.findBestAvailableByName("A7");

        assertThat(best).contains(brandNew);
    }

    @Test
    void shouldSkipEquipmentThatNeedsRepairWhenChoosingBest() {
        Equipment broken = TestData.equipment(1L, EquipmentStatus.AVAILABLE, EquipmentCondition.NEEDS_REPAIR);
        Equipment worn = TestData.equipment(2L, EquipmentStatus.AVAILABLE, EquipmentCondition.WORN);
        when(equipmentRepository.findByModelNameContainingIgnoreCaseAndStatus("A7", EquipmentStatus.AVAILABLE))
                .thenReturn(List.of(broken, worn));

        assertThat(equipmentService.findBestAvailableByName("A7")).contains(worn);
    }

    @Test
    void shouldReturnEmptyWhenNothingIsAvailable() {
        assertThat(equipmentService.findBestAvailableByName("A7")).isEmpty();
    }

    // модели и экземпляры

    @Test
    void shouldCreateModelWithTrimmedFields() {
        when(categoryRepository.findById("CAM")).thenReturn(Optional.of(TestData.cameraCategory()));
        when(modelRepository.save(any(EquipmentModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EquipmentModel model = equipmentService.createModel(
                "CAM", " Sony ", " A7 IV ", "Full-frame", BigDecimal.valueOf(50));

        assertThat(model.getBrand()).isEqualTo("Sony");
        assertThat(model.getName()).isEqualTo("A7 IV");
    }

    @Test
    void shouldRejectDuplicateModel() {
        when(categoryRepository.findById("CAM")).thenReturn(Optional.of(TestData.cameraCategory()));
        when(modelRepository.existsByBrandIgnoreCaseAndNameIgnoreCase("Sony", "A7 IV")).thenReturn(true);

        assertThatThrownBy(() -> equipmentService.createModel(
                "CAM", "Sony", "A7 IV", "Full-frame", BigDecimal.valueOf(50)))
                .isInstanceOf(IllegalStateException.class);
        verify(modelRepository, never()).save(any(EquipmentModel.class));
    }

    @Test
    void shouldRejectNegativeDailyRate() {
        assertThatThrownBy(() -> equipmentService.createModel(
                "CAM", "Sony", "A7 IV", "Full-frame", BigDecimal.valueOf(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldGenerateSerialNumberFromCategoryCounter() {
        when(modelRepository.findById(1L)).thenReturn(Optional.of(TestData.model(1L, "Sony", "A7 IV")));
        when(categoryRepository.findSerialCounter("CAM")).thenReturn(Optional.of(7));
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Equipment created = equipmentService.addEquipment(1L, EquipmentCondition.NEW);

        assertThat(created.getSerialNumber()).isEqualTo("CAM-00007");
        assertThat(created.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
        verify(categoryRepository).incrementSerialCounter("CAM");
    }

    @Test
    void shouldKeepGrowingSerialNumberPastFiveDigits() {
        when(modelRepository.findById(1L)).thenReturn(Optional.of(TestData.model(1L, "Sony", "A7 IV")));
        when(categoryRepository.findSerialCounter("CAM")).thenReturn(Optional.of(100000));
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Equipment created = equipmentService.addEquipment(1L, EquipmentCondition.NEW);

        assertThat(created.getSerialNumber()).isEqualTo("CAM-100000");
    }

    @Test
    void shouldRejectAddingDecommissionedEquipment() {
        assertThatThrownBy(() -> equipmentService.addEquipment(1L, EquipmentCondition.DECOMMISSIONED))
                .isInstanceOf(IllegalArgumentException.class);
        verify(equipmentRepository, never()).save(any(Equipment.class));
    }

    @Test
    void shouldRejectAddingEquipmentOfUnknownModel() {
        assertThatThrownBy(() -> equipmentService.addEquipment(99L, EquipmentCondition.NEW))
                .isInstanceOf(NoSuchElementException.class);
    }

    // изменение состояния

    @Test
    void shouldUpdateConditionAndWriteAuditNote() {
        Equipment camera = storedEquipment(EquipmentStatus.AVAILABLE, EquipmentCondition.WORN);
        staffLogsIn(manager);

        equipmentService.updateCondition(1L, EquipmentCondition.GOOD, 2L, "Customer cleaned the battery latch");

        assertThat(camera.getCondition()).isEqualTo(EquipmentCondition.GOOD);
        ArgumentCaptor<EquipmentNote> captor = ArgumentCaptor.forClass(EquipmentNote.class);
        verify(noteRepository).save(captor.capture());
        assertThat(captor.getValue().getText()).contains("WORN", "GOOD", "Customer cleaned the battery latch");
        assertThat(captor.getValue().getAuthor()).isEqualTo(manager);
    }

    @Test
    void shouldRejectConditionChangeWithoutReason() {
        Equipment camera = storedEquipment(EquipmentStatus.AVAILABLE, EquipmentCondition.WORN);
        staffLogsIn(manager);

        assertThatThrownBy(() -> equipmentService.updateCondition(1L, EquipmentCondition.GOOD, 2L, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(camera.getCondition()).isEqualTo(EquipmentCondition.WORN);
        verify(noteRepository, never()).save(any(EquipmentNote.class));
    }

    @Test
    void shouldRejectChangeToTheSameCondition() {
        storedEquipment(EquipmentStatus.AVAILABLE, EquipmentCondition.WORN);
        staffLogsIn(manager);

        assertThatThrownBy(() -> equipmentService.updateCondition(1L, EquipmentCondition.WORN, 2L, "No change"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectDecommissioningByNonAdmin() {
        Equipment camera = storedEquipment(EquipmentStatus.AVAILABLE, EquipmentCondition.NEEDS_REPAIR);
        staffLogsIn(manager);

        assertThatThrownBy(() -> equipmentService.updateCondition(
                1L, EquipmentCondition.DECOMMISSIONED, 2L, "Motor burned out"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("administrator");
        assertThat(camera.getCondition()).isEqualTo(EquipmentCondition.NEEDS_REPAIR);
    }

    @Test
    void shouldAllowDecommissioningByAdminWhenNoUnfinishedRentals() {
        Equipment camera = storedEquipment(EquipmentStatus.AVAILABLE, EquipmentCondition.NEEDS_REPAIR);
        staffLogsIn(admin);

        equipmentService.updateCondition(1L, EquipmentCondition.DECOMMISSIONED, 1L, "Motor burned out");

        assertThat(camera.getCondition()).isEqualTo(EquipmentCondition.DECOMMISSIONED);
        assertThat(camera.getStatus()).isEqualTo(EquipmentStatus.RETIRED);
    }

    @Test
    void shouldRejectDecommissioningWhenEquipmentHasUnfinishedRentals() {
        Equipment camera = storedEquipment(EquipmentStatus.AVAILABLE, EquipmentCondition.NEEDS_REPAIR);
        staffLogsIn(admin);
        when(rentalRepository.existsByEquipmentIdAndStatusIn(1L, RentalStatus.OPEN_STATUSES)).thenReturn(true);

        assertThatThrownBy(() -> equipmentService.updateCondition(
                1L, EquipmentCondition.DECOMMISSIONED, 1L, "Motor burned out"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unfinished rentals");
        assertThat(camera.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
        verify(noteRepository, never()).save(any(EquipmentNote.class));
    }

    @Test
    void shouldNotRestoreDecommissionedEquipment() {
        storedEquipment(EquipmentStatus.RETIRED, EquipmentCondition.DECOMMISSIONED);
        staffLogsIn(admin);

        assertThatThrownBy(() -> equipmentService.updateCondition(1L, EquipmentCondition.GOOD, 1L, "Mistake"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectCustomerAsAuthorOfConditionChange() {
        storedEquipment(EquipmentStatus.AVAILABLE, EquipmentCondition.WORN);
        CustomerUser customer = TestData.customer(3L);
        staffLogsIn(customer);

        assertThatThrownBy(() -> equipmentService.updateCondition(1L, EquipmentCondition.GOOD, 3L, "I fixed it"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("staff");
    }

    // заметки

    @Test
    void shouldSaveTrimmedNote() {
        storedEquipment(EquipmentStatus.AVAILABLE, EquipmentCondition.GOOD);
        staffLogsIn(manager);
        when(noteRepository.save(any(EquipmentNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EquipmentNote note = equipmentService.addNote(1L, 2L, "  Scratch on the lens mount ");

        assertThat(note.getText()).isEqualTo("Scratch on the lens mount");
    }

    @Test
    void shouldRejectBlankNote() {
        assertThatThrownBy(() -> equipmentService.addNote(1L, 2L, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- справочники и модели ----

    @Test
    void shouldReturnAllCategories() {
        EquipmentCategory cameras = TestData.cameraCategory();
        when(categoryRepository.findAll()).thenReturn(List.of(cameras));

        assertThat(equipmentService.getCategories()).containsExactly(cameras);
    }

    @Test
    void shouldReturnAllModelsWhenCategoryIsNotGiven() {
        EquipmentModel model = TestData.model(1L, "Sony", "A7 IV");
        when(modelRepository.findAll()).thenReturn(List.of(model));

        assertThat(equipmentService.getModels(null)).containsExactly(model);
        assertThat(equipmentService.getModels(" ")).containsExactly(model);
    }

    @Test
    void shouldNormalizeCategoryPrefixWhenFilteringModels() {
        EquipmentModel model = TestData.model(1L, "Sony", "A7 IV");
        when(modelRepository.findByCategorySerialPrefix("CAM")).thenReturn(List.of(model));

        assertThat(equipmentService.getModels(" cam ")).containsExactly(model);
    }

    @Test
    void shouldUpdateModelDescriptionAndRate() {
        EquipmentModel model = TestData.model(1L, "Sony", "A7 IV");
        when(modelRepository.findById(1L)).thenReturn(Optional.of(model));

        equipmentService.updateModel(1L, "New description", BigDecimal.valueOf(60));

        assertThat(model.getDescription()).isEqualTo("New description");
        assertThat(model.getDailyRate()).isEqualByComparingTo("60");
    }

    @Test
    void shouldRejectNegativeRateWhenUpdatingModel() {
        assertThatThrownBy(() -> equipmentService.updateModel(1L, "desc", BigDecimal.valueOf(-5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectUpdatingUnknownModel() {
        assertThatThrownBy(() -> equipmentService.updateModel(99L, "desc", BigDecimal.TEN))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void shouldRemoveExistingModel() {
        when(modelRepository.existsById(1L)).thenReturn(true);

        assertThat(equipmentService.removeModel(1L)).isTrue();
        verify(modelRepository).deleteById(1L);
    }

    @Test
    void shouldNotRemoveMissingModel() {
        assertThat(equipmentService.removeModel(99L)).isFalse();
        verify(modelRepository, never()).deleteById(any());
    }

    // ---- удаление ----

    @Test
    void shouldRemoveExistingEquipment() {
        when(equipmentRepository.existsById(1L)).thenReturn(true);

        assertThat(equipmentService.removeById(1L)).isTrue();
        verify(equipmentRepository).deleteById(1L);
    }

    @Test
    void shouldNotRemoveMissingEquipment() {
        assertThat(equipmentService.removeById(999L)).isFalse();
        verify(equipmentRepository, never()).deleteById(any());
    }

    private Equipment storedEquipment(EquipmentStatus status, EquipmentCondition condition) {
        Equipment equipment = TestData.equipment(1L, status, condition);
        when(equipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(equipment));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment));
        return equipment;
    }

    private void staffLogsIn(User staff) {
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
    }
}
