package dev.rental.photorent.controller;

import dev.rental.photorent.config.ApiDocs;
import dev.rental.photorent.dto.UserCreateRequest;
import dev.rental.photorent.dto.UserResponse;
import dev.rental.photorent.dto.UserUpdateRequest;
import dev.rental.photorent.model.User;
import dev.rental.photorent.model.UserRole;
import dev.rental.photorent.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

// транзакция на контроллере нужна, потому что open-in-view выключен:
// DTO читает ленивые связи, а они доступны только пока транзакция открыта
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Tag(name = ApiDocs.TAG_USERS)
public class UserController {

    private final UserService userService;

    @Operation(summary = "Список пользователей",
            description = "Возвращает всех пользователей. Можно оставить только пользователей с нужной ролью.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список пользователей (может быть пустым)"),
            @ApiResponse(responseCode = "400", description = "Недопустимое значение роли",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public List<UserResponse> getAll(
            @Parameter(description = "Фильтр по роли") @RequestParam(name = "role", required = false) UserRole role) {
        List<User> users = role == null ? userService.getAll() : userService.getByRole(role);
        return users.stream().map(UserResponse::from).toList();
    }

    @Operation(summary = "Пользователь по идентификатору")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь найден"),
            @ApiResponse(responseCode = "400", description = "Идентификатор не является числом",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public UserResponse getById(
            @Parameter(description = "Идентификатор пользователя", example = "3") @PathVariable("id") Long id) {
        return userService.getById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
    }

    @Operation(summary = "Регистрация пользователя",
            description = "Создаёт пользователя нужной роли. Логин и почта должны быть уникальны без учёта регистра.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пользователь создан"),
            @ApiResponse(responseCode = "400", description = "Тело запроса не прошло проверку",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Логин или почта уже заняты",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        // TODO (ЛР с авторизацией): хэшировать пароль, пока в password_hash уходит значение как есть
        User user = userService.register(request.username(), request.email(),
                request.password(), request.role());
        return UserResponse.from(user);
    }

    @Operation(summary = "Изменение логина и почты",
            description = "Роль пользователя не меняется. Пользователь может оставить свой прежний логин или почту.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь обновлён"),
            @ApiResponse(responseCode = "400", description = "Тело запроса не прошло проверку",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Логин или почта заняты другим пользователем",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    @Transactional
    public UserResponse update(
            @Parameter(description = "Идентификатор пользователя", example = "3") @PathVariable("id") Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return UserResponse.from(userService.update(id, request.username(), request.email()));
    }

    @Operation(summary = "Удаление пользователя",
            description = "Пользователя, на которого ссылаются аренды или заметки, удалить нельзя.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Пользователь удалён"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "На пользователя ссылаются другие данные",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(
            @Parameter(description = "Идентификатор пользователя", example = "3") @PathVariable("id") Long id) {
        if (!userService.removeById(id)) {
            throw new NoSuchElementException("User not found: " + id);
        }
    }
}
