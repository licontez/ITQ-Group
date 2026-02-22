package example.docs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сущность документа в системе.
 * Хранит основную информацию о документе и отслеживает его текущий статус.
 * Жизненный цикл статусов: DRAFT -> SUBMITTED -> APPROVED.
 */
@Entity
@Table(name = "documents")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true) // Генерация UUID или sequence
    private String uniqueNumber;

    private String author;
    private String title;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentHistory> history = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Поле для контроля версий при конкурентном доступе.
     * Защищает от перезаписи данных при одновременном утверждении документа разными потоками.
     */
    @Version
    private Long version;

}
