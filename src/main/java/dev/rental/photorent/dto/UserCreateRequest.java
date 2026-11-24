package dev.rental.photorent.dto;

import dev.rental.photorent.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на регистрацию пользователя")
public record UserCreateRequest(
        @Schema(description = "Логин, уникальный без учёта регистра", example = "john_doe")
        @NotBlank @Size(max = 30) String username,
        @Schema(description = "Электронная почта, уникальная без учёта регистра", example = "john@example.com")
        @NotBlank @Email @Size(max = 255) String email,
        // до лабораторной с авторизацией пароль сохраняется как есть, в ответах он не возвращается
        @Schema(description = "Пароль (в ответах не возвращается)", example = "secret", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Size(max = 255) String password,
        @Schema(description = "Роль пользователя", example = "CUSTOMER")
        @NotNull UserRole role) {
}
