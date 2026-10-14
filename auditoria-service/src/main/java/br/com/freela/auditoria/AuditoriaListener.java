
package br.com.freela.auditoria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
public class AuditoriaListener {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaListener.class);
    private final AuditoriaService service;
    private final ObjectMapper objectMapper;

    public AuditoriaListener(AuditoriaService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "contratos.eventos", groupId = "auditoria-service")
    public void consumir(String mensagem) {
        JsonNode evento = objectMapper.readTree(mensagem);

        if (!evento.hasNonNull("eventType") || !evento.hasNonNull("payload")) {
            log.info("auditoria.evento.formato.antigo.ignorado");
            return;
        }

        UUID eventId = UUID.fromString(evento.get("eventId").asText());

        String eventType = evento.get("eventType").asText();

        JsonNode payload = evento.get("payload");

        UUID contratoId = UUID.fromString(payload.get("contratoId").asText());

        String correlationId = evento.hasNonNull("correlationId") ? evento.get("correlationId").asText() : null;

        service.registrar(eventId, contratoId, eventType, correlationId, mensagem);

        log.info("auditoria.evento.processado eventId={} contratoId={} eventType={}", eventId, contratoId, eventType);
    }
}
