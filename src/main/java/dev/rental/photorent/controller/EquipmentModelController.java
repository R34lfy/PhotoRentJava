package dev.rental.photorent.controller;

import dev.rental.photorent.config.ApiDocs;
import dev.rental.photorent.dto.ModelCreateRequest;
import dev.rental.photorent.dto.ModelResponse;
import dev.rental.photorent.dto.ModelUpdateRequest;
import dev.rental.photorent.service.EquipmentService;
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
@RequestMapping("/api/models")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Tag(name = ApiDocs.TAG_MODELS)
public class EquipmentModelController {

    private final EquipmentService equipmentService;

    @Operation(summary = "Список моделей техники",
            description = "Можно оставить только модели одной категории.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список моделей (может быть пустым)")
    })
    @GetMapping
    public List<ModelResponse> getAll(
            @Parameter(description = "Префикс категории для фильтрации", example = "CAM")
            @RequestParam(name = "categoryPrefix", required = false) String categoryPrefix) {
        return equipmentService.getModels(categoryPrefix).stream().map(ModelResponse::from).toList();
    }

    @Operation(summary = "Модель техники по идентификатору")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Модель найдена"),
            @ApiResponse(responseCode = "400", description = "Идентификатор не является числом",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Модель не найдена",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ModelResponse getById(
            @Parameter(description = "Идентификатор модели", example = "1") @PathVariable("id") Long id) {
        return equipmentService.getModelById(id)
                .map(ModelResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Equipment model not found: " + id));
    }

    @Operation(summary = "Создание модели техники",
            description = "Пара бренд и название должна быть уникальной без учёта регистра.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Модель создана"),
            @ApiResponse(responseCode = "400", description = "Тело запроса не прошло проверку",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Категория не найдена",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Такая модель уже существует",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ModelResponse create(@Valid @RequestBody ModelCreateRequest request) {
        return ModelResponse.from(equipmentService.createModel(request.categoryPrefix(), request.brand(),
                request.name(), request.description(), request.dailyRate()));
    }

    @Operation(summary = "Изменение описания и тарифа модели",
            description = "Категорию, бренд и название изменить нельзя.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Модель обновлена"),
            @ApiResponse(responseCode = "400", description = "Тело запроса не прошло проверку",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Модель не найдена",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    @Transactional
    public ModelResponse update(
            @Parameter(description = "Идентификатор модели", example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody ModelUpdateRequest request) {
        return ModelResponse.from(
                equipmentService.updateModel(id, request.description(), request.dailyRate()));
    }

    @Operation(summary = "Удаление модели",
            description = "Модель, у которой есть экземпляры техники, удалить нельзя.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Модель удалена"),
            @ApiResponse(responseCode = "404", description = "Модель не найдена",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "На модель ссылаются экземпляры техники",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(
            @Parameter(description = "Идентификатор модели", example = "1") @PathVariable("id") Long id) {
        if (!equipmentService.removeModel(id)) {
            throw new NoSuchElementException("Equipment model not found: " + id);
        }
    }
}
