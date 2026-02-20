package example.docs.service;

import example.docs.entity.DocumentStatus;
import example.docs.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
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

    @Scheduled(fixedDelayString = "${app.workers.submit-delay:5000}")
    public void processDrafts() {
        long startTime = System.currentTimeMillis();

        List<UUID> draftIds = documentRepository.findIdsByStatus(
                DocumentStatus.DRAFT, PageRequest.of(0, batchSize));

        if (draftIds.isEmpty()) return;

        log.info("SUBMIT-worker: Начало обработки пачки из {} документов..", draftIds.size());
        Map<UUID, String> results = documentService.submitBatch(draftIds, "SUBMIT-WORKER");

        long remaining = documentRepository.countByStatus(DocumentStatus.DRAFT);
        long executionTime = System.currentTimeMillis() - startTime;

        log.info("SUBMIT-worker: Пачка обработана за {} мс. Успешно/Всего: {}/{}. Осталось DRAFT: {}",
                executionTime, countSuccess(results), draftIds.size(), remaining);
    }

    @Scheduled(fixedDelayString = "${app.workers.approve-delay:5000}")
    public void processSubmitted() {
        long startTime = System.currentTimeMillis();

        List<UUID> submittedIds = documentRepository.findIdsByStatus(
                DocumentStatus.SUBMITTED, PageRequest.of(0, batchSize));

        if (submittedIds.isEmpty()) {
            return;
        }

        log.info("APPROVE-worker: Начало обработки пачки из {} документов...", submittedIds.size());

        Map<UUID, String> results = documentService.approveBatch(submittedIds, "APPROVE-WORKER");

        long remaining = documentRepository.countByStatus(DocumentStatus.SUBMITTED);
        long executionTime = System.currentTimeMillis() - startTime;

        log.info("APPROVE-worker: Пачка обработана за {} мс. Успешно/Всего: {}/{}. Осталось SUBMITTED: {}",
                executionTime, countSuccess(results), submittedIds.size(), remaining);
    }

    private long countSuccess(Map<UUID, String> results) {
        return results.values().stream().filter(v -> v.equals("SUCCESS")).count();
    }
}
