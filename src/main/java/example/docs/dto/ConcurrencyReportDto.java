package example.docs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConcurrencyReportDto {
    private int successfulAttempts;
    private int conflictAttempts;
    private int errorAttempts;
    private String finalStatus;
}
