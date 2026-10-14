package br.com.freela.reputacao;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ReputacaoService {
 private static final Logger log = LoggerFactory.getLogger(ReputacaoService.class);
 private final ReputacaoRepository repository;
 private final EventoProcessadoRepository eventoRepository;

 public ReputacaoService(ReputacaoRepository repository, EventoProcessadoRepository eventoRepository) {
  this.repository = repository;
  this.eventoRepository = eventoRepository;
 }


 @Transactional
 public void registrarContratoConcluido(UUID eventId, UUID contratoId, UUID freelancerId, BigDecimal valor) {
  if (eventoRepository.existsById(eventId)) {
   log.info("reputacao.evento.duplicado eventId={} contratoId={}", eventId, contratoId);
   return;
  }

  log.info("reputacao.atualizacao.inicio eventId={} contratoId={} freelancerId={}", eventId, contratoId, freelancerId);

  var reputacao = repository.findById(freelancerId).orElseGet(() -> new ReputacaoFreelancer(freelancerId));

  reputacao.registrarContrato(valor);
  repository.save(reputacao);
  eventoRepository.save(new EventoProcessado(eventId));

  log.info(
          "reputacao.atualizacao.sucesso eventId={} contratoId={} freelancerId={} contratosConcluidos={} valorTotal={}",
          eventId,
          contratoId,
          freelancerId,
          reputacao.contratosConcluidos,
          reputacao.valorTotal
  );
 }
}
