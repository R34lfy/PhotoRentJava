package dev.rental.photorent;

import dev.rental.photorent.model.AdminUser;
import dev.rental.photorent.model.CustomerUser;
import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCategory;
import dev.rental.photorent.model.EquipmentCondition;
import dev.rental.photorent.model.EquipmentModel;
import dev.rental.photorent.model.EquipmentStatus;
import dev.rental.photorent.model.ManagerUser;
import dev.rental.photorent.model.Rental;
import dev.rental.photorent.model.RentalStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

// фабрики тестовых объектов (id у сущностей выдаёт БД, поэтому в тестах выставляем его через рефлексию)
public final class TestData {

    private TestData() {
    }

    public static EquipmentCategory cameraCategory() {
        return new EquipmentCategory("CAM", "Камеры");
    }

    public static EquipmentModel model(Long id, String brand, String name) {
        EquipmentModel model = new EquipmentModel(
                cameraCategory(), brand, name, "Test model", BigDecimal.valueOf(50));
        ReflectionTestUtils.setField(model, "id", id);
        return model;
    }

    public static Equipment equipment(Long id, EquipmentStatus status, EquipmentCondition condition) {
        Equipment equipment = new Equipment(
                model(1L, "Sony", "A7 IV"), String.format("CAM-%05d", id), status, condition);
        ReflectionTestUtils.setField(equipment, "id", id);
        return equipment;
    }

    public static AdminUser admin(Long id) {
        AdminUser user = new AdminUser("admin" + id, "admin" + id + "@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static ManagerUser manager(Long id) {
        ManagerUser user = new ManagerUser("manager" + id, "manager" + id + "@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static CustomerUser customer(Long id) {
        CustomerUser user = new CustomerUser("customer" + id, "customer" + id + "@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static Rental rental(Long id, Equipment equipment, CustomerUser customer,
                                LocalDate startDate, LocalDate endDate, RentalStatus status) {
        Rental rental = new Rental(equipment, customer, startDate, endDate, status);
        ReflectionTestUtils.setField(rental, "id", id);
        return rental;
    }
}
