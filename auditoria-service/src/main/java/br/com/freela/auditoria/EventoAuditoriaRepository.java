package br.com.freela.auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, UUID>{
    boolean existsByEventId(UUID eventId);
}
