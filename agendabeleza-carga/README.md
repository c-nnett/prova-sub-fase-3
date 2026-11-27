# agendabeleza-carga

Testes de carga da AgendaBeleza API com **Gatling 3.16** (Java DSL). É um projeto Maven separado da API, sem o
parent do Spring Boot, para que as versões de Netty e Jackson do Boot 4 não se misturem às do Gatling. Não roda no
`./mvnw verify` nem no CI: precisa da API e do banco no ar.

## Como rodar

1. Suba a aplicação na raiz do repositório: `docker compose up -d --build`. A porta da API vem do `.env`
   (`API_PORT`, padrão 8080).
2. Rode uma simulação na raiz do repositório, com o JDK 21:

   ```bash
   agendabeleza-api/mvnw -f agendabeleza-carga/pom.xml gatling:test \
     -Dgatling.simulationClass=br.com.fiap.agendabeleza.carga.DisputaPeloMesmoHorarioSimulation \
     -Dcarga.urlBase=http://localhost:8080
   ```

3. O relatório HTML fica em `agendabeleza-carga/target/gatling/<simulacao>-<data>/index.html`. Os relatórios de
   referência estão em `artefatos/carga/`.

Cada simulação cria, pela própria API, um salão novo antes da carga (`PreparacaoDaCarga`), então pode ser repetida
sem limpar o banco.

## Simulações e metas (arquitetura, seção 9.3)

| Simulação | Carga padrão | Meta |
|---|---|---|
| `DisputaPeloMesmoHorarioSimulation` | 100 clientes pedem o mesmo horário ao mesmo tempo | exatamente 1 × 201 e 99 × 409; o painel confirma 1 agendamento gravado |
| `ReservasEmHorariosDistintosSimulation` | 30 reservas por segundo durante 60 s (1.800) | p95 < 800 ms, falhas < 1% |
| `BuscaComFiltrosSimulation` | 100 usuários simultâneos por 120 s, com rampa de 10 s | p95 < 500 ms, falhas < 1% |
| `HorariosLivresSimulation` | 100 usuários simultâneos por 120 s, com rampa de 10 s | p95 < 500 ms, falhas < 1% |

Se uma meta falhar, o Maven termina com `BUILD FAILURE`.

## Parâmetros (`-Dcarga.<nome>=<valor>`)

| Nome | Padrão | Uso |
|---|---|---|
| `urlBase` | `http://localhost:8082` | Endereço da API, sem `/api/v1` |
| `usuarios`, `duracao`, `rampa` | 100, 120, 10 | Consultas: usuários simultâneos e segundos |
| `reservasPorSegundo`, `duracaoDasReservas` | 30, 60 | Reservas em horários distintos |
| `clientesNaDisputa` | 100 | Disputa pelo mesmo horário |
| `profissionais` | 10 | Profissionais do salão preparado |

Para uma rodada curta de conferência, use por exemplo
`-Dcarga.usuarios=5 -Dcarga.duracao=5 -Dcarga.rampa=1 -Dcarga.reservasPorSegundo=5 -Dcarga.duracaoDasReservas=5`.
