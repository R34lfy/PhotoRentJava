package dev.rental.photorent.dto;

import dev.rental.photorent.model.EquipmentCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// staffId пока передаётся в теле запроса, после ЛР с авторизацией его можно брать из текущего пользователя
@Schema(description = "Запрос на смену состояния техники")
public record ConditionUpdateRequest(
        @Schema(description = "Новое состояние", example = "GOOD")
        @NotNull EquipmentCondition condition,
        @Schema(description = "Идентификатор сотрудника (менеджера или администратора), который меняет состояние",
                example = "2")
        @NotNull Long staffId,
        @Schema(description = "Причина изменения, попадает в журнал заметок",
                example = "Клиент почистил и починил фиксатор аккумулятора")
        @NotBlank String reason) {
}
