package example.docs.repository;

import example.docs.entity.RegistryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Репозиторий для работы с реестром утвержденных документов.
 * Используется для фиксации факта финального утверждения (APPROVED).
 */
@Repository
public interface RegistryEntryRepository extends JpaRepository<RegistryEntry, UUID> {

}
