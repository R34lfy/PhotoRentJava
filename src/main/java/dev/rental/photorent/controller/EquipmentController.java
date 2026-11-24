package dev.rental.photorent.controller;

import dev.rental.photorent.config.ApiDocs;
import dev.rental.photorent.dto.ConditionUpdateRequest;
import dev.rental.photorent.dto.EquipmentCreateRequest;
import dev.rental.photorent.dto.EquipmentResponse;
import dev.rental.photorent.dto.NoteCreateRequest;
import dev.rental.photorent.dto.NoteResponse;
import dev.rental.photorent.model.EquipmentCondition;
import dev.rental.photorent.model.EquipmentStatus;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Tag(name = ApiDocs.TAG_EQUIPMENT)
public class EquipmentController {

    private final EquipmentService equipmentService;

    @Operation(summary = "Поиск техники с фильтрами",
            description = "Все фильтры необязательные и комбинируются через И. Название ищется по части модели без учёта регистра.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список техники (может быть пустым)"),
            @ApiResponse(responseCode = "400", description = "Недопустимое значение статуса или состояния",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public List<EquipmentResponse> search(
            @Parameter(description = "Префикс категории", example = "CAM")
            @RequestParam(name = "categoryPrefix", required = false) String categoryPrefix,
            @Parameter(description = "Текущий статус") @RequestParam(name = "status", required = false) EquipmentStatus status,
            @Parameter(description = "Физическое состояние")
            @RequestParam(name = "condition", required = false) EquipmentCondition condition,
            @Parameter(description = "Часть названия модели", example = "a7")
            @RequestParam(name = "name", required = false) String name) {
        return equipmentService.search(categoryPrefix, status, condition, name).stream()
                .map(EquipmentResponse::from).toList();
    }

    @Operation(summary = "Лучший свободный экземпляр модели",
            description = "Среди свободных и пригодных экземпляров выбирается тот, что в лучшем состоянии.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Экземпляр найден"),
            @ApiResponse(responseCode = "404", description = "Свободной техники с таким названием нет",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/best")
    public EquipmentResponse findBest(
            @Parameter(description = "Часть названия модели", example = "A7") @RequestParam("name") String name) {
        return equipmentService.findBestAvailableByName(name)
                .map(EquipmentResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No available equipment found for: " + name));
    }

    @Operation(summary = "Экземпляр техники по идентификатору")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Экземпляр найден"),
            @ApiResponse(responseCode = "400", description = "Идентификатор не является числом",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Экземпляр не найден",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public EquipmentResponse getById(
            @Parameter(description = "Идентификатор экземпляра", example = "1") @PathVariable("id") Long id) {
        return equipmentService.getById(id)
                .map(EquipmentResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Equipment not found: " + id));
    }

    @Operation(summary = "Добавление экземпляра техники",
            description = "Серийный номер выдаётся автоматически по счётчику категории, статус при создании AVAILABLE.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Экземпляр создан"),
            @ApiResponse(responseCode = "400", description = "Тело запроса не прошло проверку",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Модель не найдена",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public EquipmentResponse create(@Valid @RequestBody EquipmentCreateRequest request) {
        return EquipmentResponse.from(equipmentService.addEquipment(request.modelId(), request.condition()));
    }

    @Operation(summary = "Смена состояния экземпляра",
            description = "Причина обязательна и записывается в журнал. Списывать (DECOMMISSIONED) может только администратор и только технику без незавершённых аренд; списанную технику восстановить нельзя.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Состояние изменено"),
            @ApiResponse(responseCode = "400", description = "Тело запроса не прошло проверку или состояние не изменилось",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Экземпляр или сотрудник не найдены",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Нарушено правило: не сотрудник, списание не администратором, есть незавершённые аренды или техника уже списана",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/condition")
    @Transactional
    public EquipmentResponse updateCondition(
            @Parameter(description = "Идентификатор экземпляра", example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody ConditionUpdateRequest request) {
        equipmentService.updateCondition(id, request.condition(), request.staffId(), request.reason());
        return getById(id);
    }

    @Operation(summary = "Удаление экземпляра",
            description = "Экземпляр, на который ссылаются аренды или заметки, удалить нельзя (вместо этого его списывают).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Экземпляр удалён"),
            @ApiResponse(responseCode = "404", description = "Экземпляр не найден",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "На экземпляр ссылаются другие данные",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(
            @Parameter(description = "Идентификатор экземпляра", example = "1") @PathVariable("id") Long id) {
        if (!equipmentService.removeById(id)) {
            throw new NoSuchElementException("Equipment not found: " + id);
        }
    }

    @Operation(summary = "Журнал заметок экземпляра",
            description = "Заметки отсортированы от новых к старым.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список заметок (может быть пустым)"),
            @ApiResponse(responseCode = "400", description = "Идентификатор не является числом",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}/notes")
    public List<NoteResponse> getNotes(
            @Parameter(description = "Идентификатор экземпляра", example = "1") @PathVariable("id") Long id) {
        return equipmentService.getNotesFor(id).stream().map(NoteResponse::from).toList();
    }

    @Operation(summary = "Добавление заметки",
            description = "Заметки могут оставлять только сотрудники (менеджер или администратор).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Заметка добавлена"),
            @ApiResponse(responseCode = "400", description = "Тело запроса не прошло проверку",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Экземпляр или автор не найдены",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Автор не является сотрудником",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public NoteResponse addNote(
            @Parameter(description = "Идентификатор экземпляра", example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody NoteCreateRequest request) {
        return NoteResponse.from(equipmentService.addNote(id, request.authorId(), request.text()));
    }
}
