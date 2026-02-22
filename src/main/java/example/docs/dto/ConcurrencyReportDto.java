package example.docs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Отчет о результатах нагрузочного тестирования конкурентного доступа (Optimistic Locking)")
public class ConcurrencyReportDto {

    @Schema(description = "Количество потоков, которые успешно выполнили операцию", example = "1")
    private int successCount;

    @Schema(description = "Количество потоков, которые получили отказ из-за блокировки (ObjectOptimisticLockingFailureException)", example = "9")
    private int conflictCount;

    @Schema(description = "Количество непредвиденных ошибок", example = "0")
    private int errorCount;

    @Schema(description = "Итоговый статус документа в базе данных после завершения теста", example = "APPROVED")
    private String finalStatus;
}