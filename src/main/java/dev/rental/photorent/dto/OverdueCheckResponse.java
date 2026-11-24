package dev.rental.photorent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Результат проверки просрочек")
public record OverdueCheckResponse(
        @Schema(description = "Сколько аренд помечено как просроченные", example = "2") int marked) {
}
