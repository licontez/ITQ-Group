package example.docs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на создание нового докуемнта")
public class CreateDocumentRequest {

    @NotBlank(message = "Автор не может быть пустым")
    @Schema(description = "Имя или идентификатор автора документа", example = "Иван Иванов")
    private String author;

    @NotBlank(message = "Название не может быть пустым")
    @Schema(description = "Заголовок/название документа", example = "Годовой отчет 2026")
    private String title;
}
