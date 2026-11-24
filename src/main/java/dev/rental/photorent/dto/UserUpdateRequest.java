package dev.rental.photorent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на изменение логина и почты пользователя")
public record UserUpdateRequest(
        @Schema(description = "Новый логин", example = "john_doe")
        @NotBlank @Size(max = 30) String username,
        @Schema(description = "Новая почта", example = "john@example.com")
        @NotBlank @Email @Size(max = 255) String email) {
}
