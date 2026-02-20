package example.docs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BatchActionRequest {

    @NotEmpty(message = "Document IDs list cannot be empty")
    @Size(max = 1000, message = "Max batch size is 1000")
    private List<UUID>  documentIds;

    @NotBlank(message = "Initiator is required")
    private String initiator;
}
