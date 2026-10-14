package br.com.freela.contrato.application;

import br.com.freela.contrato.domain.model.Contrato;
import br.com.freela.contrato.domain.repository.ContratoRepository;
import br.com.freela.contrato.domain.shared.DomainEvent;
import br.com.freela.contrato.infrastructure.persistence.EventoPendenteJpaEntity;
import br.com.freela.contrato.infrastructure.persistence.SpringDataEventoPendenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;

import java.util.List;
import java.util.UUID;

@Service
public class ContratoApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ContratoApplicationService.class);
    private final ContratoRepository repository;
    private final SpringDataEventoPendenteRepository eventoRepository;
    private final ObjectMapper objectMapper;

    public ContratoApplicationService(ContratoRepository repository, SpringDataEventoPendenteRepository eventoRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.eventoRepository = eventoRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Contrato criar(CriarContratoCommand cmd) {
        log.info("contrato.criacao.inicio clienteId={} freelancerId={} titulo={} valor={}",
                cmd.clienteId(), cmd.freelancerId(), cmd.titulo(), cmd.valor());
        Contrato contrato = Contrato.criar(cmd.clienteId(), cmd.freelancerId(), cmd.titulo(), cmd.valor());
        log.info("contrato.dominio.criado contratoId={} status={} domainEvents={}",
                contrato.id(), contrato.status(), contrato.domainEvents().size());
        Contrato salvo = repository.salvar(contrato);

        for (DomainEvent event : contrato.pullDomainEvents()) {
            String payload = objectMapper.writeValueAsString(Map.of("eventId", event.eventId(), "eventType", event.eventType(), "occurredAt", event.occurredAt(), "correlationId", event.eventId().toString(), "payload", event));
            EventoPendenteJpaEntity eventoPendente = new EventoPendenteJpaEntity(
                            event.eventId(),
                            contrato.id(),
                            event.eventType(),
                            event.occurredAt(),
                            payload,
                            null
                    );
            eventoRepository.save(eventoPendente);
            log.info("contrato.evento.persistido contratoId={} eventId={} eventType={}", contrato.id(), event.eventId(), event.eventType());
        }

        log.info("contrato.criacao.sucesso contratoId={} clienteId={} freelancerId={} status={}",
                salvo.id(), salvo.clienteId(), salvo.freelancerId(), salvo.status());
        return salvo;
    }

    @Transactional(readOnly = true)
    public Contrato buscar(UUID id) {
        log.info("contrato.busca.inicio contratoId={}", id);
        var contrato = repository.buscarPorId(id).orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + id));
        log.info("contrato.busca.sucesso contratoId={} status={}", id, contrato.status());
        return contrato;
    }

    @Transactional(readOnly = true)
    public List<Contrato> listar() {
        log.info("contrato.listagem.inicio");
        var contratos = repository.listar();
        log.info("contrato.listagem.sucesso quantidade={}", contratos.size());
        return contratos;
    }


    @Transactional
    public Contrato registrarEntrega(UUID id) {
        Contrato contrato = repository.buscarPorId(id).orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + id));
        contrato.registrarEntrega();
        Contrato salvo = repository.salvar(contrato);

        log.info("contrato.entrega.registrada contratoId={} status={}", salvo.id(), salvo.status());
        return salvo;
    }

    @Transactional
    public Contrato concluir(UUID id) {
        Contrato contrato = repository.buscarPorId(id).orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + id));

        contrato.concluir();
        Contrato salvo = repository.salvar(contrato);

        for (DomainEvent event : contrato.pullDomainEvents()) {
            String payload = objectMapper.writeValueAsString(Map.of("eventId", event.eventId(), "eventType", event.eventType(), "occurredAt", event.occurredAt(), "correlationId", event.eventId().toString(), "payload", event));
            EventoPendenteJpaEntity pendente = new EventoPendenteJpaEntity(event.eventId(), contrato.id(), event.eventType(), event.occurredAt(), payload, null);

            eventoRepository.save(pendente);
        }

        log.info("contrato.concluido contratoId={} status={}", salvo.id(), salvo.status());
        return salvo;
    }

}
