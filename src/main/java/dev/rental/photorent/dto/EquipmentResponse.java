package dev.rental.photorent.dto;

import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCondition;
import dev.rental.photorent.model.EquipmentModel;
import dev.rental.photorent.model.EquipmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Экземпляр техники вместе с данными его модели")
public record EquipmentResponse(
        @Schema(description = "Идентификатор экземпляра", example = "1") Long id,
        @Schema(description = "Идентификатор модели", example = "1") Long modelId,
        @Schema(description = "Бренд", example = "Sony") String brand,
        @Schema(description = "Название модели", example = "A7 IV") String modelName,
        @Schema(description = "Префикс категории", example = "CAM") String categoryPrefix,
        @Schema(description = "Серийный номер (складской)", example = "CAM-00001") String serialNumber,
        @Schema(description = "Текущий статус", example = "AVAILABLE") EquipmentStatus status,
        @Schema(description = "Физическое состояние", example = "NEW") EquipmentCondition condition,
        @Schema(description = "Тариф за сутки", example = "50.00") BigDecimal dailyRate) {

    public static EquipmentResponse from(Equipment equipment) {
        EquipmentModel model = equipment.getModel();
        return new EquipmentResponse(equipment.getId(), model.getId(), model.getBrand(), model.getName(),
                model.getCategory().getSerialPrefix(), equipment.getSerialNumber(),
                equipment.getStatus(), equipment.getCondition(), model.getDailyRate());
    }
}
