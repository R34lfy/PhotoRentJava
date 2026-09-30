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
import java.util.Optional;
import java.util.Scanner;

@SpringBootApplication
public class PhotoRentApplication {

    private static final Logger log = LoggerFactory.getLogger(PhotoRentApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PhotoRentApplication.class, args);
    }

    @Bean
    CommandLineRunner demo() {
        return args -> {
            EquipmentRepository equipmentRepository = new InMemoryEquipmentRepository();
            UserRepository userRepository = new InMemoryUserRepository();
            RentalRepository rentalRepository = new InMemoryRentalRepository();
            EquipmentNoteRepository noteRepository = new InMemoryEquipmentNoteRepository();

            EquipmentService equipmentService = new EquipmentService(equipmentRepository, noteRepository);
            UserService userService = new UserService(userRepository);
            RentalService rentalService = new RentalService(rentalRepository, equipmentRepository);

            seedData(userService, equipmentService);

            Scanner scanner = new Scanner(System.in);
            boolean running = true;

            while (running) {
                printMenu();
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1" -> listEquipment(equipmentService);
                    case "2" -> bookRental(scanner, rentalService, equipmentService, userService);
                    case "3" -> issueEquipment(scanner, rentalService);
                    case "4" -> returnEquipment(scanner, rentalService);
                    case "5" -> extendRental(scanner, rentalService);
                    case "6" -> showNotes(scanner, equipmentService);
                    case "7" -> runOverdueCheck(scanner, rentalService);
                    case "0" -> running = false;
                    default -> System.out.println("Неизвестная команда, попробуй снова");
                }
            }

            System.out.println("Завершение работы демо.");
        };
    }

    private void printMenu() {
        System.out.println("""
                
                === PhotoRental Demo ===
                1. Показать всю технику
                2. Забронировать технику
                3. Выдать технику клиенту (по id аренды)
                4. Принять технику обратно (по id аренды)
                5. Продлить аренду (по id аренды)
                6. Показать заметки по технике (по id техники)
                7. Проверить просрочки (с указанием "сегодняшней" даты)
                Выбор:\s""");
    }

    private void seedData(UserService userService, EquipmentService equipmentService) {
        userService.register(new AdminUser(1L, "admin1", "admin1@example.com"));
        userService.register(new ManagerUser(2L, "manager1", "manager1@example.com"));
        userService.register(new CustomerUser(3L, "john_doe", "john@example.com"));

        Equipment camera1 = new Equipment(1L, "Sony A7 IV", "Sony", "Full-frame mirrorless",
                EquipmentCategory.CAMERA, equipmentService.generateSerialNumber(EquipmentCategory.CAMERA),
                BigDecimal.valueOf(50), EquipmentStatus.AVAILABLE, EquipmentCondition.WORN);
        Equipment camera2 = new Equipment(2L, "Sony A7 IV", "Sony", "Full-frame mirrorless",
                EquipmentCategory.CAMERA, equipmentService.generateSerialNumber(EquipmentCategory.CAMERA),
                BigDecimal.valueOf(50), EquipmentStatus.AVAILABLE, EquipmentCondition.NEW);

        equipmentService.addEquipment(camera1);
        equipmentService.addEquipment(camera2);

        System.out.println("Тестовые данные загружены: 3 пользователя (id 1=admin, 2=manager, 3=customer), 2 камеры (id 1, id 2)");
    }

    private void listEquipment(EquipmentService equipmentService) {
        equipmentService.getAll().forEach(System.out::println);
    }

    private void bookRental(Scanner scanner, RentalService rentalService,
                            EquipmentService equipmentService, UserService userService) {
        System.out.print("id техники: ");
        Long equipmentId = Long.parseLong(scanner.nextLine().trim());
        System.out.print("id клиента: ");
        Long customerId = Long.parseLong(scanner.nextLine().trim());
        System.out.print("дата начала (yyyy-MM-dd): ");
        LocalDate start = LocalDate.parse(scanner.nextLine().trim());
        System.out.print("дата окончания (yyyy-MM-dd): ");
        LocalDate end = LocalDate.parse(scanner.nextLine().trim());

        Optional<Equipment> equipment = equipmentService.getById(equipmentId);
        Optional<User> user = userService.getById(customerId);

        if (equipment.isEmpty() || user.isEmpty() || !(user.get() instanceof CustomerUser customer)) {
            System.out.println("Техника или клиент не найдены (или id не клиента)");
            return;
        }

        try {
            Rental rental = rentalService.bookRental(System.nanoTime(), equipment.get(), customer, start, end);
            System.out.println("Бронь создана: " + rental);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void issueEquipment(Scanner scanner, RentalService rentalService) {
        System.out.print("id аренды: ");
        Long rentalId = Long.parseLong(scanner.nextLine().trim());
        try {
            rentalService.issueEquipment(rentalId);
            System.out.println("Техника выдана");
        } catch (IllegalStateException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void returnEquipment(Scanner scanner, RentalService rentalService) {
        System.out.print("id аренды: ");
        Long rentalId = Long.parseLong(scanner.nextLine().trim());
        try {
            rentalService.returnEquipment(rentalId);
            System.out.println("Техника принята обратно");
        } catch (IllegalStateException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void extendRental(Scanner scanner, RentalService rentalService) {
        System.out.print("id аренды: ");
        Long rentalId = Long.parseLong(scanner.nextLine().trim());
        System.out.print("новая дата окончания (yyyy-MM-dd): ");
        LocalDate newEndDate = LocalDate.parse(scanner.nextLine().trim());
        try {
            rentalService.extendRental(rentalId, newEndDate);
            System.out.println("Аренда продлена");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void showNotes(Scanner scanner, EquipmentService equipmentService) {
        System.out.print("id техники: ");
        Long equipmentId = Long.parseLong(scanner.nextLine().trim());
        equipmentService.getNotesFor(equipmentId).forEach(System.out::println);
    }

    private void runOverdueCheck(Scanner scanner, RentalService rentalService) {
        System.out.print("\"сегодняшняя\" дата для проверки (yyyy-MM-dd): ");
        LocalDate today = LocalDate.parse(scanner.nextLine().trim());

        List<Rental> activeRentals = rentalService.getActiveRentals();
        RentalOverdueProcessor processor = new RentalOverdueProcessor(2);
        processor.processOverdueRentals(activeRentals, today);

        System.out.println("Проверка завершена, статусы активных аренд:");
        activeRentals.forEach(r -> System.out.println(r.getId() + " -> " + r.getStatus()));
    }
}

