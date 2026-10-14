package br.com.freela.reputacao;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "eventos_processados")
public class EventoProcessado {
    @Id
    private UUID eventId;

    private Instant processadoEm;

    protected EventoProcessado() {
    }

    public EventoProcessado(UUID eventId) {
        this.eventId = eventId;
        this.processadoEm = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getProcessadoEm() {
        return processadoEm;
    }
}
