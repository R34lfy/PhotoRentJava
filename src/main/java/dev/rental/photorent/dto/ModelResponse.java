package dev.rental.photorent.dto;

import dev.rental.photorent.model.EquipmentModel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Модель техники")
public record ModelResponse(
        @Schema(description = "Идентификатор модели", example = "1") Long id,
        @Schema(description = "Префикс категории", example = "CAM") String categoryPrefix,
        @Schema(description = "Бренд", example = "Sony") String brand,
        @Schema(description = "Название модели", example = "A7 IV") String name,
        @Schema(description = "Описание", example = "Полнокадровая беззеркальная камера") String description,
        @Schema(description = "Тариф за сутки", example = "50.00") BigDecimal dailyRate) {

    public static ModelResponse from(EquipmentModel model) {
        return new ModelResponse(model.getId(), model.getCategory().getSerialPrefix(),
                model.getBrand(), model.getName(), model.getDescription(), model.getDailyRate());
    }
}
