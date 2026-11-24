package dev.rental.photorent.dto;

import dev.rental.photorent.model.User;
import dev.rental.photorent.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Пользователь")
public record UserResponse(
        @Schema(description = "Идентификатор пользователя", example = "3") Long id,
        @Schema(description = "Логин", example = "john_doe") String username,
        @Schema(description = "Электронная почта", example = "john@example.com") String email,
        @Schema(description = "Роль", example = "CUSTOMER") UserRole role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
