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
