# Freela Marketplace — Microsserviços orientados a eventos

Projeto de marketplace de contratação de freelancers, desenvolvido usando Java, Spring Boot e Spring Cloud. O núcleo gerencia contratos; eventos publicados no Apache Kafka atualizam notificações, reputação e auditoria de forma assíncrona.

## Arquitetura

```text
Cliente HTTP
    |
API Gateway (:8080) <--> Eureka (:8761)
    |
contrato-service (:8081) ---> PostgreSQL / contrato_db
    |
    +-- Transação: contrato + eventos_pendentes (Outbox)
    |
    +-- EventoPendentePublisher (agendado)
                |
         Kafka: contratos.eventos
                |
      +---------+----------+
      |         |          |
notificacao  reputacao   auditoria
 (:8082)     (:8083)     (:8084)
      |         |          |
 notificacao_db reputacao_db auditoria_db
```

Cada microsserviço de negócio possui seu próprio banco no PostgreSQL. Os consumidores Kafka pertencem a grupos distintos e processam os eventos de maneira independente.

| Componente | Porta local | Função |
|---|------------:|---|
| API Gateway |        8080 | Entrada HTTP e roteamento via Eureka |
| contrato-service |        8081 | Ciclo de vida dos contratos e publicação de eventos |
| notificacao-service |        8082 | Notificações de criação de contrato |
| reputacao-service |        8083 | Quantidade e valor de contratos concluídos |
| auditoria-service |        8084 | Histórico dos eventos recebidos |
| Eureka Server |        8761 | Descoberta de serviços |
| Kafka |        9092 | Broker de eventos |
| Kafka UI |        8090 | Inspeção de tópicos e mensagens |
| PostgreSQL |        5433 | Bancos de dados locais (porta 5432 dentro do container) |
| Zipkin |        9411 | Rastreamento distribuído |
| Loki |        3100 | Armazenamento e consulta de logs |
| Alloy |       12345 | Coleta de arquivos de log |
| Grafana |        3000 | Exploração de logs |

## Domínio e APIs

O agregado Contrato concentra as regras de estado e gera eventos de domínio. Fluxo principal:

```text
ATIVO -> ENTREGA_REGISTRADA -> CONCLUIDO
```

O domínio também define CANCELADO. A criação e a conclusão geram eventos; o registro de entrega altera o estado do contrato.

Endpoints do contrato, disponíveis pelo Gateway em http://localhost:8080:

| Método | Caminho | Ação |
|---|---|---|
| POST | `/api/contratos` | Criar contrato |
| GET | `/api/contratos` | Listar contratos |
| GET | `/api/contratos/{id}` | Consultar contrato |
| PATCH | `/api/contratos/{id}/entrega` | Registrar entrega |
| PATCH | `/api/contratos/{id}/concluir` | Concluir contrato |

Exemplo de criação:

```json
{
  "clienteId": "4ff24d4c-2fab-41d5-8db5-5677a336d760",
  "freelancerId": "04f76487-375d-442c-877d-f974174d21cc",
  "titulo": "Construção de página web",
  "valor": 2000.00
}
```

Consultas adicionais: GET /api/notificacoes, GET /api/reputacoes e GET /api/auditoria, conforme as rotas do Gateway.

## Eventos e Kafka

O tópico principal é **contratos.eventos**. Os tipos de evento são:

- `ContratoCriado`: incluindo cliente, freelancer, título e valor.
- `ContratoConcluido`: incluindo freelancer e valor.

Envelope usado nas mensagens recentes:

```json
{
  "eventId": "<uuid>",
  "eventType": "ContratoCriado",
  "occurredAt": "<timestamp>",
  "correlationId": "<uuid>",
  "payload": {
    "eventId": "<uuid>",
    "occurredAt": "<timestamp>",
    "contratoId": "<uuid>",
    "clienteId": "<uuid>",
    "freelancerId": "<uuid>",
    "titulo": "Construção de página web",
    "valor": 2000.00
  }
}
```

O **correlationId** do envelope atualmente utiliza o **eventId** como identificador inicial. O Gateway também suporta `X-Correlation-Id` nas requisições HTTP.

### Publicação confiável (Outbox)

O **contrato-service** salva o contrato e seu evento na tabela `eventos_pendentes` na mesma transação de banco de dados. O **EventoPendentePublisher** consulta os eventos não publicados periodicamente, envia ao Kafka e, após confirmação, os marca como publicados. Se ocorrer alguma falha antes dessa confirmação, pode haver reenvio; por isso os consumidores são idempotentes.

### Consumidores e idempotência

- **Notificação:** processa `ContratoCriado`; verifica `eventId` para evitar notificação duplicada.
- **Reputação:** processa `ContratoConcluido`; atualiza o total de contratos e valores por freelancer e registra `eventId` em `eventos_processados` na mesma transação.
- **Auditoria:** registra os eventos recebidos, incluindo JSON original e `correlationId`, e evita repetição por `eventId`.

Foram realizados testes de reenvio de mensagens com o mesmo `eventId`, sem duplicação de registros nos cenários testados. Isso não equivale a afirmar que todos os cenários de concorrência foram cobertos.

### Retry e DLT

Os consumidores usam **DefaultErrorHandler**, com **duas novas tentativas**. Após esgotar as tentativas, mensagens que continuam falhando são enviadas para:

| Consumidor | Tópico de falhas |
|---|---|
| notificacao-service | `contratos.eventos.DLT` |
| reputacao-service | `contratos.eventos.reputacao.DLT` |
| auditoria-service | `contratos.eventos.auditoria.DLT` |

Esse fluxo foi validado publicando um JSON inválido e verificando as mensagens nas DLTs pelo Kafka UI.

### Chave e ordenação

A publicação usa o **contratoId** como chave Kafka, mantendo os eventos de um mesmo contrato na mesma partição. Nos testes, `contratos.eventos` possui **uma partição**, o que preserva a ordem de publicação no tópico, mas limita o paralelismo entre partições.

## Observabilidade

- **Logs estruturados em texto:** incluem data, nível, serviço, thread, logger e mensagem. Os serviços também gravam arquivos em `logs/<nome-do-servico>.log`.
- **Loki + Alloy + Grafana:** o Alloy lê os logs dos cinco serviços e envia ao Loki; o Grafana permite consultas.
- **Zipkin:** rastreamento HTTP entre Gateway e contrato-service e rastreamento das publicações/consumos Kafka, com observação habilitada no produtor e nos listeners.

Interfaces locais:

- Eureka: http://localhost:8761
- Kafka UI: http://localhost:8090
- Zipkin: http://localhost:9411
- Grafana: http://localhost:3000

## Execução local

Pré-requisitos: Java 21, Maven e Docker.

Na raiz do projeto:

```bash
cd infra
docker compose up -d
```

Depois, iniciar os módulos Spring Boot, preferencialmente na ordem: **eureka-server**, **api-gateway**, **contrato-service**, **notificacao-service**, **reputacao-service** e **auditoria-service**. Pelo IntelliJ, execute as respectivas classes principais. 

Para interromper os containers sem remover os volumes:

```bash
cd infra
docker compose stop
```


## Evidências

As capturas de tela dos testes devem foram colocadas em **docs/prints/**. Entre os cenários registrados estão: persistência nos bancos, idempotência, publicação Kafka, DLT, correlação na auditoria, traces Zipkin e logs no Grafana.