
package br.com.freela.notificacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
public class NotificacaoListener {
    private static final Logger log = LoggerFactory.getLogger(NotificacaoListener.class);
    private final NotificacaoService service;
    private final ObjectMapper objectMapper;

    public NotificacaoListener(NotificacaoService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "contratos.eventos", groupId = "notificacao-service")
    public void consumir(String mensagem) {
        EventoMensagem evento = objectMapper.readValue(mensagem, EventoMensagem.class);

        if (!"ContratoCriado".equals(evento.eventType())) {
            log.info("notificacao.evento.ignorado eventId={} eventType={}", evento.eventId(), evento.eventType());
            return;
        }

        ContratoCriadoPayload dados = objectMapper.convertValue(evento.payload(), ContratoCriadoPayload.class);

        service.registrar(evento.eventId(), dados.contratoId(), dados.clienteId(), "CONTRATO_CRIADO", "Seu contrato foi criado com sucesso.");

        log.info("notificacao.evento.processado eventId={} contratoId={}", evento.eventId(), dados.contratoId());
    }

    public record EventoMensagem(UUID eventId, String eventType, Object payload) {
    }

    public record ContratoCriadoPayload(UUID contratoId, UUID clienteId) {
    }
}
