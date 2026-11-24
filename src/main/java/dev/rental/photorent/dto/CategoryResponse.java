package dev.rental.photorent.dto;

import dev.rental.photorent.model.EquipmentCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Категория техники")
public record CategoryResponse(
        @Schema(description = "Префикс категории, он же первичный ключ", example = "CAM") String serialPrefix,
        @Schema(description = "Название категории", example = "Камеры") String name) {

    public static CategoryResponse from(EquipmentCategory category) {
        return new CategoryResponse(category.getSerialPrefix(), category.getName());
    }
}
