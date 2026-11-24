package dev.rental.photorent.dto;

import dev.rental.photorent.model.EquipmentCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Запрос на добавление экземпляра техники")
public record EquipmentCreateRequest(
        @Schema(description = "Идентификатор модели техники", example = "1")
        @NotNull Long modelId,
        @Schema(description = "Начальное состояние (списанным быть не может)", example = "NEW")
        @NotNull EquipmentCondition condition) {
}
