
package br.com.freela.contrato.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "eventos_pendentes")
public class EventoPendenteJpaEntity {

    @Id
    private UUID eventId;

    @Column(nullable = false)
    private UUID contratoId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    private String correlationId;

    @Column(nullable = false)
    private boolean published = false;

    protected EventoPendenteJpaEntity() {
    }

    public EventoPendenteJpaEntity(
            UUID eventId,
            UUID contratoId,
            String eventType,
            Instant occurredAt,
            String payload,
            String correlationId
    ) {
        this.eventId = eventId;
        this.contratoId = contratoId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.payload = payload;
        this.correlationId = correlationId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getContratoId() {
        return contratoId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getPayload() {
        return payload;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public boolean isPublished() {
        return published;
    }

    public void marcarComoPublicado() {
        this.published = true;
    }
}
