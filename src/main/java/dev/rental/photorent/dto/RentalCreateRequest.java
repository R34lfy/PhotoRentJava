package dev.rental.photorent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Запрос на бронирование техники")
public record RentalCreateRequest(
        @Schema(description = "Идентификатор экземпляра техники", example = "1")
        @NotNull Long equipmentId,
        @Schema(description = "Идентификатор клиента (пользователь с ролью CUSTOMER)", example = "3")
        @NotNull Long customerId,
        @Schema(description = "Дата начала, не раньше сегодняшней", example = "2026-12-01")
        @NotNull LocalDate startDate,
        @Schema(description = "Дата окончания включительно, не раньше даты начала", example = "2026-12-05")
        @NotNull LocalDate endDate) {
}
