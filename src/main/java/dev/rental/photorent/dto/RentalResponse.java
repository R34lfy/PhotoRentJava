package dev.rental.photorent.dto;

import dev.rental.photorent.model.Rental;
import dev.rental.photorent.model.RentalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Аренда")
public record RentalResponse(
        @Schema(description = "Идентификатор аренды", example = "10") Long id,
        @Schema(description = "Идентификатор техники", example = "1") Long equipmentId,
        @Schema(description = "Серийный номер техники", example = "CAM-00001") String serialNumber,
        @Schema(description = "Идентификатор клиента", example = "3") Long customerId,
        @Schema(description = "Логин клиента", example = "john_doe") String customerUsername,
        @Schema(description = "Дата начала", example = "2026-12-01") LocalDate startDate,
        @Schema(description = "Дата окончания включительно", example = "2026-12-05") LocalDate endDate,
        @Schema(description = "Статус аренды", example = "BOOKED") RentalStatus status) {

    public static RentalResponse from(Rental rental) {
        return new RentalResponse(rental.getId(), rental.getEquipment().getId(),
                rental.getEquipment().getSerialNumber(), rental.getCustomer().getId(),
                rental.getCustomer().getUsername(), rental.getStartDate(), rental.getEndDate(),
                rental.getStatus());
    }
}
