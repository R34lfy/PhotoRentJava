package dev.rental.photorent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Запрос на добавление заметки к технике")
public record NoteCreateRequest(
        @Schema(description = "Идентификатор автора (сотрудника)", example = "2")
        @NotNull Long authorId,
        @Schema(description = "Текст заметки", example = "Царапина на байонете объектива")
        @NotBlank String text) {
}
