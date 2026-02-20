package example.docs.repository;

import example.docs.entity.Document;
import example.docs.entity.DocumentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID>, JpaSpecificationExecutor<Document> {

    @Query("SELECT d.id FROM Document d WHERE d.status = :status")
    List<UUID> findIdsByStatus(@Param("status")DocumentStatus status, Pageable pageable);

    long countByStatus(DocumentStatus status);
}
