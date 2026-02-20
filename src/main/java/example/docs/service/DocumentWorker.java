package example.docs.service;

import example.docs.entity.DocumentStatus;
import example.docs.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentWorker {

    private final DocumentRepository documentRepository;
    private final DocumentService documentService;

    @Value("${app.workers.batch-size:10}")
    private int batchSize;

    @Async
    @Scheduled(fixedDelayString = "${app.workers.submit-delay:5000}")
    public void processDrafts() {
        long startTime = System.currentTimeMillis();

        List<UUID> draftIds = documentRepository.findIdsByStatus(
                DocumentStatus.DRAFT,
                PageRequest.of(0, batchSize, Sort.by(Sort.Direction.ASC, "createdAt"))
        );

        if (draftIds.isEmpty()) return;

        log.info("SUBMIT-worker: Найдено {} документов DRAFT. Отправка на согласование...", draftIds.size());

        Map<UUID, String> results = documentService.submitBatch(draftIds, "SUBMIT-WORKER");

        long remaining = documentRepository.countByStatus(DocumentStatus.DRAFT);
        long executionTime = System.currentTimeMillis() - startTime;
        long successCount = countSuccess(results);

        log.info("SUBMIT-worker: Пачка обработана за {} мс. Успешно: {}, Ошибок: {}. Осталось DRAFT: {}",
                executionTime, successCount, draftIds.size() - successCount, remaining);

        if (successCount == 0 && !draftIds.isEmpty()) {
            log.warn("SUBMIT-worker: Внимание! Вся пачка завершилась с ошибками. Возможна блокировка очереди битыми документами: {}", results);
        }
    }

    @Async
    @Scheduled(fixedDelayString = "${app.workers.approve-delay:5000}")
    public void processSubmitted() {
        long startTime = System.currentTimeMillis();

        List<UUID> submittedIds = documentRepository.findIdsByStatus(
                DocumentStatus.SUBMITTED,
                PageRequest.of(0, batchSize, Sort.by(Sort.Direction.ASC, "createdAt"))
        );

        if (submittedIds.isEmpty()) return;

        log.info("APPROVE-worker: Найдено {} документов SUBMITTED. Отправка на утверждение...", submittedIds.size());

        Map<UUID, String> results = documentService.approveBatch(submittedIds, "APPROVE-WORKER");

        long remaining = documentRepository.countByStatus(DocumentStatus.SUBMITTED);
        long executionTime = System.currentTimeMillis() - startTime;
        long successCount = countSuccess(results);

        log.info("APPROVE-worker: Пачка обработана за {} мс. Успешно: {}, Ошибок: {}. Осталось SUBMITTED: {}",
                executionTime, successCount, submittedIds.size() - successCount, remaining);

        if (successCount == 0 && !submittedIds.isEmpty()) {
            log.warn("APPROVE-worker: Внимание! Вся пачка завершилась с ошибками. Возможна блокировка очереди: {}", results);
        }
    }

    private long countSuccess(Map<UUID, String> results) {
        return results.values().stream().filter(v -> v.equals("SUCCESS")).count();
    }
}