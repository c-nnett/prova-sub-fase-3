package br.com.fiap.agendabeleza.api.notificacoes;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.endereco;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;

import br.com.fiap.agendabeleza.api.TestcontainersConfiguration;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.CancelarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.CriarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.HorarioFuncionamento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.notificacoes.application.port.NotificacaoGateway;
import br.com.fiap.agendabeleza.api.notificacoes.application.port.NotificacaoRepository;
import br.com.fiap.agendabeleza.api.notificacoes.application.usecase.EnviarNotificacoesPendentesUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.PeriodoTrabalho;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;
import br.com.fiap.agendabeleza.api.profissionais.domain.ServicoProfissional;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Funcionalidade 3 de ponta a ponta, com as peças reais: caso de uso, transação, outbox no PostgreSQL do
 * Testcontainers, {@code NotificacaoSmtpAdapter} e um servidor SMTP de verdade (GreenMail).
 * <p>
 * O job automático fica desligado nos testes. Cada rodada é chamada aqui com um relógio fixo, que só decide o que
 * já venceu; repositório e servidor de e-mail são os beans da aplicação. "Pronto quando" de 25/11: um agendamento
 * cancelado não recebe lembrete.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class NotificacoesPorEmailTest {

	/** Segunda-feira, 4 de março de 2030, à meia-noite em Brasília. */
	private static final Instant SEGUNDA = Instant.parse("2030-03-04T03:00:00Z");
	private static final Instant DEZ_HORAS = SEGUNDA.plus(Duration.ofHours(10));
	private static final Instant CATORZE_HORAS = SEGUNDA.plus(Duration.ofHours(14));
	/** Domingo, 17h01 UTC: os lembretes das duas reservas (10h e 14h de Brasília, menos 24h) já venceram. */
	private static final Instant DEPOIS_DOS_LEMBRETES = Instant.parse("2030-03-03T17:01:00Z");

	@RegisterExtension
	static final GreenMailExtension servidorSmtp = new GreenMailExtension(ServerSetupTest.SMTP);

	@DynamicPropertySource
	static void apontarParaOGreenMail(DynamicPropertyRegistry propriedades) {
		propriedades.add("spring.mail.host", () -> "localhost");
		propriedades.add("spring.mail.port", ServerSetupTest.SMTP::getPort);
	}

	@Autowired
	private CriarAgendamentoUseCase criarAgendamento;

	@Autowired
	private CancelarAgendamentoUseCase cancelarAgendamento;

	@Autowired
	private NotificacaoRepository notificacoes;

	@Autowired
	private NotificacaoGateway servidorDeEmail;

	@Autowired
	private EstabelecimentoRepository estabelecimentos;

	@Autowired
	private ServicoRepository servicos;

	@Autowired
	private ProfissionalRepository profissionais;

	@Value("${agendabeleza.notificacoes.remetente}")
	private String remetente;

	private final String sufixo = UUID.randomUUID().toString().substring(0, 8);
	private final String emailClienteQueMantem = "mantem." + sufixo + "@email.com";
	private final String emailClienteQueCancela = "cancela." + sufixo + "@email.com";
	private final String emailProfissional = "ana." + sufixo + "@salao.com";

	private UUID gestorId;
	private Servico corte;
	private Profissional ana;

	@BeforeEach
	void prepararAgenda() {
		gestorId = UUID.randomUUID();
		Estabelecimento estabelecimento = Estabelecimento.cadastrar(gestorId, "Salão Bela Vista", null, endereco(),
				null);
		estabelecimento.definirHorarios(List.of(
				new HorarioFuncionamento(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(20, 0))));
		estabelecimentos.salvar(estabelecimento);
		corte = servicos.salvar(Servico.cadastrar(estabelecimento.id(), "Corte", null, CategoriaServico.CABELO, 60,
				new BigDecimal("80.00")));
		Profissional profissional = Profissional.cadastrar(estabelecimento.id(), null, "Ana", emailProfissional,
				null);
		profissional.definirServicos(List.of(new ServicoProfissional(corte.id(), new BigDecimal("95.00"))));
		profissional.definirJornada(List.of(
				new PeriodoTrabalho(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0))));
		ana = profissionais.salvar(profissional);
	}

	@Test
	void confirmaCancelaELembraSoQuemManteveOAgendamento() throws Exception {
		reservar(emailClienteQueMantem, DEZ_HORAS);
		Agendamento cancelado = reservar(emailClienteQueCancela, CATORZE_HORAS);
		cancelarAgendamento.executar(new CancelarAgendamentoUseCase.Comando(cancelado.id(), gestorId, null));

		rodadaDeEnvioEm(Clock.systemUTC().instant());

		assertThat(assuntosRecebidosPor(emailClienteQueMantem)).containsExactly("Agendamento reservado");
		assertThat(assuntosRecebidosPor(emailClienteQueCancela))
				.containsExactlyInAnyOrder("Agendamento reservado", "Agendamento cancelado");
		assertThat(assuntosRecebidosPor(emailProfissional))
				.containsExactlyInAnyOrder("Agendamento reservado", "Agendamento reservado", "Agendamento cancelado");
		MimeMessage confirmacao = recebidasPor(emailClienteQueMantem).getFirst();
		assertThat(confirmacao.getFrom()[0]).hasToString(remetente);
		assertThat(confirmacao.getContent().toString().strip())
				.isEqualTo("O agendamento no Salão Bela Vista com Ana está reservado para 04/03/2030 às 10:00.");

		rodadaDeEnvioEm(DEPOIS_DOS_LEMBRETES);

		assertThat(assuntosRecebidosPor(emailClienteQueMantem))
				.containsExactly("Agendamento reservado", "Lembrete de agendamento");
		assertThat(assuntosRecebidosPor(emailClienteQueCancela))
				.as("o agendamento cancelado não recebe lembrete")
				.doesNotContain("Lembrete de agendamento");
		assertThat(assuntosRecebidosPor(emailProfissional))
				.filteredOn("Lembrete de agendamento"::equals)
				.hasSize(1);

		int totalAntes = servidorSmtp.getReceivedMessages().length;
		rodadaDeEnvioEm(DEPOIS_DOS_LEMBRETES);

		assertThat(servidorSmtp.getReceivedMessages()).as("nada é enviado duas vezes").hasSize(totalAntes);
	}

	private Agendamento reservar(String emailCliente, Instant inicio) {
		return criarAgendamento.executar(new CriarAgendamentoUseCase.Comando(UUID.randomUUID(), emailCliente,
				ana.id(), corte.id(), inicio, null));
	}

	/** Uma rodada do job, com o relógio no instante informado e os beans reais de outbox e e-mail. */
	private void rodadaDeEnvioEm(Instant momento) {
		new EnviarNotificacoesPendentesUseCase(notificacoes, servidorDeEmail, Clock.fixed(momento, ZoneOffset.UTC))
				.executar();
	}

	private List<MimeMessage> recebidasPor(String email) throws MessagingException {
		List<MimeMessage> recebidas = new ArrayList<>();
		for (MimeMessage mensagem : servidorSmtp.getReceivedMessages()) {
			if (destinadaA(mensagem, email)) {
				recebidas.add(mensagem);
			}
		}
		return recebidas;
	}

	private List<String> assuntosRecebidosPor(String email) throws MessagingException {
		List<String> assuntos = new ArrayList<>();
		for (MimeMessage mensagem : recebidasPor(email)) {
			assuntos.add(mensagem.getSubject());
		}
		return assuntos;
	}

	private static boolean destinadaA(MimeMessage mensagem, String email) throws MessagingException {
		Address[] destinatarios = mensagem.getAllRecipients();
		return destinatarios != null
				&& Arrays.stream(destinatarios).anyMatch(destinatario -> destinatario.toString().equals(email));
	}

}
