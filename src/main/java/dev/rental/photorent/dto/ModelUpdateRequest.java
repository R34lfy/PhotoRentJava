package dev.rental.photorent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Schema(description = "Запрос на изменение описания и тарифа модели")
public record ModelUpdateRequest(
        @Schema(description = "Новое описание", example = "Полнокадровая беззеркальная камера")
        String description,
        @Schema(description = "Новый тариф за сутки: не меньше нуля, до 8 цифр до запятой и 2 после", example = "60.00")
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal dailyRate) {
}
