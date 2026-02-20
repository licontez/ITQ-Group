package example.docs.service;

import example.docs.entity.Document;
import example.docs.entity.DocumentHistory;
import example.docs.entity.DocumentStatus;
import example.docs.entity.RegistryEntry;
import example.docs.repository.DocumentHistoryRepository;
import example.docs.repository.DocumentRepository;
import example.docs.repository.RegistryEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessor {

    private final DocumentRepository documentRepository;
    private final DocumentHistoryRepository historyRepository;
    private final RegistryEntryRepository registryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSubmit(UUID documentId, String initiator) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found"));

        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new IllegalArgumentException("CONFLICT: Invalid status transition");
        }
        document.setStatus(DocumentStatus.SUBMITTED);
        historyRepository.save(new DocumentHistory(document, initiator, "SUBMIT", "Sent for approval"));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processApprove(UUID documentId, String initiator) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("NOT_FOUND"));

        if (document.getStatus() != DocumentStatus.SUBMITTED) {
            throw new IllegalArgumentException("CONFLICT: Invalid status transition");
        }
        document.setStatus(DocumentStatus.APPROVED);
        historyRepository.save(new DocumentHistory(document, initiator, "APPROVE", "Document approved"));

        try {
            registryRepository.save(new RegistryEntry(document));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("REGISTRY_ERROR");
        }
    }

}
