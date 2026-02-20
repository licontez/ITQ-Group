package example.docs.service;

import example.docs.dto.ConcurrencyReportDto;
import example.docs.entity.Document;
import example.docs.entity.DocumentStatus;
import example.docs.repository.DocumentRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentProcessor documentProcessor;

    public Document createDocument(String author, String title) {
        Document document = new Document();
        document.setAuthor(author);
        document.setTitle(title);
        document.setStatus(DocumentStatus.DRAFT);

        document.setUniqueNumber("DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return documentRepository.save(document);
    }

    public List<Document> searchDocuments(DocumentStatus status, String author, LocalDateTime from, LocalDateTime to) {
        Specification<Document> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (author != null && !author.isBlank()) {
                predicates.add(cb.equal(root.get("author"), author));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return documentRepository.findAll(spec);
    }

    public ConcurrencyReportDto testConcurrency(UUID docId, int threads, int attempts) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            tasks.add(() -> {
                try {
                    // Пытаемся утвердить документ (наш атомарный метод с REQUIRES_NEW)
                    documentProcessor.processApprove(docId, "CONCURRENCY_TESTER");
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    // Hibernate поймал изменение версии другим потоком
                    conflictCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    // Наша бизнес-ошибка (статус уже не SUBMITTED или ошибка реестра)
                    if (e.getMessage().contains("CONFLICT") || e.getMessage().contains("REGISTRY_ERROR")) {
                        conflictCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
                return null;
            });
        }

        try {
            // Запускаем все потоки одновременно
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }

        // Получаем финальный статус документа
        String finalStatus = documentRepository.findById(docId)
                .map(doc -> doc.getStatus().name())
                .orElse("NOT_FOUND");

        return new ConcurrencyReportDto(
                successCount.get(),
                conflictCount.get(),
                errorCount.get(),
                finalStatus
        );
    }

    public Map<UUID, String> submitBatch(List<UUID> documentIds, String initiator) {
        Map<UUID, String> results = new HashMap<>();

        for (UUID id : documentIds) {
            try {
                documentProcessor.processSubmit(id, initiator);
                results.put(id, "SUCCESS");
            } catch (ObjectOptimisticLockingFailureException e) {
                results.put(id, "CONFLICT");
            } catch (Exception e) {
                results.put(id, parseExceptionToResult(e));
            }
        }
        return results;
    }


    public Map<UUID, String> approveBatch(List<UUID> documentIds, String initiator) {
        Map<UUID, String> results = new HashMap<>();

        for (UUID id : documentIds) {
            try {
                documentProcessor.processApprove(id, initiator);
                results.put(id, "SUCCESS");
            } catch (ObjectOptimisticLockingFailureException e) {
                results.put(id, "CONFLICT");
            } catch (Exception e) {
                results.put(id, parseExceptionToResult(e));
            }
        }
        return results;
    }

    private String parseExceptionToResult(Exception e) {
        if (e.getMessage() != null) {
            if (e.getMessage().contains("NOT_FOUND")) return "NOT_FOUND";
            if (e.getMessage().contains("CONFLICT")) return "CONFLICT";
            if (e.getMessage().contains("REGISTRY_ERROR")) return "REGISTRY_ERROR";
        }
        log.error("Unexpected error during processing", e);
        return "ERROR";
    }
}
