package example.docs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Запрос на пакетную обработку документов (смена статусов)")
public class BatchActionRequest {

    @NotEmpty(message = "Список документов не может быть пустым")
    @Schema(description = "Список уникальных идентификаторов документов",
            example = "[\"123e4567-e89b-12d3-a456-426614174000\", \"987e6543-e21b-12d3-a456-426614174000\"]")
    private List<UUID> documentIds;

    @NotNull(message = "Инициатор должен быть указан")
    @Schema(description = "Пользователь или система, инициирующая пакетную операцию", example = "admin_user")
    private String initiator;
}
