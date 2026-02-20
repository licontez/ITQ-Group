package example.docs.repository;

import example.docs.entity.RegistryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RegistryEntryRepository extends JpaRepository<RegistryEntry, UUID> {

}
