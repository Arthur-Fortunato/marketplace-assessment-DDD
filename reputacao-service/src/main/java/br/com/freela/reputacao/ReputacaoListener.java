package br.com.freela.reputacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class ReputacaoListener {
    private static final Logger log = LoggerFactory.getLogger(ReputacaoListener.class);
    private final ReputacaoService service;
    private final ObjectMapper objectMapper;

    public ReputacaoListener(ReputacaoService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "contratos.eventos", groupId = "reputacao-service")
    public void consumir(String mensagem) {
        EventoMensagem evento = objectMapper.readValue(mensagem, EventoMensagem.class);

        if (!"ContratoConcluido".equals(evento.eventType())) {
            log.info("reputacao.evento.ignorado eventId={} eventType={}", evento.eventId(), evento.eventType());
            return;
        }

        ContratoConcluidoPayload dados = objectMapper.convertValue(evento.payload(), ContratoConcluidoPayload.class);

        service.registrarContratoConcluido(evento.eventId(), dados.contratoId(), dados.freelancerId(), dados.valor());

        log.info("reputacao.evento.processado eventId={} contratoId={}", evento.eventId(), dados.contratoId());
    }

    public record EventoMensagem(UUID eventId, String eventType, Object payload) {
    }

    public record ContratoConcluidoPayload(UUID contratoId, UUID freelancerId, BigDecimal valor) {
    }
}
