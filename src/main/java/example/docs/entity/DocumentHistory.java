package example.docs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * История изменения статусов документа.
 * Фиксирует каждое действие (например, перевод в SUBMITTED или APPROVED), время его совершения
 * и инициатора, обеспечивая полный аудит жизненного цикла документа.
 */
@Entity
@Table(name = "document_history")
@Getter
@Setter
@NoArgsConstructor
public class DocumentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(nullable = false)
    private String initiator;

    @Enumerated(EnumType.STRING)
    private DocumentAction action;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private String comment;

    public DocumentHistory(Document document, String initiator, DocumentAction action, String comment) {
        this.document = document;
        this.initiator = initiator;
        this.action = action;
        this.comment = comment;
    }

}
