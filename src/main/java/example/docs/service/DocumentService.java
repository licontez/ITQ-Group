package example.docs.service;

import example.docs.dto.ConcurrencyReportDto;
import example.docs.entity.Document;
import example.docs.entity.DocumentStatus;
import example.docs.exception.InvalidStatusTransitionException;
import example.docs.exception.RegistryRegistrationException;
import example.docs.repository.DocumentRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
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

    public Page<Document> searchDocuments(DocumentStatus status, String author, LocalDateTime from, LocalDateTime to, Pageable pageable) {
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

        return documentRepository.findAll(spec, pageable);
    }

    public ConcurrencyReportDto testConcurrency(UUID docId, int threads, int attempts) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(attempts);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < attempts; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Все потоки ждут здесь отмашки
                    documentProcessor.processApprove(docId, "CONCURRENCY_TESTER");
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException | InvalidStatusTransitionException | RegistryRegistrationException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("Unexpected error in concurrency test", e);
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        try {
            startLatch.countDown(); // Даем отмашку! Все потоки ломятся в БД одновременно
            doneLatch.await();      // Ждем завершения всех задач
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }

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
            results.put(id, processSingleSubmit(id, initiator));
        }
        return results;
    }

    public Map<UUID, String> approveBatch(List<UUID> documentIds, String initiator) {
        Map<UUID, String> results = new HashMap<>();
        for (UUID id : documentIds) {
            results.put(id, processSingleApprove(id, initiator));
        }
        return results;
    }

    public Optional<Document> getDocumentWithHistory(UUID id) {
        return documentRepository.findWithHistoryById(id);
    }

    public List<Document> getDocumentsByIds(List<UUID> ids) {
        return documentRepository.findAllById(ids);
    }

    private String processSingleSubmit(UUID id, String initiator) {
        try {
            documentProcessor.processSubmit(id, initiator);
            return "SUCCESS";
        } catch (EntityNotFoundException e) {
            return "NOT_FOUND";
        } catch (InvalidStatusTransitionException | ObjectOptimisticLockingFailureException e) {
            return "CONFLICT";
        } catch (Exception e) {
            log.error("Unexpected error submitting document {}", id, e);
            return "ERROR";
        }
    }

    private String processSingleApprove(UUID id, String initiator) {
        try {
            documentProcessor.processApprove(id, initiator);
            return "SUCCESS";
        } catch (EntityNotFoundException e) {
            return "NOT_FOUND";
        } catch (InvalidStatusTransitionException | ObjectOptimisticLockingFailureException e) {
            return "CONFLICT";
        } catch (RegistryRegistrationException e) {
            return "REGISTRY_ERROR";
        } catch (Exception e) {
            log.error("Unexpected error approving document {}", id, e);
            return "ERROR";
        }
    }
}