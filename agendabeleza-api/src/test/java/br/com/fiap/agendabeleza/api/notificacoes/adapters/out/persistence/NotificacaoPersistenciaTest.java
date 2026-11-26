package br.com.fiap.agendabeleza.api.notificacoes.adapters.out.persistence;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.endereco;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import br.com.fiap.agendabeleza.api.TestcontainersConfiguration;
import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.notificacoes.application.port.NotificacaoRepository;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Mensagem;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Notificacao;
import br.com.fiap.agendabeleza.api.notificacoes.domain.PapelDestinatario;
import br.com.fiap.agendabeleza.api.notificacoes.domain.StatusNotificacao;
import br.com.fiap.agendabeleza.api.notificacoes.domain.TipoNotificacao;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;
import br.com.fiap.agendabeleza.api.profissionais.domain.ServicoProfissional;

/**
 * Outbox real no PostgreSQL do Testcontainers. Cada teste cria o próprio agendamento, para a FK, e as consultas
 * filtram por ele quando outras notificações do banco poderiam interferir.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class NotificacaoPersistenciaTest {

	private static final Instant AGORA = Instant.parse("2030-03-04T12:00:00Z");
	private static final String EMAIL_CLIENTE = "cliente@email.com";

	@Autowired
	private NotificacaoRepository notificacoes;

	@Autowired
	private AgendamentoRepository agendamentos;

	@Autowired
	private EstabelecimentoRepository estabelecimentos;

	@Autowired
	private ServicoRepository servicos;

	@Autowired
	private ProfissionalRepository profissionais;

	private UUID agendamentoId;

	@BeforeEach
	void prepararAgendamento() {
		Estabelecimento estabelecimento = estabelecimentos.salvar(Estabelecimento.cadastrar(
				UUID.randomUUID(), "Salão Bela Vista", null, endereco(), null));
		Servico corte = servicos.salvar(Servico.cadastrar(estabelecimento.id(), "Corte", null,
				CategoriaServico.CABELO, 60, new BigDecimal("80.00")));
		Profissional ana = Profissional.cadastrar(estabelecimento.id(), null, "Ana", "ana@salao.com", null);
		ana.definirServicos(List.of(new ServicoProfissional(corte.id(), new BigDecimal("95.00"))));
		ana = profissionais.salvar(ana);
		Agendamento agendamento = agendamentos.salvar(Agendamento.reservar(UUID.randomUUID(),
				estabelecimento.id(), ana.id(), corte.id(), Instant.parse("2030-03-05T13:00:00Z"), 60,
				new BigDecimal("95.00"), null, AGORA));
		agendamentoId = agendamento.id();
	}

	private Notificacao pendente(PapelDestinatario papel, String destinatario, Instant enviarEm) {
		return Notificacao.pendente(agendamentoId, TipoNotificacao.CONFIRMACAO, papel,
				new Mensagem(destinatario, "Agendamento reservado", "O agendamento está reservado."), enviarEm);
	}

	private List<Notificacao> pendentesDoAgendamentoAte(Instant momento) {
		return notificacoes.pendentesAte(momento, 1_000).stream()
				.filter(notificacao -> notificacao.agendamentoId().equals(agendamentoId))
				.toList();
	}

	@Test
	void devolveSoAsPendentesVencidasEmOrdemDeEnvio() {
		Notificacao maisRecente = pendente(PapelDestinatario.PROFISSIONAL, "ana@salao.com", AGORA);
		Notificacao maisAntiga = pendente(PapelDestinatario.CLIENTE, EMAIL_CLIENTE, AGORA.minus(Duration.ofMinutes(5)));
		Notificacao futura = pendente(PapelDestinatario.CLIENTE, EMAIL_CLIENTE, AGORA.plus(Duration.ofHours(1)));
		Notificacao enviada = pendente(PapelDestinatario.CLIENTE, EMAIL_CLIENTE, AGORA.minus(Duration.ofHours(1)));
		enviada.marcarEnviada(AGORA);

		notificacoes.salvarTodas(List.of(maisRecente, maisAntiga, futura, enviada));

		assertThat(pendentesDoAgendamentoAte(AGORA))
				.extracting(Notificacao::id)
				.containsExactly(maisAntiga.id(), maisRecente.id());
	}

	@Test
	void gravaEReleTodosOsCampos() {
		Notificacao original = pendente(PapelDestinatario.CLIENTE, EMAIL_CLIENTE, AGORA);
		notificacoes.salvarTodas(List.of(original));

		Notificacao lida = pendentesDoAgendamentoAte(AGORA).getFirst();

		assertThat(lida.id()).isEqualTo(original.id());
		assertThat(lida.tipo()).isEqualTo(TipoNotificacao.CONFIRMACAO);
		assertThat(lida.papel()).isEqualTo(PapelDestinatario.CLIENTE);
		assertThat(lida.mensagem()).isEqualTo(original.mensagem());
		assertThat(lida.enviarEm()).isEqualTo(AGORA);
		assertThat(lida.status()).isEqualTo(StatusNotificacao.PENDENTE);
		assertThat(lida.tentativas()).isZero();
	}

	@Test
	void atualizaFalhasEEnvio() {
		Notificacao notificacao = pendente(PapelDestinatario.CLIENTE, EMAIL_CLIENTE, AGORA);
		notificacoes.salvarTodas(List.of(notificacao));

		notificacao.registrarFalha("Conexão recusada");
		notificacoes.atualizar(notificacao);
		Notificacao comFalha = pendentesDoAgendamentoAte(AGORA).getFirst();
		assertThat(comFalha.tentativas()).isEqualTo(1);
		assertThat(comFalha.ultimoErro()).isEqualTo("Conexão recusada");

		comFalha.marcarEnviada(AGORA);
		notificacoes.atualizar(comFalha);
		assertThat(pendentesDoAgendamentoAte(AGORA)).isEmpty();
	}

	@Test
	void encontraOsPendentesDoAgendamentoPorTipo() {
		Notificacao lembrete = lembrete(PapelDestinatario.CLIENTE);
		Notificacao lembreteEnviado = lembrete(PapelDestinatario.PROFISSIONAL);
		lembreteEnviado.marcarEnviada(AGORA);
		Notificacao confirmacao = pendente(PapelDestinatario.CLIENTE, EMAIL_CLIENTE, AGORA);
		notificacoes.salvarTodas(List.of(lembrete, lembreteEnviado, confirmacao));

		assertThat(notificacoes.pendentesDoAgendamento(agendamentoId, TipoNotificacao.LEMBRETE))
				.extracting(Notificacao::id)
				.containsExactly(lembrete.id());
		assertThat(notificacoes.pendentesDoAgendamento(UUID.randomUUID(), TipoNotificacao.LEMBRETE)).isEmpty();
	}

	@Test
	void canceladaFicaGravadaEForaDaRodadaDeEnvio() {
		Notificacao lembrete = lembrete(PapelDestinatario.CLIENTE);
		notificacoes.salvarTodas(List.of(lembrete));

		lembrete.cancelar();
		notificacoes.atualizar(lembrete);

		assertThat(notificacoes.pendentesDoAgendamento(agendamentoId, TipoNotificacao.LEMBRETE)).isEmpty();
		assertThat(pendentesDoAgendamentoAte(AGORA.plus(Duration.ofDays(2)))).isEmpty();
	}

	/** Lembrete para 24 horas antes do início do agendamento preparado (05/03, 13h UTC). */
	private Notificacao lembrete(PapelDestinatario papel) {
		return Notificacao.pendente(agendamentoId, TipoNotificacao.LEMBRETE, papel,
				new Mensagem(EMAIL_CLIENTE, "Lembrete de agendamento", "Lembrete."),
				Instant.parse("2030-03-04T13:00:00Z"));
	}

	@Test
	void encontraOEmailDoClientePeloAgendamento() {
		notificacoes.salvarTodas(List.of(
				pendente(PapelDestinatario.PROFISSIONAL, "ana@salao.com", AGORA),
				pendente(PapelDestinatario.CLIENTE, EMAIL_CLIENTE, AGORA)));

		assertThat(notificacoes.emailDoCliente(agendamentoId)).contains(EMAIL_CLIENTE);
		assertThat(notificacoes.emailDoCliente(UUID.randomUUID())).isEmpty();
	}

}
