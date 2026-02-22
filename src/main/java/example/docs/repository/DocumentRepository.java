package example.docs.repository;

import example.docs.entity.Document;
import example.docs.entity.DocumentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Основной репозиторий для работы с документами.
 * Наследует JpaSpecificationExecutor для поддержки сложного динамического поиска по фильтрам.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID>, JpaSpecificationExecutor<Document> {

    /**
     * Возвращает только идентификаторы документов вместо загрузки полных сущностей (Entity).
     *
     * @param status целевой статус (например, DRAFT или SUBMITTED)
     * @param pageable параметры пагинации (лимит и сортировка по дате создания)
     * @return список UUID документов, готовых к обработке
     */
    @Query("SELECT d.id FROM Document d WHERE d.status = :status")
    List<UUID> findIdsByStatus(@Param("status") DocumentStatus status, Pageable pageable);

    /**
     * Жадная (EAGER) загрузка документа вместе с его историей за один SELECT.
     *
     * @param id идентификатор документа
     * @return Optional с документом и инициализированной коллекцией истории
     */
    @EntityGraph(attributePaths = "history")
    Optional<Document> findWithHistoryById(UUID id);

    long countByStatus(DocumentStatus status);
}
