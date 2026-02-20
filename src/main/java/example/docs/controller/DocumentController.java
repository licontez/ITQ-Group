package example.docs.controller;

import example.docs.dto.BatchActionRequest;
import example.docs.dto.ConcurrencyReportDto;
import example.docs.dto.CreateDocumentRequest;
import example.docs.entity.Document;
import example.docs.entity.DocumentStatus;
import example.docs.repository.DocumentRepository;
import example.docs.service.DocumentService;
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

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/search")
    public Page<Document> searchDocuments(@RequestParam(required = false) DocumentStatus status,
                                          @RequestParam(required = false) String author,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                          @PageableDefault(size = 20) Pageable pageable) { // Добавили Pageable

        return documentService.searchDocuments(status, author, from, to, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable UUID id) {
        return documentService.getDocumentWithHistory(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/concurrency-test")
    public ConcurrencyReportDto testConcurrency(@PathVariable UUID id,
                                                @RequestParam(defaultValue = "5") int threads,
                                                @RequestParam(defaultValue = "10") int attempts) {
        return documentService.testConcurrency(id, threads, attempts);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Document createDocument(@Valid @RequestBody CreateDocumentRequest request) {
        return documentService.createDocument(request.getAuthor(), request.getTitle());
    }

    @PostMapping("/batch-get")
    public List<Document> getDocumentsByIds(@RequestBody List<UUID> ids) {
        return documentService.getDocumentsByIds(ids);
    }

    @PostMapping("/submit")
    public Map<UUID, String> submitBatch(@Valid @RequestBody BatchActionRequest request) {
        return documentService.submitBatch(request.getDocumentIds(), request.getInitiator());
    }

    @PostMapping("/approve")
    public Map<UUID, String> approveBatch(@Valid @RequestBody BatchActionRequest request) {
        return documentService.approveBatch(request.getDocumentIds(), request.getInitiator());
    }
}