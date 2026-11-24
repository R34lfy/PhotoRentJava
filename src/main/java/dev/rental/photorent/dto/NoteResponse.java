package dev.rental.photorent.dto;

import dev.rental.photorent.model.EquipmentNote;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Заметка из журнала изменений техники")
public record NoteResponse(
        @Schema(description = "Идентификатор заметки", example = "1") Long id,
        @Schema(description = "Идентификатор техники", example = "1") Long equipmentId,
        @Schema(description = "Идентификатор автора", example = "2") Long authorId,
        @Schema(description = "Логин автора", example = "manager1") String authorUsername,
        @Schema(description = "Время создания", example = "2026-11-18T12:30:00") LocalDateTime createdAt,
        @Schema(description = "Текст заметки") String text) {

    public static NoteResponse from(EquipmentNote note) {
        return new NoteResponse(note.getId(), note.getEquipment().getId(), note.getAuthor().getId(),
                note.getAuthor().getUsername(), note.getCreatedAt(), note.getText());
    }
}
