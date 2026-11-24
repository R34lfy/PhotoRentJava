package dev.rental.photorent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Запрос на создание модели техники")
public record ModelCreateRequest(
        @Schema(description = "Префикс категории", example = "CAM")
        @NotBlank String categoryPrefix,
        @Schema(description = "Бренд", example = "Sony")
        @NotBlank @Size(max = 50) String brand,
        @Schema(description = "Название модели (пара бренд + название уникальна без учёта регистра)", example = "A7 IV")
        @NotBlank @Size(max = 100) String name,
        @Schema(description = "Описание, необязательное", example = "Полнокадровая беззеркальная камера")
        String description,
        // колонка daily_rate в БД: numeric(10,2), то есть до 8 цифр до запятой и 2 после
        @Schema(description = "Тариф за сутки: не меньше нуля, до 8 цифр до запятой и 2 после", example = "50.00")
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal dailyRate) {
}
