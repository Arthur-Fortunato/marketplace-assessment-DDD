
package br.com.freela.contrato.infrastructure.messaging;

import br.com.freela.contrato.infrastructure.persistence.EventoPendenteJpaEntity;
import br.com.freela.contrato.infrastructure.persistence.SpringDataEventoPendenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class EventoPendentePublisher {
    private static final Logger log = LoggerFactory.getLogger(EventoPendentePublisher.class);
    private final SpringDataEventoPendenteRepository repository;
    private final ContratoEventProducer producer;

    public EventoPendentePublisher(SpringDataEventoPendenteRepository repository, ContratoEventProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    @Scheduled(fixedRate = 10000)
    public void publicarPendentes() {
        var eventos = repository.findByPublishedFalseOrderByOccurredAtAsc();
        for (EventoPendenteJpaEntity evento : eventos) {
            try {
                producer.publicar(evento.getContratoId(), evento.getPayload()).get();
                evento.marcarComoPublicado();
                repository.save(evento);
                log.info("evento.publicado contratoId={} eventId={}", evento.getContratoId(), evento.getEventId());
            } catch (Exception e) {
                log.error("evento.publicacao.falhou contratoId={} eventId={}", evento.getContratoId(), evento.getEventId(), e);
                break;
            }
        }
    }
}
