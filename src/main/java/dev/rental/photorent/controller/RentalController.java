package dev.rental.photorent.controller;

import dev.rental.photorent.config.ApiDocs;
import dev.rental.photorent.dto.OverdueCheckResponse;
import dev.rental.photorent.dto.RentalCreateRequest;
import dev.rental.photorent.dto.RentalExtendRequest;
import dev.rental.photorent.dto.RentalResponse;
import dev.rental.photorent.model.RentalStatus;
import dev.rental.photorent.service.RentalService;
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
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

// транзакция на контроллере нужна, потому что open-in-view выключен:
// DTO читает ленивые связи, а они доступны только пока транзакция открыта
@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Tag(name = ApiDocs.TAG_RENTALS)
public class RentalController {

    private final RentalService rentalService;

    @Operation(summary = "Поиск аренд с фильтрами",
            description = "Все фильтры необязательные и комбинируются через И.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список аренд (может быть пустым)"),
            @ApiResponse(responseCode = "400", description = "Недопустимое значение статуса или идентификатора",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public List<RentalResponse> search(
            @Parameter(description = "Идентификатор клиента", example = "3")
            @RequestParam(name = "customerId", required = false) Long customerId,
            @Parameter(description = "Идентификатор экземпляра техники", example = "1")
            @RequestParam(name = "equipmentId", required = false) Long equipmentId,
            @Parameter(description = "Статус аренды") @RequestParam(name = "status", required = false) RentalStatus status) {
        return rentalService.search(customerId, equipmentId, status).stream()
                .map(RentalResponse::from).toList();
    }

    @Operation(summary = "Аренда по идентификатору")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Аренда найдена"),
            @ApiResponse(responseCode = "400", description = "Идентификатор не является числом",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Аренда не найдена",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public RentalResponse getById(
            @Parameter(description = "Идентификатор аренды", example = "10") @PathVariable("id") Long id) {
        return rentalService.getById(id)
                .map(RentalResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Rental not found: " + id));
    }

    @Operation(summary = "Бронирование техники",
            description = "Дата начала не может быть в прошлом. Бронь отклоняется, если техника непригодна (требует ремонта или списана) или на эти даты уже есть пересекающаяся бронь или аренда. Текущий статус техники не важен: бронировать можно на будущее, даже если она сейчас у другого клиента.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Бронь создана (статус BOOKED)"),
            @ApiResponse(responseCode = "400", description = "Тело запроса не прошло проверку, неверные даты или начало в прошлом",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Техника или клиент не найдены",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Техника непригодна или даты пересекаются с другой арендой",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public RentalResponse book(@Valid @RequestBody RentalCreateRequest request) {
        return RentalResponse.from(rentalService.bookRental(request.equipmentId(), request.customerId(),
                request.startDate(), request.endDate()));
    }

    @Operation(summary = "Выдача техники клиенту",
            description = "Бронь переходит в ACTIVE, техника получает статус RENTED. Выдать можно только технику, свободную прямо сейчас.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Техника выдана"),
            @ApiResponse(responseCode = "404", description = "Аренда не найдена",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Аренда не в статусе BOOKED или техника сейчас недоступна",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/issue")
    @Transactional
    public RentalResponse issue(
            @Parameter(description = "Идентификатор аренды", example = "10") @PathVariable("id") Long id) {
        rentalService.issueEquipment(id);
        return getById(id);
    }

    @Operation(summary = "Возврат техники",
            description = "Аренда в статусе ACTIVE или OVERDUE переходит в RETURNED, техника снова становится AVAILABLE (кроме списанной).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Техника принята"),
            @ApiResponse(responseCode = "404", description = "Аренда не найдена",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Аренда не в статусе ACTIVE или OVERDUE",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/return")
    @Transactional
    public RentalResponse returnEquipment(
            @Parameter(description = "Идентификатор аренды", example = "10") @PathVariable("id") Long id) {
        rentalService.returnEquipment(id);
        return getById(id);
    }

    @Operation(summary = "Продление аренды",
            description = "Продлить можно аренду в статусе ACTIVE. Если на период продления уже есть чья-то бронь, продление отклоняется: у кого бронь раньше, тот и в приоритете.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Аренда продлена"),
            @ApiResponse(responseCode = "400", description = "Тело запроса не прошло проверку или новая дата не позже текущей",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Аренда не найдена",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Аренда не ACTIVE или период занят другой бронью",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/extend")
    @Transactional
    public RentalResponse extend(
            @Parameter(description = "Идентификатор аренды", example = "10") @PathVariable("id") Long id,
            @Valid @RequestBody RentalExtendRequest request) {
        rentalService.extendRental(id, request.newEndDate());
        return getById(id);
    }

    @Operation(summary = "Отмена брони",
            description = "Отменить (удалить) можно только бронь в статусе BOOKED, пока технику не выдали.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Бронь отменена"),
            @ApiResponse(responseCode = "404", description = "Аренда не найдена",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Аренда уже не в статусе BOOKED",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void cancel(
            @Parameter(description = "Идентификатор аренды", example = "10") @PathVariable("id") Long id) {
        rentalService.cancelBooking(id);
    }

    @Operation(summary = "Проверка просрочек",
            description = "Активные аренды делятся между потоками OverdueChecker-N; у просроченных статус меняется на OVERDUE. Если дата не указана, берётся сегодняшняя.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Проверка выполнена, в ответе число помеченных аренд"),
            @ApiResponse(responseCode = "400", description = "Дата в неверном формате",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/overdue-check")
    @Transactional
    public OverdueCheckResponse markOverdue(
            @Parameter(description = "Дата, относительно которой считать просрочку (yyyy-MM-dd)", example = "2026-12-20")
            @RequestParam(name = "today", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today) {
        return new OverdueCheckResponse(rentalService.markOverdueRentals(today));
    }
}
