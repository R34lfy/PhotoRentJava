package dev.rental.photorent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Запрос на продление аренды")
public record RentalExtendRequest(
        @Schema(description = "Новая дата окончания, позже текущей", example = "2026-12-08")
        @NotNull LocalDate newEndDate) {
}
