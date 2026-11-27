# Arquitetura do Sistema

Sistema de agendamento e gerenciamento de serviços de beleza e bem-estar (Tech Challenge Java, Fase 3, Substitutiva).

Documentos relacionados: [Modelagem de Dados](modelagem-de-dados.md) · [Planejamento de Desenvolvimento](planejamento-de-desenvolvimento.md)

## 1. Visão geral

O sistema é uma API REST em um **monolito modular com Clean Architecture**. O enunciado avalia o backend, portanto não há frontend nesse escopo: o painel do estabelecimento é entregue como endpoints documentados no Swagger.

| Atributo | Como é atendido |
|---|---|
| Regras isoladas de framework | `domain` e `application` sem Spring ou JPA, verificado com ArchUnit |
| Reservas simultâneas sem conflito | Restrição de exclusão no PostgreSQL |
| Notificações confiáveis | Outbox gravada na transação do agendamento |
| Falhas externas não bloqueiam a reserva | Google Calendar e e-mail processados por jobs, com novas tentativas |
| Escala horizontal | API sem estado (JWT), várias tasks no ECS, jobs protegidos com `SKIP LOCKED` |

## 2. Diagrama de contexto

```mermaid
flowchart LR
    cliente["Cliente"]
    profissional["Profissional"]
    gestor["Gestor do estabelecimento"]
    sistema["Sistema de agendamento"]
    google["Google Calendar API"]
    email["Servidor de e-mail"]

    cliente -->|"busca, agenda e avalia"| sistema
    profissional -->|"mantém perfil e agenda"| sistema
    gestor -->|"cadastra e gerencia"| sistema
    sistema -->|"sincroniza eventos via OAuth2"| google
    sistema -->|"envia confirmações e lembretes"| email
```

## 3. Diagrama de contêineres

```mermaid
flowchart TB
    usuarios["Clientes de API e Swagger UI"]

    subgraph app["Aplicação Spring Boot"]
        api["API REST"]
        jobs["Jobs agendados: notificações e calendário"]
    end

    db[("PostgreSQL")]
    fotos[("S3 / MinIO")]
    smtp["SMTP: Amazon SES / Mailpit"]
    google["Google Calendar API"]

    usuarios -->|"HTTPS + JWT"| api
    api --> db
    api -->|"upload de fotos"| fotos
    jobs --> db
    jobs --> smtp
    jobs --> google
```

## 4. Clean Architecture

### 4.1 Camadas

```mermaid
flowchart LR
    infrastructure["infrastructure<br/>Spring, configuração, segurança"]
    adapters["adapters<br/>controllers, JPA, gateways"]
    application["application<br/>casos de uso e portas"]
    domain["domain<br/>entidades e regras"]

    infrastructure --> adapters
    adapters --> application
    application --> domain
```

As dependências sempre apontam para dentro.

| Camada | Conceito do enunciado | Responsabilidade | Exemplos |
|---|---|---|---|
| `domain` | Entidades | Regras de negócio puras | `Agendamento`, `StatusAgendamento`, `AgendaProfissional`, `Avaliacao` |
| `application` | Casos de uso | Orquestrar um fluxo e declarar portas | `CriarAgendamentoUseCase`, `AgendamentoRepository` (porta) |
| `adapters` | Adaptadores | Traduzir HTTP, banco e APIs externas | `AgendamentoController`, `AgendamentoJpaAdapter`, `GoogleCalendarGateway` |
| `infrastructure` | Frameworks externos | Montar a aplicação | `BeanConfig`, `SecurityConfig`, `@Scheduled` |

### 4.2 Regras verificadas com ArchUnit

As regras ficam em `src/test/java/br/com/fiap/agendabeleza/api/arquitetura/RegrasArquiteturaTest.java` e rodam a cada build.

- As camadas só dependem para dentro: `infrastructure` → `adapters` → `application` → `domain`.
- `domain` depende somente de `domain` e da biblioteca padrão do Java.
- `application` depende somente de `domain` e da biblioteca padrão do Java.
- `domain` e `application` não importam `org.springframework..`, `jakarta.persistence..` ou `com.google.api..`.
- Um módulo só acessa outro pelos contratos de `application` ou por eventos, nunca por `adapters`, `infrastructure` ou tabelas JPA alheias.
- `compartilhado` não depende de nenhum módulo de negócio.
- Controllers não acessam repositórios diretamente.
- Classes `@Entity` ficam somente em `adapters.out.persistence`.

### 4.3 Módulos

| Módulo | Responsabilidade | Funcionalidade do enunciado |
|---|---|---|
| `identidade` | Cadastro, login e papéis | Base para todas |
| `estabelecimentos` | Dados, fotos, horários e serviços | 1 |
| `profissionais` | Perfil, especialidades, tarifas, disponibilidade e bloqueios | 2 |
| `agendamentos` | Horários livres, reserva, status e painel | 3, 6 |
| `avaliacoes` | Avaliações e notas agregadas | 4 |
| `busca` | Consulta com filtros combinados | 5 |
| `notificacoes` | Outbox, confirmações e lembretes | 3b |
| `calendario` | Feed iCalendar (`.ics`) por URL secreta, para profissional e cliente | 7 |
| `compartilhado` | Exceções base, eventos e tipos comuns | Apoio |

### 4.4 Estrutura de pacotes

Projeto Maven de módulo único. As fronteiras são garantidas por pacotes e ArchUnit, sem o custo de um build multimódulo.

```text
src/main/java/br/com/fiap/agendabeleza/api/
├── AgendaBelezaApplication.java
├── agendamentos/
│   ├── domain/
│   │   ├── Agendamento.java
│   │   ├── StatusAgendamento.java
│   │   ├── AgendaProfissional.java
│   │   └── HorarioIndisponivelException.java
│   ├── application/
│   │   ├── usecase/
│   │   │   ├── CriarAgendamentoUseCase.java
│   │   │   ├── AlteracaoDeAgendamentoUseCase.java   (base das ações de gestão)
│   │   │   ├── ReagendarAgendamentoUseCase.java
│   │   │   └── ConsultarHorariosLivresUseCase.java
│   │   └── port/
│   │       └── AgendamentoRepository.java
│   ├── adapters/
│   │   ├── in/web/
│   │   │   ├── AgendamentoController.java
│   │   │   └── dto/
│   │   └── out/persistence/
│   │       ├── AgendamentoJpaEntity.java
│   │       ├── AgendamentoJpaRepository.java
│   │       └── AgendamentoJpaAdapter.java
│   └── infrastructure/
│       └── AgendamentosConfig.java
├── estabelecimentos/   (mesma estrutura)
├── profissionais/      (mesma estrutura)
├── identidade/         (mesma estrutura)
├── avaliacoes/         (mesma estrutura)
├── busca/              (mesma estrutura)
├── notificacoes/       (mesma estrutura)
├── calendario/         (mesma estrutura)
└── compartilhado/
```

### 4.5 Decisões de implementação

- **Entidade de domínio x entidade JPA:** são classes separadas, convertidas no adaptador. Há mais código, mas o domínio fica independente e testável sem banco.
- **Casos de uso sem Spring:** são classes Java comuns, registradas como beans em `infrastructure`.
- **Transações:** a porta `GerenciadorTransacao` isola o Spring, e sua implementação usa `TransactionTemplate`. Assim, `application` não precisa de `@Transactional`.
- **E-mail:** a porta `NotificacaoGateway` (`notificacoes/application/port`) recebe uma `Mensagem` já validada. O `NotificacaoSmtpAdapter` envia por SMTP com o `JavaMailSender` e traduz `MailException` em `FalhaNoEnvioException`, então o núcleo não conhece o Spring. Localmente o servidor é o Mailpit (`spring.mail.host`/`port`).
- **Eventos entre módulos:** a porta `PublicadorEventos` usa o `ApplicationEventPublisher` do Spring. O ouvinte de notificações roda de forma síncrona, na mesma transação, para gravar a outbox.

## 5. API REST

Prefixo `/api/v1`. A documentação é gerada pelo springdoc em `/swagger-ui.html`. Os erros seguem o formato `application/problem+json` (RFC 9457).

### 5.1 Autenticação e calendário

As rotas `/auth/*` (JWT) e `/integracoes/google/*` (OAuth2) saíram do escopo em 20/11 (planejamento, seção 2.1). O usuário chega no cabeçalho `X-Usuario-Id`, e a integração com calendários é um feed iCalendar que o Google Calendar e o Outlook assinam por URL (implementado em 26/11):

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/calendario/assinaturas/profissionais/{profissionalId}` | Gestor do estabelecimento ou o próprio profissional | Gera a URL secreta da agenda do profissional. Responde 201 com `{ url, webcal }` e a mesma URL no `Location`; 400 sem `X-Usuario-Id`, 403 para os demais usuários e 404 para profissional inexistente |
| POST | `/calendario/assinaturas/cliente` | O cliente do `X-Usuario-Id` | Gera a URL secreta dos horários do cliente, em todos os estabelecimentos. Responde 201 com `{ url, webcal }`; 400 sem `X-Usuario-Id` |
| GET | `/calendario/feeds/{token}.ics` | Público (o token é a credencial) | Feed `text/calendar; charset=utf-8`. Responde 404, sem diferenciar, para token malformado, desconhecido ou já trocado |

Cada `POST` gera um token novo e invalida o anterior do mesmo titular: é assim que um link vazado é revogado. O token tem 256 bits aleatórios em Base64 para URL (43 caracteres), e o banco guarda só o SHA-256 dele. O feed traz os agendamentos que terminam a partir de 30 dias atrás, sem limite à frente e sem os cancelados (um evento que some do feed some da agenda assinada). `AGENDADO` sai como `STATUS:TENTATIVE`; confirmado, concluído e não comparecimento, como `CONFIRMED`. Os horários vão em UTC, sem `VTIMEZONE`, e cada calendário os mostra no fuso de quem assina. O `UID` é fixo por agendamento e o `SEQUENCE` é a versão dele, para que um reagendamento troque o evento em vez de duplicá-lo. No feed do profissional, o título é o serviço; no do cliente, "serviço em estabelecimento". A URL é montada a partir do endereço que o cliente usou: atrás de túnel ou da plataforma em nuvem, `server.forward-headers-strategy=framework` faz valer os cabeçalhos `X-Forwarded-*`.

### 5.2 Estabelecimentos e profissionais

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/estabelecimentos` | Gestor | Cadastra estabelecimento |
| GET | `/estabelecimentos/{id}` | Público | Consulta detalhes |
| PUT | `/estabelecimentos/{id}` | Gestor dono | Atualiza dados |
| POST | `/estabelecimentos/{id}/fotos` | Gestor dono | Adiciona foto informada como URL (`{ "url": ... }`) |
| DELETE | `/estabelecimentos/{id}/fotos/{fotoId}` | Gestor dono | Remove foto |
| PUT | `/estabelecimentos/{id}/horarios` | Gestor dono | Define horário de funcionamento |
| POST | `/estabelecimentos/{id}/servicos` | Gestor dono | Cadastra serviço |
| GET | `/estabelecimentos/{id}/servicos` | Público | Lista os serviços ativos |
| PUT | `/estabelecimentos/{id}/servicos/{servicoId}` | Gestor dono | Atualiza, ativa ou desativa serviço |
| POST | `/estabelecimentos/{id}/profissionais` | Gestor dono | Cadastra profissional. Retorna 409 se o `usuarioId` já estiver vinculado a outro profissional |
| GET | `/estabelecimentos/{id}/profissionais` | Público | Lista profissionais ativos, sem e-mail |
| GET | `/especialidades` | Público | Lista o catálogo de especialidades |
| GET | `/profissionais/{id}` | Público | Consulta perfil, com os nomes das especialidades e sem e-mail |
| PUT | `/profissionais/{id}` | Gestor ou próprio | Atualiza perfil |
| PUT | `/profissionais/{id}/especialidades` | Gestor ou próprio | Define especialidades |
| PUT | `/profissionais/{id}/servicos` | Gestor ou próprio | Define serviços e tarifas. Tarifa ausente assume o preço base |
| PUT | `/profissionais/{id}/disponibilidades` | Gestor ou próprio | Define jornada semanal, dentro do funcionamento do estabelecimento |
| POST | `/profissionais/{id}/bloqueios` | Gestor ou próprio | Cria bloqueio `[inicio, fim)`. Retorna 422 se o fim não for posterior ao início ou se o bloqueio já tiver terminado e 409 se houver agendamento ativo no período |
| GET | `/profissionais/{id}/bloqueios` | Gestor ou próprio | Lista os bloqueios que ainda não terminaram, ordenados pelo início |
| DELETE | `/profissionais/{id}/bloqueios/{bloqueioId}` | Gestor ou próprio | Remove bloqueio. Retorna 404 se ele não existir ou for de outro profissional |

Os bloqueios recebem `inicio` e `fim` com fuso (`2026-11-30T10:00:00-03:00`) e são devolvidos em UTC (`2026-11-30T13:00:00Z`).

### 5.3 Agendamentos e painel

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/profissionais/{id}/horarios-livres?servicoId&data` | Público | Lista horários disponíveis, descontando bloqueios e agendamentos ativos. Retorna 404 se o profissional, o serviço ou o estabelecimento não existir e 422 se algum deles estiver inativo, se o serviço for de outro estabelecimento ou se o profissional não o executar |
| POST | `/agendamentos` | Cliente | Cria agendamento com a tarifa do profissional e registra a confirmação por e-mail para o cliente (`emailCliente`, obrigatório) e para o profissional. Retorna 400 para campo ausente ou `emailCliente` inválido, 404 para profissional ou serviço inexistente, 422 para horário no passado, fora do atendimento ou em bloqueio do profissional e 409 para horário ocupado |
| GET | `/agendamentos/meus` | Cliente | Lista histórico próprio, do mais recente para o mais antigo |
| GET | `/agendamentos/{id}` | Partes envolvidas | Consulta detalhes. Cliente, gestor ou profissional; os demais recebem 403 |
| POST | `/agendamentos/{id}/confirmar` | Gestor ou profissional | Confirma um agendamento `AGENDADO`. Retorna 422 para os demais status |
| POST | `/agendamentos/{id}/reagendar` | Cliente ou gestor | Recebe `{ "novoInicio" }` e mantém a duração; o status volta para `AGENDADO`. O novo horário passa pelas regras da reserva, sem contar o horário atual do próprio agendamento: 422 para agendamento inativo, início no passado ou igual ao atual, bloqueio ou fora do atendimento, e 409 para outro agendamento no horário |
| POST | `/agendamentos/{id}/cancelar` | Cliente ou gestor | Cancela um agendamento ativo e libera o horário. Corpo opcional `{ "motivo" }`, de até 250 caracteres, gravado no histórico |
| POST | `/agendamentos/{id}/concluir` | Gestor ou profissional | Conclui um agendamento `CONFIRMADO`, a partir do início. Retorna 422 antes do início ou para os demais status |
| POST | `/agendamentos/{id}/nao-comparecimento` | Gestor ou profissional | Registra a falta em um agendamento `CONFIRMADO`, a partir do início. Retorna 422 antes do início ou para os demais status |

As cinco ações de gestão respondem 200 com o agendamento como ficou gravado, 404 se ele não existir, 403 para quem não tem a permissão indicada, 422 para uma transição fora do diagrama de status (modelagem, seção 4.2) e 409 se outra operação tiver gravado o agendamento antes (bloqueio otimista). Cada uma grava uma linha em `agendamento_historico`.

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/estabelecimentos/{id}/agendamentos?de&ate&status&profissionalId` | Gestor dono | Painel: agendamentos que começam no período, em ordem de início. `status` pode se repetir (`status=AGENDADO&status=CONFIRMADO`); sem ele, todos os status |
| GET | `/estabelecimentos/{id}/painel/resumo?de&ate&profissionalId` | Gestor dono | Painel: total, total por status (os cinco, com zero quando não houver), receita realizada (`CONCLUIDO`) e receita prevista (`AGENDADO` e `CONFIRMADO`) |

As duas rotas do painel ficam no `PainelEstabelecimentoController` do módulo de agendamentos. `de` e `ate` são datas (`aaaa-mm-dd`) no fuso do estabelecimento, e o período inclui os dois dias: vira `[de 00:00, ate+1 00:00)`. Sem datas, vale o dia de hoje; só com uma delas, vale esse dia. Respondem 400 para data, status ou UUID malformado, 403 se o usuário não for o gestor, 404 se o estabelecimento não existir e 422 se `ate` for anterior a `de` ou se o período passar de 31 dias. O resumo reaproveita a consulta da agenda (`ResumirAgendaDoEstabelecimentoUseCase` compõe `ConsultarAgendaDoEstabelecimentoUseCase`), e a parte comum, que carrega o estabelecimento, exige o gestor e resolve o período, fica em `PeriodoDoPainel`. No adaptador, os filtros opcionais são `Specification` combináveis (`AgendamentoEspecificacoes`), apoiados no índice `idx_agendamento_estab_inicio`.

Resposta do resumo:

```json
{
  "de": "2030-03-04",
  "ate": "2030-03-04",
  "fuso": "America/Sao_Paulo",
  "total": 6,
  "porStatus": { "AGENDADO": 2, "CONFIRMADO": 1, "CONCLUIDO": 2, "CANCELADO": 1, "NAO_COMPARECEU": 0 },
  "receitaRealizada": 180.00,
  "receitaPrevista": 120.00
}
```

Resposta de horários livres, com os horários no fuso do estabelecimento e `fim` exclusivo:

```json
{
  "data": "2026-11-30",
  "fuso": "America/Sao_Paulo",
  "duracaoMinutos": 60,
  "horarios": [{ "inicio": "2026-11-30T09:00:00-03:00", "fim": "2026-11-30T10:00:00-03:00" }]
}
```

### 5.4 Avaliações e busca

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/agendamentos/{id}/avaliacao` | Cliente do agendamento | Avalia atendimento concluído com `{ "notaEstabelecimento", "notaProfissional", "comentario"? }`, notas de 1 a 5 e comentário de até 1000 caracteres. Responde 201, 400 para nota ausente ou fora da escala e comentário longo, 404 para agendamento inexistente, 403 para quem não é o cliente e 422 para atendimento não concluído ou já avaliado |
| GET | `/estabelecimentos/{id}/avaliacoes` | Público | Lista avaliações, da mais recente para a mais antiga, sem o `clienteId`. 404 se o estabelecimento não existir |
| GET | `/profissionais/{id}/avaliacoes` | Público | Lista avaliações, da mais recente para a mais antiga, sem o `clienteId`. 404 se o profissional não existir |
| GET | `/busca/estabelecimentos` | Público | Busca paginada de estabelecimentos ativos com filtros combinados. Responde 200 com `{ itens, pagina, tamanho, totalDeItens, totalDePaginas }`; 400 para número, data, hora, categoria ou ordem malformados, página negativa, tamanho fora de 1 a 50, nota mínima fora de 0 a 5 e preço negativo; 422 se `precoMinimo` for maior que `precoMaximo`, se a `data` estiver no passado, se houver hora sem `data` ou se `horaInicio` não for anterior a `horaFim` |

A avaliação roda em `ExecucaoSerializada` com a chave do estabelecimento, que já abre uma transação e serializa as avaliações dele. Dentro dela, o caso de uso grava a avaliação e recalcula a nota média e o total do estabelecimento e do profissional a partir da soma de todas as avaliações (`NotaMedia`, duas casas, arredondamento `HALF_UP`). Como o profissional pertence a um único estabelecimento, a mesma trava protege as duas médias. As colunas `nota_media` e `total_avaliacoes` são só leitura nas entidades JPA e mudam apenas por `atualizarAvaliacoes`, para que uma edição do gestor ou do perfil feita ao mesmo tempo não sobrescreva as notas. `GET /estabelecimentos/{id}` e `GET /profissionais/{id}` devolvem `notaMedia` (nula até a primeira avaliação) e `totalAvaliacoes`.

Filtros de `/busca/estabelecimentos`, combináveis entre si:

| Critério | Parâmetros |
|---|---|
| Nome | `nome` |
| Serviço | `servico`, `categoria` |
| Localização | `cidade`, `bairro` |
| Avaliação | `notaMinima` |
| Faixa de preço | `precoMinimo`, `precoMaximo` |
| Disponibilidade | `data` (`aaaa-mm-dd`), `horaInicio`, `horaFim` (`hh:mm`, fim exclusivo) |
| Paginação e ordenação | `pagina` (a partir de 0), `tamanho` (padrão 20, máximo 50), `ordem` (`MELHOR_AVALIADOS`, padrão, ou `NOME`) |

Os nomes dos parâmetros são completos, sem abreviação (`precoMinimo`, e não `precoMin`). Nome do estabelecimento e do serviço são buscados por trecho; cidade e bairro, pelo valor exato; tudo sem diferenciar maiúsculas, e `%` e `_` digitados valem como texto. Serviço, categoria e faixa de preço valem para **o mesmo serviço ativo**, e o preço é a tarifa de um profissional ativo, que é o valor cobrado na reserva. Com `notaMinima`, quem ainda não foi avaliado fica de fora; na ordem `MELHOR_AVALIADOS`, vai para o fim. Toda ordem desempata pelo nome e pelo identificador, para que as páginas sejam estáveis. A busca por raio de distância foi cortada (planejamento, seção 2.1).

Com `data`, entra o estabelecimento que tem um profissional ativo cuja jornada naquele dia da semana, o funcionamento do estabelecimento nesse dia e o horário pedido têm algum instante em comum (intervalos semiabertos: só encostar não conta). Sem horas, vale o dia inteiro; só com `horaInicio`, até o fim do dia; só com `horaFim`, desde a meia-noite. Com filtro de serviço, categoria ou preço, esse profissional precisa executar um serviço que atenda ao filtro. Agendamentos e bloqueios **não** são descontados: a busca aponta onde procurar, e o horário exato sai de `GET /profissionais/{id}/horarios-livres`. Data, horas e colunas `time` estão no fuso do estabelecimento, então não há conversão; só o "hoje" que recusa data no passado usa o fuso padrão (`America/Sao_Paulo`) com o `Clock` injetado. No adaptador, é um segundo `EXISTS`, sobre profissional, jornada e funcionamento, que reaproveita as condições de serviço e de faixa de preço do primeiro.

O módulo `busca` é um modelo de leitura: como as regras ArchUnit impedem usar os adaptadores de outro módulo, ele tem entidades JPA próprias e somente leitura (`@Immutable`) sobre `estabelecimento`, `servico`, `profissional_servico` e `profissional`. Os filtros são `Specification` combináveis (`BuscaEspecificacoes`), com um único `EXISTS` correlacionado para serviço, categoria e preço, o que também evita repetir o estabelecimento no resultado. A paginação no núcleo usa `Paginacao` e `Pagina` (`compartilhado/domain`), e o adaptador as converte para o `PageRequest` do Spring Data.

## 6. Fluxos principais

### 6.1 Criar agendamento

```mermaid
sequenceDiagram
    actor C as Cliente
    participant API as AgendamentoController
    participant UC as CriarAgendamentoUseCase
    participant R as AgendamentoRepository
    participant DB as PostgreSQL

    C->>API: POST /agendamentos
    API->>UC: executar(comando)
    UC->>R: carregar serviço, tarifa e agenda
    UC->>UC: validar funcionamento, disponibilidade e bloqueios
    UC->>R: salvar agendamento
    R->>DB: INSERT agendamento, histórico e outbox (mesma transação)
    alt Horário livre
        DB-->>R: sucesso
        UC-->>API: agendamento criado
        API-->>C: 201 Created
    else Outra reserva venceu a corrida
        DB-->>R: erro 23P01
        R-->>UC: HorarioIndisponivelException
        API-->>C: 409 Conflict
    end
```

### 6.2 Notificações

```mermaid
sequenceDiagram
    participant Job as Job de notificações
    participant DB as PostgreSQL
    participant Mail as NotificacaoGateway

    loop A cada minuto (fixedDelay)
        Job->>DB: SELECT pendentes com enviar_em vencido (até 50)
        Job->>Mail: enviar e-mail
        alt Sucesso
            Job->>DB: status = ENVIADA
        else Falha
            Job->>DB: tentativas + 1 (FALHA após 5)
        end
    end
```

- **Como ficou implementado (25/11):** a reserva (`CriarAgendamentoUseCase`), o cancelamento e o reagendamento pedem o aviso pela porta `AvisosDoAgendamento` (`agendamentos/application/port`) **dentro** da `ExecucaoSerializada`, depois de gravar. A porta é implementada por `AvisosPorEmail` (`notificacoes`), que grava uma notificação para o cliente e outra para o profissional. Se a reserva for recusada ou sofrer rollback, a outbox volta junto.
- O cancelamento passou a rodar em `ExecucaoSerializada` para ter uma transação; a trava por profissional só o faz esperar uma reserva ou bloqueio do mesmo profissional em andamento.
- Sem `SKIP LOCKED`: roda uma instância só, e o `fixedDelay` impede duas rodadas juntas. Com mais de uma instância, a consulta precisaria de `FOR UPDATE SKIP LOCKED`.
- O e-mail do cliente chega só na reserva (`emailCliente`) e fica em `notificacao.destinatario_email`, de papel `CLIENTE`. Os avisos seguintes o leem de lá.
- Cada reserva gera quatro registros: confirmação (imediata) e lembrete (24 h antes do início), para o cliente e para o profissional. Sem lembrete quando a reserva é feita com menos de 24 h de antecedência.
- Cancelar ou reagendar marca os lembretes pendentes como `CANCELADA`. O reagendamento grava os lembretes do novo horário. Só `PENDENTE` é enviada; `ENVIADA`, `FALHA` e `CANCELADA` são finais.
- Corrida aceita: se o cancelamento acontecer no mesmo instante em que o job envia aquele lembrete, o e-mail já terá saído e o job grava `ENVIADA` por cima de `CANCELADA`. A correção, se necessária, é um update condicional (`WHERE status = 'PENDENTE'`) no adaptador.
- O job entrega cada e-mail pela porta `NotificacaoGateway`. Uma `FalhaNoEnvioException` conta como tentativa.
- O fluxo inteiro é coberto por `NotificacoesPorEmailTest`: casos de uso reais, outbox no PostgreSQL do Testcontainers, `NotificacaoSmtpAdapter` e SMTP do GreenMail. As rodadas do job são chamadas com relógio fixo para fazer o lembrete vencer.
- O envio é "pelo menos uma vez": se a task cair depois de enviar e antes de atualizar o status, o e-mail pode se repetir. Esse risco é aceitável para este domínio.

### 6.3 Assinatura do calendário (feed `.ics`)

O fluxo original, com OAuth2 e um job que gravava eventos pela API do Google, foi trocado em 20/11 por um feed que o calendário busca sozinho (planejamento, seção 2.1). A API não chama nenhum serviço externo: quem busca é o Google ou o Outlook.

```mermaid
sequenceDiagram
    actor U as Profissional ou cliente
    participant API as API
    participant DB as PostgreSQL
    participant G as Google Calendar ou Outlook

    U->>API: POST /calendario/assinaturas/... (X-Usuario-Id)
    API->>API: Confere o acesso (TitularDoCalendario)
    API->>DB: Apaga a assinatura anterior e grava o SHA-256 do token novo
    API-->>U: 201 { url, webcal }
    U->>G: Cola a URL em "Outras agendas, Do URL"
    loop A cada atualização do calendário
        G->>API: GET /calendario/feeds/{token}.ics
        API->>DB: Assinatura pelo hash e agendamentos do titular
        API-->>G: text/calendar (CalendarioGateway → CalendarioIcsAdapter)
    end
```

O que muda entre profissional e cliente (quem pode assinar, quais agendamentos entram e os títulos) fica na estratégia `TitularDoCalendario`, com `CalendarioDoProfissional` e `CalendarioDoCliente`. A emissão e a geração do feed são as mesmas para os dois papéis. O `CalendarioGateway` isola o formato: o núcleo monta um `FeedDoCalendario`, e o `CalendarioIcsAdapter` escreve o RFC 5545 à mão, com escape de texto, dobra de linha em 75 octetos e CRLF. O feed pede atualização a cada hora (`REFRESH-INTERVAL` e `X-PUBLISHED-TTL`). O Outlook respeita; o Google Calendar decide sozinho, em geral a cada várias horas, então uma alteração não aparece na hora na agenda assinada.

## 7. Segurança

- **Autenticação:** JWT assinado, com expiração curta. Senhas em BCrypt.
- **Autorização:** papéis no `SecurityConfig` e verificação de posse nos casos de uso, como "o gestor é dono deste estabelecimento".
- **Feed do calendário (no lugar do OAuth2, cortado em 20/11):** a URL leva um token de 256 bits gerado com `SecureRandom`, e o banco guarda só o SHA-256 dele, então um vazamento da tabela não abre nenhum feed. Token malformado, desconhecido e trocado recebem o mesmo 404, e o `toString()` do token não o expõe em logs. Não há expiração; a revogação é emitir um token novo.
- **Segredos:** variáveis de ambiente no local e Secrets Manager na AWS. Nada é versionado no Git.
- **Entradas:** Bean Validation nos DTOs. Fotos limitadas por tipo e tamanho.

## 8. Tecnologias

| Finalidade | Escolha |
|---|---|
| Linguagem e framework | Java 21, Spring Boot 3 |
| Build | Maven |
| Persistência | Spring Data JPA, PostgreSQL 16, Flyway |
| Segurança | Spring Security, JWT |
| Documentação | springdoc-openapi (Swagger), Javadoc |
| Integração com calendário | Biblioteca cliente da Google Calendar API |
| Contêineres | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| Infraestrutura como código | Terraform |

## 9. Estratégia de qualidade

| Critério da avaliação | Ferramenta | O que cobre | Quando roda |
|---|---|---|---|
| Testes unitários (TDD) | JUnit 5, Mockito, AssertJ | Domínio e casos de uso | Todo push |
| Cobertura | JaCoCo | Mínimo de 80% em `domain` e `application`; abaixo disso o build falha | Todo push |
| Regras de arquitetura | ArchUnit | Regras da seção 4.2 | Todo push |
| Testes integrados | Testcontainers, WireMock | Repositórios, restrição de conflito, jobs, Google simulado | Todo push |
| Controllers e BDD | Cucumber, RestAssured | Cenários em Gherkin, em português, por funcionalidade | Todo push |
| Análise estática | SonarCloud, Checkstyle, SpotBugs | Bugs, code smells, duplicação | Todo push |
| Performance e carga | Gatling | Cenários da seção 9.3 | Manual, via `workflow_dispatch` |

### 9.1 Evidência de TDD

O histórico do Git deve mostrar o ciclo: um commit `test:` falhando, depois `feat:` e, quando houver, `refactor:`. Essa evidência será citada no Relatório Técnico.

### 9.2 Cenários BDD

As features ficam em `src/test/resources/features/<modulo>/`, escritas em Gherkin em português (`# language: pt`). A suíte `bdd/CucumberTest` roda as features no JUnit Platform e entra no `./mvnw verify` do CI. A configuração `bdd/CucumberSpringConfiguration` sobe a aplicação inteira numa porta aleatória, com PostgreSQL do Testcontainers. Os passos chamam a API por HTTP com o RestAssured 6.x, mandando o JSON como texto e lendo as respostas com o `JsonPath`, sem depender do Jackson. Cada cenário usa um gestor novo, então não é preciso limpar o banco. O relatório fica em `target/cucumber-reports/` (HTML e JSON) e é publicado no artefato `relatorios` do CI.

| Feature | Cenários | Desde |
|---|---|---|
| `estabelecimentos/cadastro_estabelecimento.feature` | Cadastro com horários e consulta pública, serviço, foto, dados inválidos (400 e 422) e alteração por outro usuário (403) | 22/11 |
| `avaliacoes/avaliacao_atendimento.feature` | Avaliação de atendimento concluído com as notas médias atualizadas, e segunda avaliação recusada (422) | 25/11 |
| `agendamentos/agendamento.feature` | Reserva que tira o horário dos livres, dois clientes no mesmo horário ao mesmo tempo (exatamente um 201 e um 409), reserva fora da jornada (422) e cancelamento que devolve o horário a outro cliente | 26/11 |

O salão dos cenários de avaliação e de agendamento (gestor, serviço "Corte" e uma profissional que atende às segundas) fica em `bdd/SalaoDoCenario`. Os passos das duas features o recebem por `@Autowired`, já que uma classe que declara passos não pode ser herdada no Cucumber. A base `ApiDaAplicacao` reúne os auxiliares `idCriado`, `exigirSucesso` e `exigirRecusa` (status e `detail` do problem+json).

O cenário da disputa:

```gherkin
  Cenário: Dois clientes tentam reservar o mesmo horário ao mesmo tempo
    Quando dois clientes solicitam o horário de "2030-03-11T14:00:00-03:00" ao mesmo tempo
    Então exatamente uma reserva é criada com status 201
    E a outra é recusada com status 409 e a mensagem "O profissional já tem um agendamento nesse horário"
```

As duas reservas partem de duas threads presas numa `CyclicBarrier` e chegam juntas ao servidor real, por HTTP. Ele é a mesma prova do `ConcorrenciaAgendaTest`, agora de ponta a ponta.

### 9.3 Cenários de carga

As metas são próprias do projeto, já que o enunciado não define números. A carga original (200 usuários por 5 minutos e 100 reservas por segundo por 2 minutos) foi pensada para a AWS. Em 26/11 ela foi revista para o Docker Compose local do notebook, onde a medição é feita. Os tempos-meta não mudaram.

| Cenário | Simulação | Carga | Meta |
|---|---|---|---|
| Busca com filtros | `BuscaComFiltrosSimulation` | 100 usuários simultâneos por 2 minutos, com rampa de 10 s e pausa de 1 s | p95 < 500 ms, erros < 1% |
| Consulta de horários livres | `HorariosLivresSimulation` | 100 usuários simultâneos por 2 minutos, com rampa de 10 s e pausa de 1 s | p95 < 500 ms, erros < 1% |
| Reservas em horários distintos | `ReservasEmHorariosDistintosSimulation` | 30 reservas por segundo durante 1 minuto (1.800) | p95 < 800 ms, erros < 1% |
| Disputa pelo mesmo horário | `DisputaPeloMesmoHorarioSimulation` | 100 requisições simultâneas | Exatamente um 201 e 99 respostas 409 |

As simulações ficam no projeto Maven separado `agendabeleza-carga/`, com Gatling 3.16 na Java DSL e sem o parent do Spring Boot, para não misturar as versões de Netty e Jackson do Boot 4 com as do Gatling. Elas rodam contra a API no ar (`agendabeleza-api/mvnw -f agendabeleza-carga/pom.xml gatling:test -Dgatling.simulationClass=...`), fora do CI; o modo de rodar está em `agendabeleza-carga/README.md`. Antes da carga, cada simulação cria pela API um salão novo (`PreparacaoDaCarga`), com funcionamento e jornada todos os dias das 06:00 às 22:00 e serviço de 15 minutos. As metas são asserções do Gatling, e uma meta não atingida termina o Maven com falha. Na disputa, uma verificação final no painel do gestor exige um único agendamento gravado. Os relatórios HTML da medição de referência estão em `artefatos/carga/`, e os números, no relatório de desenvolvimento, seção 3.2.

## 10. CI/CD

```mermaid
flowchart LR
    push["Push ou PR"] --> verify["CI: mvnw verify<br/>unitários, Testcontainers, BDD, ArchUnit, JaCoCo"]
    verify --> sonar["SonarCloud"]
    push --> mudou{"Mudou agendabeleza-api/?"}
    mudou -->|"sim"| image["Workflow Imagem: build do alvo production"]
    image --> ghcr["GHCR: agendabeleza-api com a tag do commit"]
    ghcr -.->|"terraform apply"| vps["VPS"]
```

Revisto em 27/11/2026. Dois workflows:
- `ci.yml` roda `./mvnw -B verify` e, com `SONAR_TOKEN`, a análise no SonarCloud.
- `imagem.yml` publica a imagem `production` no GitHub Container Registry, com a tag do commit e a da branch.

O deploy não é automático: o autor roda o `terraform apply` com a tag escolhida (seção 11). O pacote do GHCR é público, e a VPS baixa a imagem sem credencial.

## 11. Deploy

### 11.1 Ambientes

Revisto em 27/11/2026: a nuvem é uma VPS Linux da HostGator que o autor já tinha, no lugar da AWS (planejamento §2.1).

| Item | Local (`dev`) | Nuvem (`production`) |
|---|---|---|
| Execução | Docker Compose (`docker-compose.yml`) | Contêineres Docker na VPS, descritos em Terraform (`infra/terraform/`, provider `kreuzwerker/docker` por SSH) |
| Imagem | Build local, alvo `development` (com JDWP) | `ghcr.io/c-nnett/agendabeleza-api:<sha>`, alvo `production` |
| Banco | PostgreSQL 16 em contêiner | PostgreSQL 16 em contêiner, com volume próprio e sem porta pública |
| Fotos | URLs no cadastro | URLs no cadastro |
| E-mail | Mailpit | Mailpit interno; a interface só abre por túnel SSH |
| HTTPS | Não | Caddy com Let's Encrypt, o único contêiner com portas públicas (80 e 443) |
| Segredos | Arquivo `.env` fora do Git | Senha do banco gerada por `random_password`, guardada só no estado do Terraform, fora do Git |
| Logs | Console | `docker logs agendabeleza-api`, sem cores |
| Endereço | `http://localhost:8082` | `https://143-95-161-158.sslip.io` |

### 11.2 Topologia na VPS

```mermaid
flowchart LR
    internet["Internet"] -->|"80 e 443"| caddy["caddy<br/>HTTPS Let's Encrypt"]

    subgraph vps["VPS HostGator: AlmaLinux 9, Docker"]
        subgraph rede["Rede Docker agendabeleza"]
            caddy --> api["agendabeleza-api:8080<br/>perfil production, 768 MB"]
            api --> pg[("postgres:5432")]
            api --> mail["mailpit:1025"]
        end
    end

    mail -.->|"8025 só em 127.0.0.1"| tunel["Túnel SSH do autor"]
    ghcr["GHCR"] -.->|"imagem"| api
    tf["Terraform na máquina do autor"] -.->|"Docker por SSH"| vps
```

- **Sem domínio próprio:** o endereço é `143-95-161-158.sslip.io`, de um DNS público e gratuito que devolve o IP embutido no nome. Com um nome, o Let's Encrypt emite o certificado, e o HTTPS evita problemas na assinatura do feed `.ics` no Google Calendar.
- **Por que o Caddy:** ele emite e renova o certificado sozinho, e a configuração é um template versionado (`Caddyfile.tftpl`) aplicado pelo Terraform. Em 27/11 o autor cogitou httpd e Nginx Proxy Manager. O httpd exigiria `mod_md` ou certbot. O NPM é configurado por interface fora do Git e expõe um painel na porta 81.
- **Atrás do proxy:** com `server.forward-headers-strategy=framework`, a API monta as URLs, como a do feed `.ics`, com `https://` e o domínio público.
- **Memória:** a VPS tem 1,7 GB e não tem swap. A API fica limitada a 768 MB (a JVM usa 75%) e usou cerca de 360 MB em repouso.
- **Atualizar, voltar atrás e encerrar:** `terraform apply -var versao_da_imagem=<sha>` recria só a API; `terraform destroy` remove tudo, inclusive o banco (`infra/terraform/README.md`).

## 12. Decisões arquiteturais

| # | Decisão | Motivo | Consequência |
|---|---|---|---|
| ADR-01 | Monolito modular em vez de microsserviços | O enunciado não exige distribuição, e o prazo favorece um único deployable | Extrair serviços depois exige trocar eventos internos por mensageria |
| ADR-02 | Evitar conflito com `EXCLUDE` no banco | Garante a regra sob concorrência, sem bloqueios distribuídos | Vínculo com PostgreSQL, compensado por testes com Testcontainers |
| ADR-03 | Outbox em tabela, sem broker | Atomicidade e novas tentativas com menos infraestrutura | Polling a cada minuto, suficiente para e-mails |
| ADR-04 | `FOR UPDATE SKIP LOCKED` nos jobs | Evita processamento duplicado entre tasks | Implementado em SQL nativo no adaptador |
| ADR-05 | Entidades de domínio separadas das JPA | Independência de framework, critério central da avaliação | Mais classes e mapeadores |
| ADR-06 | Busca em SQL com `pg_trgm` em vez de Elasticsearch | Atende ao requisito com menos componentes | Menos recursos de relevância textual |
| ADR-07 | `CalendarioGateway` gera um feed iCalendar que o Google Calendar e o Outlook assinam por URL secreta (revisto em 20/11 e implementado em 26/11; antes: OAuth2 com a API do Google) | Atende aos dois calendários sem OAuth, sem tokens de terceiros e sem projeto no Google Cloud; o gerador é testado sem servidor externo | A atualização da agenda assinada depende do calendário (horas no Google); quem tem a URL lê a agenda, e a revogação é emitir um token novo |
| ADR-08 | ~~ECS Fargate em vez de EC2 ou EKS~~ **Revisto em 27/11:** contêineres numa VPS já existente, descritos em Terraform com o provider Docker por SSH, e Caddy como proxy HTTPS | Sem custo adicional e sem a hibernação das plataformas gratuitas; o Terraform mantém a infraestrutura como código, e o Caddy emite o certificado sozinho | Uma única instância, sem alta disponibilidade; backup manual do banco (`pg_dump`); a VPS é administrada pelo autor |

## 13. Requisitos de avaliação x arquitetura

| Item do enunciado | Onde é atendido |
|---|---|
| Clean Architecture | Seção 4 |
| Testes unitários (TDD) | Seções 9 e 9.1 |
| Testes integrados e análise de código | Seção 9 |
| Testes de controllers, CI e BDD | Seções 9.2 e 10 |
| Testes não funcionais | Seção 9.3 |
| Deploy local e em nuvem | Seção 11 |
| Documentação da API | Seção 5, Swagger e Javadoc |
