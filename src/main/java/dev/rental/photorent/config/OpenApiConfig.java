package dev.rental.photorent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

// Swagger UI: /swagger-ui.html, описание в JSON: /v3/api-docs
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI photoRentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PhotoRent API")
                        .version("1.0")
                        .description("Сервис аренды фото- и видеооборудования. "
                                + "Ошибки возвращаются в формате ProblemDetail (RFC 9457): "
                                + "400 - неверный запрос, 404 - объект не найден, "
                                + "409 - нарушено бизнес-правило или ограничение БД."))
                .tags(List.of(
                        new Tag().name(ApiDocs.TAG_CATEGORIES)
                                .description("Справочник категорий техники (только чтение)"),
                        new Tag().name(ApiDocs.TAG_MODELS)
                                .description("Модели техники: бренд, название, тариф"),
                        new Tag().name(ApiDocs.TAG_EQUIPMENT)
                                .description("Экземпляры техники, их состояние и журнал заметок"),
                        new Tag().name(ApiDocs.TAG_RENTALS)
                                .description("Бронирование, выдача, возврат и продление аренды"),
                        new Tag().name(ApiDocs.TAG_USERS)
                                .description("Пользователи с ролями ADMIN, MANAGER, CUSTOMER")));
    }
}
