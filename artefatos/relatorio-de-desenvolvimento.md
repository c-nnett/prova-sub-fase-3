# Relatório de Desenvolvimento

Acompanhamento diário do que foi realizado em relação ao planejamento e às métricas do projeto (Tech Challenge Java, Fase 3, Substitutiva).

Documentos relacionados: [Planejamento de Desenvolvimento](planejamento-de-desenvolvimento.md) · [Arquitetura do Sistema](arquitetura-do-sistema.md) · [Modelagem de Dados](modelagem-de-dados.md)

## 1. Situação atual

Atualizado em: -

| Indicador | Planejado até a data | Realizado | Desvio |
|---|---|---|---|
| Horas acumuladas | - | 0h | - |
| Tarefas concluídas | - | 0 de 86 | - |
| Dias com "Pronto quando" atendido | - | 0 de 28 | - |
| Funcionalidades concluídas | - | 0 de 7 | - |

**Tendência:** sem registros até o momento.

**Status usados nas tabelas:** Pendente, Concluído, Parcial, Não realizado. O desvio é sempre realizado menos planejado: negativo indica atraso.

## 2. Acompanhamento diário

Horas e tarefas planejadas vêm da seção 4 do planejamento. Horas extras nos feriados de 02/11 e 20/11 contam como folga para o cronograma.

| Data | Entrega planejada | Horas plan. | Horas plan. acum. | Horas real. | Tarefas plan. | Tarefas feitas | Pronto quando | Status |
|---|---|---:|---:|---:|---:|---:|---|---|
| Seg 02/11 | Projeto iniciado | 2 | 2 | - | 3 | - | - | Pendente |
| Ter 03/11 | Qualidade automatizada | 2 | 4 | - | 3 | - | - | Pendente |
| Qua 04/11 | Domínio de usuário | 2 | 6 | - | 3 | - | - | Pendente |
| Qui 05/11 | Autenticação | 2 | 8 | - | 3 | - | - | Pendente |
| Sex 06/11 | Domínio de estabelecimento | 2 | 10 | - | 2 | - | - | Pendente |
| Sáb 07/11 | API de estabelecimentos | 5,5 | 15,5 | - | 3 | - | - | Pendente |
| Dom 08/11 | Fotos e BDD (**M1**) | 5,5 | 21 | - | 5 | - | - | Pendente |
| Seg 09/11 | Domínio de profissional | 2 | 23 | - | 2 | - | - | Pendente |
| Ter 10/11 | API de profissional | 2 | 25 | - | 2 | - | - | Pendente |
| Qua 11/11 | Cálculo de horários livres | 2 | 27 | - | 2 | - | - | Pendente |
| Qui 12/11 | API de disponibilidade | 2 | 29 | - | 3 | - | - | Pendente |
| Sex 13/11 | Domínio de agendamento | 2 | 31 | - | 2 | - | - | Pendente |
| Sáb 14/11 | Criar agendamento | 5,5 | 36,5 | - | 4 | - | - | Pendente |
| Dom 15/11 | Gestão e painel (**M2**) | 5,5 | 42 | - | 4 | - | - | Pendente |
| Seg 16/11 | Confirmações | 2 | 44 | - | 3 | - | - | Pendente |
| Ter 17/11 | Lembretes | 2 | 46 | - | 3 | - | - | Pendente |
| Qua 18/11 | Domínio de avaliação | 2 | 48 | - | 2 | - | - | Pendente |
| Qui 19/11 | API de avaliações | 2 | 50 | - | 4 | - | - | Pendente |
| Sex 20/11 | Busca básica | 2 | 52 | - | 2 | - | - | Pendente |
| Sáb 21/11 | Filtros avançados e OAuth2 | 5,5 | 57,5 | - | 3 | - | - | Pendente |
| Dom 22/11 | Sincronização (**M3**) | 5,5 | 63 | - | 4 | - | - | Pendente |
| Seg 23/11 | Primeira medição de carga | 2 | 65 | - | 3 | - | - | Pendente |
| Ter 24/11 | Ajustes de performance | 2 | 67 | - | 3 | - | - | Pendente |
| Qua 25/11 | Infraestrutura base | 2 | 69 | - | 3 | - | - | Pendente |
| Qui 26/11 | Aplicação no ECS | 2 | 71 | - | 3 | - | - | Pendente |
| Sex 27/11 | Validação na nuvem (**Congelamento**) | 2 | 73 | - | 3 | - | - | Pendente |
| Sáb 28/11 | Documentação e Relatório (**M4**) | 5,5 | 78,5 | - | 5 | - | - | Pendente |
| Dom 29/11 | Revisão e envio (**Entrega**) | 5,5 | 84 | - | 4 | - | - | Pendente |

## 3. Métricas

### 3.1 Qualidade

Cada métrica passa a ser medida na data indicada, conforme a lista de conferência do planejamento.

| Métrica | Meta | Fonte | Medir a partir de | Último valor | Data da medição |
|---|---|---|---|---|---|
| CI | Verde na `main` | GitHub Actions | 03/11 | - | - |
| Regras de arquitetura | Sem violações | ArchUnit | 03/11 | - | - |
| Cobertura de `domain` e `application` | 80% ou mais | JaCoCo | 04/11 | - | - |
| Ciclo de TDD | Commit `test:` antes de `feat:` | Histórico do Git | 04/11 | - | - |
| Análise estática | Sem novos problemas críticos e quality gate aprovado | SonarCloud, Checkstyle, SpotBugs | 03/11 | - | - |
| Testes integrados | Todos passando no CI | Testcontainers, WireMock | 07/11 | - | - |
| Cenários BDD | Fluxo principal de cada funcionalidade coberto | Cucumber, RestAssured | 08/11 | - | - |

### 3.2 Carga

As metas são próprias do projeto e podem ser ajustadas após a primeira medição em 23/11.

| Cenário | Carga | Meta | Local | AWS |
|---|---|---|---|---|
| Busca com filtros | 200 usuários simultâneos por 5 minutos | p95 < 500 ms, erros < 1% | - | - |
| Consulta de horários livres | 200 usuários simultâneos por 5 minutos | p95 < 500 ms, erros < 1% | - | - |
| Reservas em horários distintos | 100 reservas por segundo durante 2 minutos | p95 < 800 ms, erros < 1% | - | - |
| Disputa pelo mesmo horário | 100 requisições simultâneas | Exatamente um 201 e 99 respostas 409 | - | - |

## 4. Marcos e funcionalidades

| Marco | Data planejada | Resultado esperado | Data real | Status |
|---|---|---|---|---|
| **M1** | 08/11 | CI completo, autenticação e cadastro de estabelecimentos funcionando | - | Pendente |
| **M2** | 15/11 | Perfil de profissionais, agendamento sem conflito e painel prontos | - | Pendente |
| **M3** | 22/11 | As 7 funcionalidades do enunciado completas | - | Pendente |
| **Congelamento** | 27/11 | Sistema na AWS. A partir daqui, somente correções | - | Pendente |
| **M4** | 28/11 | Documentação, Relatório e PDF final prontos | - | Pendente |
| **Entrega** | 29/11 | PDF revisado e enviado | - | Pendente |

| Funcionalidade | Data planejada | Data real | Status |
|---|---|---|---|
| 1. Cadastro de estabelecimentos | 08/11 | - | Pendente |
| 2. Perfil de profissionais | 12/11 | - | Pendente |
| 3. Agendamento, confirmações e lembretes | 17/11 | - | Pendente |
| 4. Avaliações e comentários | 19/11 | - | Pendente |
| 5. Busca e filtragem | 21/11 | - | Pendente |
| 6. Gerenciamento de agendamentos | 15/11 | - | Pendente |
| 7. Integração com calendários | 22/11 | - | Pendente |

## 5. Mudanças no plano

Cortes de escopo (seção 7 do planejamento), tarefas remanejadas e metas ajustadas.

| Data | Mudança | Motivo | Impacto |
|---|---|---|---|
| - | - | - | - |

## 6. Registros diários

Um registro por dia, do mais antigo para o mais recente, seguindo o modelo abaixo.

```markdown
### DD/MM (dia da semana): entrega planejada

**Resultado:** Concluído | Parcial | Não realizado
**Horas:** X h de Y h planejadas (acumulado: A h de B h, desvio de C h)
**Tarefas:** X de Y planejadas
**Pronto quando:** atendido | não atendido, com o motivo

**Realizado**
- Item concluído.

**Pendente**
- Item não concluído e nova data prevista.

**Fora do plano**
- Trabalho não previsto, se houver.

**Métricas do dia**
- Somente as medidas neste dia, com valor e meta.

**Riscos e decisões**
- Risco novo, risco que se concretizou ou decisão tomada.

**Próximo passo**
- Primeira ação do próximo dia de trabalho.
```

### 23/11 (segunda): criar agendamento

**Resultado:** Concluído
**Horas:** a preencher, de 2 h planejadas
**Tarefas:** 3 de 3 planejadas
**Pronto quando:** atendido. Um agendamento em horário ocupado é recusado com 409, pela API e em 10 reservas simultâneas (1 aceita, 9 recusadas)

**Realizado**
- `Agendamento` e a máquina de status em TDD, com as 25 combinações de transição testadas.
- Migração com `btree_gist` e a restrição `agendamento_sem_conflito` (`EXCLUDE`), adaptador JPA com bloqueio otimista e tradução do erro `23P01` para 409.
- `CriarAgendamentoUseCase`, `POST /agendamentos`, `GET /agendamentos/meus` e `GET /agendamentos/{id}`.

**Pendente**
- Nenhum item da meta. Confirmar, cancelar, reagendar e concluir seguem para 24/11, como planejado.

**Fora do plano**
- Integração com os bloqueios e os horários livres da `issue-2211`, que chegaram à `master` no mesmo dia. Os horários livres passaram a descontar os agendamentos, a reserva passou a respeitar os bloqueios, e o bloqueio passou a recusar agendamento ativo.
- Correções da revisão de código: 409 para versão desatualizada, índice para a consulta de ocupações, histórico gravado sem SELECT prévio e mensagens padronizadas.

**Métricas do dia**
- Testes: a preencher com o total do CI.
- Cobertura de `agendamentos`: 100% das linhas nos casos de uso e 99% no domínio (meta de 80%).

**Desafios e soluções**
- **Corrida entre reserva e bloqueio na agenda do mesmo profissional.**
  - *Como apareceu:* na revisão de código do dia. A reserva lia os bloqueios e depois gravava. O bloqueio lia as reservas e depois gravava. Cada passo rodava em uma transação própria, e o `EXCLUDE` do banco só compara agendamento com agendamento. Com uma reserva e um bloqueio enviados no mesmo instante, as duas verificações passavam e as duas gravações eram aceitas. O teste `ConcorrenciaAgendaTest` reproduziu o problema: sem a correção, reserva e bloqueio foram aceitos juntos na rodada das 10h.
  - *Solução:* a porta `ExecucaoSerializada` (`compartilhado/application/port`) executa a verificação e a gravação em uma única transação, com `pg_advisory_xact_lock` sobre o id do profissional. A segunda operação espera a primeira terminar e só então verifica, e recebe 409 (bloqueio) ou 422 (reserva). A trava é liberada no commit ou no rollback e não bloqueia profissionais diferentes. Os casos de uso continuam sem Spring, como exige o ArchUnit, e a implementação fica no adaptador.
  - *Alternativas descartadas:* guardar bloqueios e agendamentos em uma única tabela de ocupações, sob o mesmo `EXCLUDE`, mudaria a modelagem dos dois módulos às vésperas das próximas metas. `SELECT ... FOR UPDATE` na linha do profissional também travaria quem só edita o perfil. Manter a limitação documentada deixaria uma inconsistência possível na regra central do sistema.

**Riscos e decisões**
- FKs de `agendamento` separadas para `profissional` e `servico`, sem a FK composta para `profissional_servico`, que o adaptador de profissionais regrava a cada atualização.
- Transição de status inválida responde 422, e o 409 fica para os conflitos de horário e de versão.

**Próximo passo**
- Casos de uso e endpoints de confirmar, cancelar, reagendar, concluir e registrar não comparecimento (24/11), já usando o objeto devolvido por `salvar` e o 409 de versão desatualizada.

### 24/11 (terça): gestão de agendamentos (1ª meta do dia)

**Resultado:** Parcial (1 de 2 metas do dia; o painel segue pendente)
**Horas:** a preencher, de 2 h planejadas para o dia
**Tarefas:** 1 de 2 planejadas
**Pronto quando:** atendido para a gestão. Toda transição fora do diagrama é recusada com 422, e cada ação grava uma linha em `agendamento_historico`. O painel ainda não existe

**Realizado**
- Casos de uso e rotas de confirmar, cancelar (com motivo opcional), reagendar, concluir e registrar não comparecimento, em `POST /api/v1/agendamentos/{id}/<ação>`, no novo `GestaoAgendamentoController`.
- Cada ação em TDD e em commits próprios (`test:` seguido de `feat:`), na branch `issue-2411`.
- Permissões: gestor ou profissional confirmam, concluem e registram a falta; cliente ou gestor cancelam e reagendam (403 para os demais).
- Reagendamento serializado por profissional, revalidando o novo horário com as mesmas regras da reserva.
- Teste de concorrência entre reagendamento e bloqueio no `ConcorrenciaAgendaTest`.

**Pendente**
- Painel do estabelecimento: agenda filtrada e resumo por status (2ª meta de 24/11).

**Fora do plano**
- Extração da verificação de horário livre de `CriarAgendamentoUseCase` para `DisponibilidadeDaAgenda`, usada pela reserva e pelo reagendamento.
- A consulta de agendamento passou a usar as mesmas partes e regras de permissão das ações de gestão.

**Métricas do dia**
- Testes: 441 no `./mvnw verify`, todos passando, com as regras de cobertura do JaCoCo atendidas.
- Cobertura de linhas de `agendamentos`: 99,0% nos casos de uso e 99,4% no domínio (meta de 80%).

**Desafios e soluções**
- **Cinco ações com o mesmo fluxo.**
  - *Como apareceu:* confirmar, cancelar, reagendar, concluir e registrar a falta repetem os mesmos passos: carregar o agendamento (404), checar quem pede (403), aplicar a ação do domínio (422) e gravar com histórico (409 de versão). Cinco classes copiadas multiplicariam a duplicação.
  - *Solução:* **Template Method** em `AlteracaoDeAgendamentoUseCase`, cujo `executar` é final e fixa o fluxo, com a ação em um passo abstrato e um gancho para rodar serializado. As regras de quem pode agir ficam no enum `PermissaoNoAgendamento` (**Strategy**), reaproveitado pela consulta. `PartesDoAgendamento.carregar` (fábrica estática) carrega o agendamento, o estabelecimento e o profissional uma única vez. Cada caso de uso concreto ficou com poucas linhas.
- **Reagendar para um horário que se sobrepõe ao atual.**
  - *Como apareceu:* ao revalidar o novo horário, o próprio agendamento aparecia entre as ocupações do profissional. Mover um atendimento das 10h para as 10h30 seria recusado com 409 por conflitar consigo mesmo.
  - *Solução:* a verificação ignora o intervalo que o agendamento ocupa hoje. Basta comparar o intervalo, porque o `EXCLUDE` garante que nenhum outro agendamento ativo do profissional o sobrepõe. Assim a porta do repositório não mudou.

**Riscos e decisões**
- As ações de gestão ficaram em um controller próprio, com o mesmo prefixo de rota. Assim o `AgendamentoController` não passa de sete dependências (regra S107 do Sonar), e cada controller tem uma única responsabilidade.
- Confirmar, cancelar, concluir e registrar a falta não usam a trava por profissional, porque não verificam horário; o bloqueio otimista cobre alterações simultâneas do mesmo agendamento.

**Próximo passo**
- Painel do estabelecimento: `GET /estabelecimentos/{id}/agendamentos` com filtros e `GET /estabelecimentos/{id}/painel/resumo`. Feito em 25/11 (registro abaixo).

### 25/11 (quarta): painel do estabelecimento (2ª meta de 24/11, branch `issue-2411`)

**Resultado:** Concluído, com um dia de atraso. **Funcionalidade 6 pronta.** As notificações de 25/11 seguem em paralelo na branch `issue-2511`, com registro próprio
**Horas:** a preencher
**Tarefas:** 1 de 1 (a meta pendente de 24/11)
**Pronto quando:** atendido nos testes. O painel lê direto dos agendamentos gravados, e o caso de uso comprova que um agendamento novo no período aparece no resumo. A verificação manual pelo Swagger ainda não foi feita

**Realizado**
- `GET /api/v1/estabelecimentos/{id}/agendamentos?de&ate&status&profissionalId`: agenda do período em ordem de início, com status repetível e profissional opcionais.
- `GET /api/v1/estabelecimentos/{id}/painel/resumo?de&ate&profissionalId`: total, total por status (os cinco, com zero quando não houver), receita realizada e receita prevista.
- Datas no fuso do estabelecimento, padrão "hoje", limite de 31 dias (422) e acesso só do gestor (403).
- Em TDD e em commits próprios: `refactor:` da checagem do gestor, dois `test:` (domínio e casos de uso; persistência e controller) e um `feat:`.

**Fora do plano**
- `Estabelecimento.garantirGestor` e `EstabelecimentoDoGestor.obter` ganharam a ação como parâmetro, para o 403 dizer "Somente o gestor do estabelecimento pode consultar o painel" em vez de "alterá-lo". `EstabelecimentoDoGestor` ficou público e é reaproveitado pelo módulo de agendamentos, pela camada `application`, como as regras do ArchUnit permitem.

**Métricas do dia**
- Testes: 478 no `./mvnw verify`, todos passando, com as regras de cobertura do JaCoCo atendidas.
- Cobertura de linhas de `agendamentos`: 99,2% nos casos de uso e 99,5% no domínio; as classes novas ficaram com 100%.

**Desafios e soluções**
- **Filtros opcionais na consulta.**
  - *Como apareceu:* status e profissional são opcionais. O caminho óbvio seria uma JPQL com `(:profissionalId is null or a.profissionalId = :profissionalId)`, mas esse padrão é conhecido por falhar no PostgreSQL com parâmetro nulo de tipo UUID, porque o tipo não pode ser inferido. Não chegou a ser implementado: a alternativa foi escolhida antes.
  - *Solução:* critérios pequenos e combináveis com `Specification` (`AgendamentoEspecificacoes`). Um filtro ausente vira `Specification.unrestricted()`, e `Specification.allOf` junta os critérios. O mesmo recurso vai servir à busca de 27/11.
- **Agenda e resumo com as mesmas regras.**
  - *Como apareceu:* as duas rotas carregam o estabelecimento (404), exigem o gestor (403), resolvem as datas no fuso e validam o período (422). Duplicar esse fluxo deixaria as regras divergirem.
  - *Solução:* `PeriodoDoPainel.carregar` (fábrica estática) concentra a parte comum. O resumo é uma **composição**: `ResumirAgendaDoEstabelecimentoUseCase` chama a consulta da agenda e transforma o resultado com `ResumoDaAgenda.de` (domínio puro). `DadosDoPeriodo<T>` carrega o período resolvido junto com a agenda ou com o resumo, para a resposta informar as datas e o fuso usados.

**Riscos e decisões**
- O período considera o **início** do agendamento, como o índice `idx_agendamento_estab_inicio`; um atendimento que começa às 23h30 e termina depois da meia-noite aparece só no primeiro dia.
- O resumo é calculado em memória sobre a agenda do período. O limite de 31 dias mantém o volume pequeno; se crescer, a soma pode ir para uma consulta agregada sem mudar a porta dos casos de uso.
- Profissional de outro estabelecimento no filtro devolve lista vazia, sem erro, porque a consulta sempre filtra também pelo estabelecimento.

**Próximo passo**
- Abrir o PR da `issue-2411` (funcionalidade 6 completa). No merge com a `issue-2511`, ordenar os registros diários 23/11 → 24/11 → 25/11.

### 25/11 (quarta): confirmações e lembretes (branch `issue-2511`). Marco M2

**Resultado:** Concluído. **Funcionalidade 3 pronta.**
**Horas:** a preencher, de 2 h planejadas
**Tarefas:** 4 de 4 planejadas
**Pronto quando:** atendido. No Mailpit do Docker Compose, uma reserva seguida de cancelamento gerou os 4 e-mails esperados (confirmação e cancelamento, para cliente e profissional), com remetente e acentos corretos. No `NotificacoesPorEmailTest`, com PostgreSQL e SMTP reais, o agendamento cancelado não recebe lembrete, o mantido recebe, e uma nova rodada não reenvia nada

**Realizado**
- `NotificacaoGateway` (porta) com a `Mensagem` validada no domínio e o `NotificacaoSmtpAdapter` (`JavaMailSender`), apontado para o Mailpit do Docker Compose.
- Teste do adaptador com GreenMail: confere remetente, destinatário, assunto e corpo, e traduz a falha de conexão em `FalhaNoEnvioException`.
- Outbox `notificacao` (migração nova), com o agregado `Notificacao` (pendente, enviada, falha após 5 tentativas) e o texto de cada tipo em `TipoNotificacao`, no fuso do estabelecimento.
- Porta `AvisosDoAgendamento`, declarada em `agendamentos` e implementada por `AvisosPorEmail` em `notificacoes`. A reserva, o cancelamento e o reagendamento avisam cliente e profissional dentro da mesma transação da gravação.
- `EnviarNotificacoesPendentesUseCase` e o job `@Scheduled` (a cada minuto, desligado nos testes).
- `emailCliente` obrigatório no `POST /agendamentos` (400 se ausente ou inválido).
- Lembretes: `LEMBRETE` em início − 24 h na reserva e no reagendamento (nenhum se faltar menos de 24 h), e `CANCELADA` para os pendentes ao cancelar ou reagendar. Sem migração nova e sem mudança no módulo de agendamentos: o `CHECK` da outbox já previa os dois valores.
- `NotificacoesPorEmailTest`: fluxo de ponta a ponta com os beans reais (casos de uso, outbox no PostgreSQL do Testcontainers, `NotificacaoSmtpAdapter`) e o SMTP do GreenMail. As rodadas do job usam relógio fixo para fazer o lembrete vencer.
- Conferência manual no Mailpit, com um `.env` em portas livres (as padrão estavam ocupadas por outros projetos da máquina).

**Pendente**
- Nenhum item da meta. Abrir o PR da `issue-2511`.

**Fora do plano**
- Validação de e-mail extraída do `Profissional` para `Validacoes.email`, reaproveitada pela `Mensagem` (sem duplicar a expressão regular).
- A `issue-2511` foi refeita sobre a `master` (gestão e painel da `issue-2411`, avaliações da `issue-2611`) antes do PR, com rebase. A 2ª meta liga os e-mails ao cancelamento e ao reagendamento da gestão.
- O job genérico de envio, previsto para a 3ª meta, entrou na 2ª. Por isso a 3ª ficou só com as regras dos lembretes.

**Métricas do dia**
- Testes: 533, todos verdes no `./mvnw verify`, com as regras de cobertura do JaCoCo atendidas.
- Cobertura das classes novas de `notificacoes` e das portas novas de `agendamentos`: 100% das linhas e dos ramos (meta de 80%).

**Desafios e soluções**
- **E-mail que não pode desfazer nem sair antes da reserva.**
  - *Como apareceu:* o e-mail precisa sair só se a reserva for gravada, mas um servidor de e-mail fora do ar não pode recusar a reserva. Enviar dentro da transação prende a reserva ao SMTP; enviar depois do commit em memória (`@TransactionalEventListener` + `@Async`, como previa o corte de 20/11) perde o e-mail se a aplicação cair entre o commit e o envio, e não tem onde guardar o e-mail do cliente para os avisos seguintes.
  - *Solução:* **outbox** gravada na mesma transação, dentro da `ExecucaoSerializada` que já protegia a reserva. Um job envia as pendentes com novas tentativas. O teste de concorrência comprova o rollback junto: de cinco reservas simultâneas, só a aceita deixa notificações. O cancelamento passou a rodar na `ExecucaoSerializada` para ter a mesma transação.
- **Três avisos com o mesmo fluxo.**
  - *Solução:* um gancho `depoisDeGravar` no **Template Method** `AlteracaoDeAgendamentoUseCase`, sobrescrito só pelo cancelamento e pelo reagendamento. No módulo de notificações, `AvisosPorEmail` concentra o fluxo comum, e cada `TipoNotificacao` é a **Strategy** do próprio texto.

**Riscos e decisões**
- O e-mail do cliente é um campo obrigatório `emailCliente` no `POST /agendamentos`, guardado na outbox `notificacao` (`destinatario_email`, coluna `papel = CLIENTE`). Não há tabela de usuários depois do corte de escopo de 20/11, e o agregado `Agendamento` não mudou. Agendamento sem notificação do cliente (anterior à migração) avisa só o profissional.
- O corte de 20/11 trocava a outbox por envio em memória depois do commit; a decisão foi revista (planejamento, seção 2.1). Sem `SKIP LOCKED`, porque roda uma instância só.
- A dependência entre módulos segue o padrão de `AgendamentosAtivos`: quem consome declara a porta (`agendamentos`), e quem sabe fazer implementa (`notificacoes`).
- O GreenMail traz o JUnit 4; ele foi excluído no `pom.xml`, porque os testes usam só a extensão do JUnit 5.
- O Actuator passa a checar o SMTP no `/actuator/health`. Sem servidor de e-mail, a saúde fica `DOWN`. Avaliar antes do deploy de 28/11.
- Reserva feita com menos de 24 h de antecedência não ganha lembrete: a confirmação acabou de sair.
- Corrida aceita entre o cancelamento e o envio do mesmo lembrete, no instante exato em que ele vence: o e-mail já saiu e o job grava `ENVIADA` por cima de `CANCELADA`. Sem coluna de versão; a correção seria um update condicional no adaptador.

- O Docker Compose de todas as worktrees usa o mesmo nome de projeto (`agendabeleza`) e, portanto, o mesmo volume do PostgreSQL. Subir o compose numa branch aplica as migrações dela nesse banco local compartilhado.

**Próximo passo**
- Abrir o PR da `issue-2511` (funcionalidade 3 completa) e, na sexta 27/11, a busca e os filtros.

### 26/11 (quinta): avaliações (feita em 25/11, adiantada; branch `issue-2611`)

**Resultado:** Concluído um dia antes do planejado. **Funcionalidade 4 pronta**
**Horas:** a preencher, de 2 h planejadas
**Tarefas:** 3 de 3
**Pronto quando:** atendido, comprovado pela feature BDD. Depois de uma avaliação, a nota média do salão e a da profissional mudam, e a segunda avaliação do mesmo atendimento é recusada com 422

**Realizado**
- `POST /api/v1/agendamentos/{id}/avaliacao`: só o cliente do agendamento, só para atendimento `CONCLUIDO`, uma vez; notas de 1 a 5 para o estabelecimento e para o profissional, e comentário opcional.
- `GET /api/v1/estabelecimentos/{id}/avaliacoes` e `GET /api/v1/profissionais/{id}/avaliacoes`: listas públicas, da mais recente para a mais antiga, sem o `clienteId`.
- `GET /estabelecimentos/{id}` passou a devolver `notaMedia` e `totalAvaliacoes`, como o profissional já fazia.
- Migração `V1_1795657196611__criar_tabela_avaliacao.sql`.
- Feature `avaliacoes/avaliacao_atendimento.feature`, com o fluxo inteiro pela API: cadastro, reserva, confirmação, conclusão e avaliação.
- Em TDD: dois `test:` (domínio e casos de uso; persistência, controller e BDD) e um `feat:`.

**Fora do plano**
- Correção de um *lost update* que já existia: o `salvar` do profissional regravava `nota_media` e `total_avaliacoes` com o valor lido antes. Com as avaliações, uma edição de perfil simultânea apagaria a nota recém-calculada.
- `ApiDaAplicacao` extraída do `EstabelecimentoSteps`, para os passos BDD não repetirem a montagem das requisições.

**Métricas do dia**
- Testes: 540 no `./mvnw verify`, todos passando (eram 478), com as regras de cobertura do JaCoCo atendidas. 9 cenários BDD, todos verdes.
- Cobertura de linhas: 100% em `avaliacoes.domain`, `avaliacoes.application` e `compartilhado.domain`.

**Desafios e soluções**
- **Média exata e sem perder avaliações simultâneas.**
  - *Como apareceu:* `nota_media` é `numeric(3,2)`. Uma média incremental (`(media * total + nota) / (total + 1)`) acumularia o arredondamento a cada avaliação. Duas avaliações do mesmo estabelecimento ao mesmo tempo poderiam ler o mesmo total e gravar uma por cima da outra.
  - *Solução:* a média é sempre recalculada da soma e da contagem das notas gravadas (`NotaMedia.de`, domínio puro), dentro de `ExecucaoSerializada` com a chave do estabelecimento, a mesma porta que já serializa reserva e bloqueio por profissional. A trava e a transação vêm juntas, e como o profissional pertence a um único estabelecimento, uma trava só cobre as duas médias, sem risco de deadlock entre duas travas.
- **Notas sobrescritas por outras gravações.**
  - *Como apareceu:* os adapters de estabelecimento e profissional montam a entidade inteira a cada `salvar`. Com as notas mapeadas, uma edição feita com dados lidos antes da avaliação gravaria a nota antiga por cima.
  - *Solução:* as colunas ficaram `insertable = false, updatable = false` nas entidades, e só mudam pelo `atualizarAvaliacoes` das portas `EstabelecimentoRepository` e `ProfissionalRepository`, com `update` nativo. O `AvaliacaoPersistenciaTest` comprova que uma edição com dados antigos preserva as notas.
- **Concluir um atendimento no BDD.**
  - *Como apareceu:* a reserva exige início no futuro, e o "concluir" só vale a partir do início. Com o relógio real, o cenário não conseguiria chegar a um atendimento concluído pela API.
  - *Solução:* `RelogioAjustavel`, um `Clock` `@Primary` só no contexto do Cucumber. O passo reserva para 2030, fixa o "agora" para depois do atendimento e conclui. Cada cenário volta ao relógio real no fim.

**Riscos e decisões**
- A segunda avaliação é recusada com 422 (`RegraNegocioException`), e não com 409, porque o 409 fica reservado para conflito de horário e de versão. A restrição única só seria acionada fora da trava, e nesse caso cairia no 409 genérico de integridade.
- As listas não são paginadas, como `/agendamentos/meus`. A paginação entra com a busca de 27/11, se for preciso.
- O `Location` do 201 aponta para a lista de avaliações do estabelecimento, porque não há consulta individual de avaliação.

**Próximo passo**
- PR da `issue-2611` e busca com filtros (funcionalidade 5).

### 27/11 (sexta): busca e filtros (feita em 26/11, adiantada, em duas metas; branch `issue-2711`)

**Resultado:** Concluído um dia antes do planejado. **Funcionalidade 5 pronta**
**Horas:** a preencher, de 2 h planejadas para o dia
**Tarefas:** 2 de 2
**Pronto quando:** atendido. Os filtros funcionam combinados, inclusive a disponibilidade, e a resposta é paginada, comprovado pelo `BuscaPersistenciaTest`

**Realizado**
- `GET /api/v1/busca/estabelecimentos`, público: `nome`, `cidade`, `bairro`, `servico`, `categoria`, `notaMinima`, `precoMinimo`, `precoMaximo`, `pagina`, `tamanho` (padrão 20, máximo 50) e `ordem` (`MELHOR_AVALIADOS`, padrão, ou `NOME`). Resposta `{ itens, pagina, tamanho, totalDeItens, totalDePaginas }`.
- Migração `V1_1795662281145__criar_indices_busca.sql`: `idx_estabelecimento_cidade_bairro`, `idx_estabelecimento_nota` e `idx_servico_categoria`, todos parciais (`WHERE ativo`).
- `Paginacao` e `Pagina` em `compartilhado/domain`, reutilizáveis por outras listas; `FiltroDaBusca`, `OrdemDaBusca` e `EstabelecimentoEncontrado` no domínio da busca; `BuscarEstabelecimentosUseCase` e a porta `CatalogoDeEstabelecimentos`.
- Em TDD: `test:` → `feat:` do núcleo, depois `test:` → `feat:` da persistência e do controller.
- Meta 2, disponibilidade: `data`, `horaInicio` e `horaFim`. `JanelaDeDisponibilidade` no domínio (422 para data no passado, hora sem data e janela vazia ou invertida), `Clock` no caso de uso, entidades somente leitura de jornada e funcionamento e um segundo `EXISTS` na `Specification`. Sem migração. Também em `test:` → `feat:`, duas vezes.

**Fora do plano**
- Nenhum.

**Métricas do dia**
- Testes: 690 no `./mvnw verify`, todos passando (eram 595; 661 ao fim da meta 1), com as regras de cobertura do JaCoCo atendidas. 9 cenários BDD, todos verdes.
- Cobertura de linhas: 100% em `busca.domain`, `busca.application` e `compartilhado.domain`; 91% no adaptador de persistência da busca.

**Desafios e soluções**
- **Ler cadastros de outros módulos sem quebrar a arquitetura.**
  - *Como apareceu:* a busca filtra estabelecimento, serviços, tarifas e profissionais, mas o ArchUnit proíbe um módulo de usar os adaptadores (e as entidades JPA) de outro.
  - *Solução:* o módulo `busca` virou um modelo de leitura, com entidades JPA próprias e `@Immutable` sobre as mesmas tabelas, só com as colunas usadas. Os módulos donos continuam sendo os únicos que gravam.
- **Serviço e preço do mesmo serviço, sem repetir o estabelecimento.**
  - *Como apareceu:* com `JOIN`, "corte até R$ 50" aceitaria um salão com corte a R$ 120 e manicure a R$ 40, e um salão com vários serviços na faixa apareceria várias vezes, distorcendo o total da página.
  - *Solução:* um único `EXISTS` correlacionado sobre os serviços ativos do estabelecimento, com outro `EXISTS` para a tarifa de profissional ativo dentro da faixa. O preço é a tarifa do profissional, que é o valor cobrado na reserva.
- **Páginas estáveis e sem avaliação no fim.**
  - *Como apareceu:* no PostgreSQL, `ORDER BY nota_media DESC` põe os nulos primeiro, e empates sem desempate podem trocar de página entre consultas.
  - *Solução:* `Sort.Order.desc("notaMedia").nullsLast()`, desempate por nome e por identificador, e ordem fechada em enum, em vez de um `sort` livre com nome de propriedade.
- **Disponível em quê, sem calcular a agenda de todos os profissionais.**
  - *Como apareceu:* os horários livres reais vêm de `AgendaProfissional`, por profissional e por serviço. Rodar esse cálculo para cada candidato da busca seria lento e impediria paginar no banco.
  - *Solução:* a busca responde "pode atender nesse horário", e os horários livres respondem "a que horas". Um `EXISTS` exige um profissional ativo com jornada, funcionamento e janela com um instante em comum; para três intervalos semiabertos, isso é todo início anterior a todo fim, seis comparações simples. Com filtro de serviço, o mesmo profissional precisa executar o serviço, para não mostrar um salão em que só a manicure trabalha no dia.
- **"Hoje" sem fuso do estabelecimento.**
  - *Como apareceu:* a validação de data no passado depende do fuso, mas a busca abrange estabelecimentos de vários lugares.
  - *Solução:* "hoje" é o dia no fuso padrão (`America/Sao_Paulo`), com o `Clock` injetado; o teste fixa um instante que ainda é o dia anterior em São Paulo. A data e as horas da janela não são convertidas, porque estão no fuso de cada estabelecimento, como as colunas `time`.
- **Texto digitado na busca.**
  - *Como apareceu:* `%` e `_` são curingas do `LIKE`, então buscar "100%" encontraria "1000".
  - *Solução:* os curingas são escapados, e os valores vão como parâmetros da consulta, já em minúsculas, nunca como literal no SQL.

**Riscos e decisões**
- Os parâmetros usam nomes completos (`precoMinimo`, `pagina`, `tamanho`, `ordem`), e não `precoMin`/`page`/`size`/`sort`, como previa a arquitetura, pela diretriz de nomes sem abreviação.
- Limites de cada parâmetro respondem 400 (validação de método do Spring MVC); a faixa de preço invertida é regra entre campos, do domínio, e responde 422.
- Sem `pg_trgm`, o trecho do nome não usa índice. Aceitável no volume do projeto; o teste de carga da busca (28/11) vai medir.
- A disponibilidade, primeiro item da ordem de corte, considera só a jornada do profissional no dia da semana e o funcionamento, sem descontar agendamentos e bloqueios: um salão com a agenda cheia ainda aparece. Decisão do autor, para caber nas 2 h do dia.
- Data no passado, hora sem data e janela invertida respondem 422, como as outras regras entre parâmetros; data ou hora malformada responde 400.

**Próximo passo**
- PR da `issue-2711`. Depois, 28/11: feed `.ics`, carga e deploy.
