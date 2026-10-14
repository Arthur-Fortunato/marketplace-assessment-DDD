package br.com.freela.notificacao;
import org.slf4j.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.UUID;
@Service
public class NotificacaoService {
 private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);
 private final NotificacaoRepository repository;

 public NotificacaoService(NotificacaoRepository r){
  this.repository=r;
 }

 @Transactional
 public void registrar(UUID eventId, UUID contratoId, UUID destinatarioId, String tipo, String mensagem) {
  if (repository.existsByEventId(eventId)) {
   log.info("notificacao.evento.duplicado eventId={} contratoId={}", eventId, contratoId);
   return;
  }

  log.info("notificacao.registro.inicio eventId={} contratoId={} destinatarioId={} tipo={}", eventId, contratoId, destinatarioId, tipo);

  var notificacao = new Notificacao(eventId, contratoId, destinatarioId, tipo, mensagem);
  var salva = repository.save(notificacao);

  log.info("notificacao.registro.sucesso notificacaoId={} eventId={} contratoId={}", salva.id, eventId, contratoId);
 }

}
