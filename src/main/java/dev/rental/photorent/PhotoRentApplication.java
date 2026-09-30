package dev.rental.photorent;

import dev.rental.photorent.concurrent.RentalOverdueProcessor;
import dev.rental.photorent.model.*;
import dev.rental.photorent.repository.*;

import dev.rental.photorent.service.EquipmentService;
import dev.rental.photorent.service.RentalService;
import dev.rental.photorent.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@SpringBootApplication
public class PhotoRentApplication {

    private static final Logger log = LoggerFactory.getLogger(PhotoRentApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PhotoRentApplication.class, args);
    }

    @Bean
    CommandLineRunner demo() {
        return args -> {
            log.info("=== PhotoRental demo run ===");

            EquipmentRepository equipmentRepository = new InMemoryEquipmentRepository();
            UserRepository userRepository = new InMemoryUserRepository();
            RentalRepository rentalRepository = new InMemoryRentalRepository();
            EquipmentNoteRepository noteRepository = new InMemoryEquipmentNoteRepository();

            EquipmentService equipmentService = new EquipmentService(equipmentRepository, noteRepository);
            UserService userService = new UserService(userRepository);
            RentalService rentalService = new RentalService(rentalRepository, equipmentRepository);

            AdminUser admin = new AdminUser(1L, "admin1", "admin1@example.com");
            ManagerUser manager = new ManagerUser(2L, "manager1", "manager1@example.com");
            CustomerUser customer = new CustomerUser(3L, "john_doe", "john@example.com");

            userService.register(admin);
            userService.register(manager);
            userService.register(customer);

            log.info("Registered users: {}", userService.getAll());

            Equipment camera1 = new Equipment(1L, "Sony A7 IV", "Sony", "Full-frame mirrorless",
                    EquipmentCategory.CAMERA, equipmentService.generateSerialNumber(EquipmentCategory.CAMERA),
                    BigDecimal.valueOf(50), EquipmentStatus.AVAILABLE, EquipmentCondition.WORN);
            Equipment camera2 = new Equipment(2L, "Sony A7 IV", "Sony", "Full-frame mirrorless",
                    EquipmentCategory.CAMERA, equipmentService.generateSerialNumber(EquipmentCategory.CAMERA),
                    BigDecimal.valueOf(50), EquipmentStatus.AVAILABLE, EquipmentCondition.NEW);

            equipmentService.addEquipment(camera1);
            equipmentService.addEquipment(camera2);

            log.info("Added equipment: {}", equipmentService.getAll());

            equipmentService.findBestAvailableByName("Sony A7 IV")
                    .ifPresent(best -> log.info("Best available Sony A7 IV: {}", best));

            Rental rental = rentalService.bookRental(1L, camera2, customer,
                    LocalDate.now(), LocalDate.now().plusDays(5));
            log.info("Booked rental: {}", rental);

            rentalService.issueEquipment(rental.getId());
            log.info("Issued equipment, rental status: {}, equipment status: {}",
                    rental.getStatus(), camera2.getStatus());

            rentalService.extendRental(rental.getId(), LocalDate.now().plusDays(8));
            log.info("Extended rental, new end date: {}", rental.getEndDate());

            equipmentService.updateCondition(camera1.getId(), EquipmentCondition.GOOD, manager,
                    "Customer cleaned the battery latch mechanism before return");
            log.info("Updated condition for camera1: {}, notes: {}",
                    camera1.getCondition(), equipmentService.getNotesFor(camera1.getId()));

            RentalOverdueProcessor processor = new RentalOverdueProcessor(2);
            List<Rental> activeRentals = rentalService.getActiveRentals();
            processor.processOverdueRentals(activeRentals, LocalDate.now().plusDays(20));

            log.info("Processed overdue check, rental status now: {}", rental.getStatus());

            log.info("=== Demo run finished ===");
        };
    }
}

