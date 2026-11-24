package dev.rental.photorent.controller;

import dev.rental.photorent.config.ApiDocs;
import dev.rental.photorent.dto.CategoryResponse;
import dev.rental.photorent.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// категории заполняются в schema.sql, поэтому только чтение
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = ApiDocs.TAG_CATEGORIES)
public class CategoryController {

    private final EquipmentService equipmentService;

    @Operation(summary = "Список категорий техники",
            description = "Справочник создаётся скриптом schema.sql: камеры, объективы, свет, штативы, аудио, аксессуары.")
    @ApiResponse(responseCode = "200", description = "Список категорий")
    @GetMapping
    public List<CategoryResponse> getAll() {
        return equipmentService.getCategories().stream().map(CategoryResponse::from).toList();
    }
}
