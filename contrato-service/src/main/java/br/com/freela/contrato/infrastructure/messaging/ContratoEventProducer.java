package br.com.freela.contrato.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.support.SendResult;

@Component
public class ContratoEventProducer {
    private static final Logger log = LoggerFactory.getLogger(ContratoEventProducer.class);
    private static final String TOPICO = "contratos.eventos";
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ContratoEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTemplate.setObservationEnabled(true);
    }

    public CompletableFuture<SendResult<String, String>> publicar(UUID contratoId, String mensagem) {
        log.info("kafka.publicacao.inicio contratoId={} topico={}", contratoId, TOPICO);
        return kafkaTemplate.send(TOPICO, contratoId.toString(), mensagem);
    }
}
