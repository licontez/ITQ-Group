package example.docs.service;

import example.docs.entity.*;
import example.docs.exception.InvalidStatusTransitionException;
import example.docs.exception.RegistryRegistrationException;
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
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));

        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new InvalidStatusTransitionException("CONFLICT: Cannot submit document in status " + document.getStatus());
        }

        document.setStatus(DocumentStatus.SUBMITTED);

        historyRepository.save(new DocumentHistory(document, initiator, DocumentAction.SUBMIT, "Sent for approval"));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processApprove(UUID documentId, String initiator) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));

        if (document.getStatus() != DocumentStatus.SUBMITTED) {
            throw new InvalidStatusTransitionException("CONFLICT: Cannot approve document in status " + document.getStatus());
        }

        document.setStatus(DocumentStatus.APPROVED);

        historyRepository.save(new DocumentHistory(document, initiator, DocumentAction.APPROVE, "Document approved"));

        try {
            registryRepository.saveAndFlush(new RegistryEntry(document));
        } catch (DataIntegrityViolationException e) {
            throw new RegistryRegistrationException("REGISTRY_ERROR: Failed to create registry entry");
        }
    }
}
