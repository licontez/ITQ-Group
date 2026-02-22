package example.docs.repository;

import example.docs.entity.DocumentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для работы с историей изменений документов.
 */
@Repository
public interface DocumentHistoryRepository extends JpaRepository<DocumentHistory, UUID> {

    /** Возвращает полную историю документа в хронологическом порядке.
     * @param documentId идентификатор документа
     * @return список записей истории, отсортированный от старых к новым
     */
    List<DocumentHistory> findAllByDocumentIdOrderByCreatedAtAsc(UUID documentId);
}
