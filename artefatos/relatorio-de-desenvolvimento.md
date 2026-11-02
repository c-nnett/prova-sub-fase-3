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

Nenhum dia registrado até o momento.
