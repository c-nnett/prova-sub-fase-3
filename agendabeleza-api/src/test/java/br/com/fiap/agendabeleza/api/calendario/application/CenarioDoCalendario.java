package br.com.fiap.agendabeleza.api.calendario.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.application.AgendamentoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.calendario.application.usecase.CalendarioDoCliente;
import br.com.fiap.agendabeleza.api.calendario.application.usecase.CalendarioDoProfissional;
import br.com.fiap.agendabeleza.api.calendario.application.usecase.EmitirAssinaturaDoCalendarioUseCase;
import br.com.fiap.agendabeleza.api.calendario.application.usecase.GerarFeedDoCalendarioUseCase;
import br.com.fiap.agendabeleza.api.calendario.application.usecase.TitularesDoCalendario;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.EstabelecimentoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.ServicoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.profissionais.application.ProfissionalRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;
import br.com.fiap.agendabeleza.api.profissionais.domain.ProfissionalFixture;

/**
 * Monta os dois casos de uso do calendário sobre repositórios em memória, com um estabelecimento, um serviço e um
 * profissional já cadastrados.
 */
public class CenarioDoCalendario {

	public static final Instant AGORA = Instant.parse("2030-03-04T12:00:00Z");
	public static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

	public final AgendamentoRepositoryEmMemoria agendamentos = new AgendamentoRepositoryEmMemoria();
	public final EstabelecimentoRepositoryEmMemoria estabelecimentos = new EstabelecimentoRepositoryEmMemoria();
	public final ServicoRepositoryEmMemoria servicos = new ServicoRepositoryEmMemoria();
	public final ProfissionalRepositoryEmMemoria profissionais = new ProfissionalRepositoryEmMemoria();
	public final AssinaturaCalendarioRepositoryEmMemoria assinaturas = new AssinaturaCalendarioRepositoryEmMemoria();
	public final CalendarioGatewayEmMemoria calendario = new CalendarioGatewayEmMemoria();

	public final Estabelecimento estabelecimento;
	public final Servico servico;
	public final Profissional profissional;

	public final EmitirAssinaturaDoCalendarioUseCase emitir;
	public final GerarFeedDoCalendarioUseCase gerarFeed;

	public CenarioDoCalendario() {
		estabelecimento = estabelecimentos.salvar(EstabelecimentoFixture.estabelecimento());
		servico = servicos.salvar(Servico.cadastrar(estabelecimento.id(), "Corte feminino", null,
				CategoriaServico.CABELO, 45, new BigDecimal("80.00")));
		profissional = profissionais.salvar(ProfissionalFixture.profissional(estabelecimento.id()));
		Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);
		TitularesDoCalendario titulares = new TitularesDoCalendario(List.of(
				new CalendarioDoProfissional(profissionais, estabelecimentos, agendamentos),
				new CalendarioDoCliente(agendamentos)));
		emitir = new EmitirAssinaturaDoCalendarioUseCase(titulares, assinaturas, relogio);
		gerarFeed = new GerarFeedDoCalendarioUseCase(titulares, assinaturas, estabelecimentos, servicos,
				profissionais, calendario, relogio);
	}

	/** Agendamento do cliente padrão com o profissional do cenário, começando {@code deslocamento} após agora. */
	public Agendamento agendado(Duration deslocamento, StatusAgendamento status) {
		return agendado(CLIENTE_ID, deslocamento, status);
	}

	public Agendamento agendado(UUID clienteId, Duration deslocamento, StatusAgendamento status) {
		Instant inicio = AGORA.plus(deslocamento);
		return agendamentos.salvar(Agendamento.reconstituir(UUID.randomUUID(), clienteId, estabelecimento.id(),
				profissional.id(), servico.id(), inicio, inicio.plus(Duration.ofMinutes(45)), status,
				new BigDecimal("80.00"), null, null));
	}

}
