package example.docs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_history")
@AllArgsConstructor
@NoArgsConstructor
public class DocumentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    public Document document;

    private String initiator;
    private String action;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private String comment;

    public DocumentHistory(Document document, String initiator, String action, String comment) {
        this.document = document;
        this.initiator = initiator;
        this.action = action;
        this.comment = comment;
    }

}
