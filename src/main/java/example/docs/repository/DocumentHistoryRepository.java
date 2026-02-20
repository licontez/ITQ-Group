package example.docs.repository;

import example.docs.entity.DocumentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentHistoryRepository extends JpaRepository<DocumentHistory, UUID> {

    /** Получить историю конкретного документа с сортировкой по дате */
    List<DocumentHistory> findAllByDocumentIdOrderByCreatedAtAsc(UUID documentId);
}
