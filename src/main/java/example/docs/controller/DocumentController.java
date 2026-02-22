package example.docs.controller;

import example.docs.dto.BatchActionRequest;
import example.docs.dto.ConcurrencyReportDto;
import example.docs.dto.CreateDocumentRequest;
import example.docs.entity.Document;
import example.docs.entity.DocumentStatus;
import example.docs.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Управление документами", description = "API для работы с документами(создание, поиск, смена статусов)")
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Поиск документов",
               description = "Динамический поиск с пагинацией и фильтрацией по статусу, автору и дате создания."
    )
    @GetMapping("/search")
    public Page<Document> searchDocuments(@RequestParam(required = false) DocumentStatus status,
                                          @RequestParam(required = false) String author,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                          @PageableDefault(size = 20) Pageable pageable) {

        return documentService.searchDocuments(status, author, from, to, pageable);
    }

    @Operation(summary = "Получить документ",
               description = "Возвращает документ и полную историю его изменений по UUID."
    )
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable UUID id) {
        return documentService.getDocumentWithHistory(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Тест Optimistic Locking",
               description = "Технический эндпоинт для эмуляции конкурентного доступа (многопоточного утверждения документа)."
    )
    @PostMapping("/{id}/concurrency-test")
    public ConcurrencyReportDto testConcurrency(@PathVariable UUID id,
                                                @RequestParam(defaultValue = "5") int threads,
                                                @RequestParam(defaultValue = "10") int attempts) {
        return documentService.testConcurrency(id, threads, attempts);
    }

    @Operation(summary = "Создать документ",
               description = "Создает новый документ в статусе DRAFT."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Document createDocument(@Valid @RequestBody CreateDocumentRequest request) {
        return documentService.createDocument(request.getAuthor(), request.getTitle());
    }

    @Operation(summary = "Пакетное получение",
               description = "Возвращает список документов по массиву идентификаторов."
    )
    @PostMapping("/batch-get")
    public List<Document> getDocumentsByIds(@RequestBody List<UUID> ids) {
        return documentService.getDocumentsByIds(ids);
    }

    @Operation(summary = "Отправить на согласование (Batch)",
               description = "Пакетный перевод документов из DRAFT в SUBMITTED."
    )
    @PostMapping("/submit")
    public Map<UUID, String> submitBatch(@Valid @RequestBody BatchActionRequest request) {
        return documentService.submitBatch(request.getDocumentIds(), request.getInitiator());
    }

    @Operation(summary = "Утвердить документы (Batch)",
               description = "Пакетный перевод документов из SUBMITTED в APPROVED с занесением в реестр."
    )
    @PostMapping("/approve")
    public Map<UUID, String> approveBatch(@Valid @RequestBody BatchActionRequest request) {
        return documentService.approveBatch(request.getDocumentIds(), request.getInitiator());
    }
}