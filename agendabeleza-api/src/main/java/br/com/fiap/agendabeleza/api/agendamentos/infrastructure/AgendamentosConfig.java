package br.com.fiap.agendabeleza.api.agendamentos.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.AgendamentosAtivosDoProfissional;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.CancelarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConcluirAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConfirmarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConsultarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConsultarHorariosLivresUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.CriarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ListarMeusAgendamentosUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.RegistrarNaoComparecimentoUseCase;
import br.com.fiap.agendabeleza.api.compartilhado.application.port.ExecucaoSerializada;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.AgendamentosAtivos;
import br.com.fiap.agendabeleza.api.profissionais.application.port.BloqueioAgendaRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;

/**
 * Registra os casos de uso do módulo como beans. As portas dos módulos {@code estabelecimentos} e
 * {@code profissionais} são recebidas pela interface (arquitetura, seção 4.2).
 */
@Configuration(proxyBeanMethods = false)
class AgendamentosConfig {

	@Bean
	ConsultarHorariosLivresUseCase consultarHorariosLivresUseCase(ProfissionalRepository profissionais,
			EstabelecimentoRepository estabelecimentos, ServicoRepository servicos,
			BloqueioAgendaRepository bloqueios, AgendamentoRepository agendamentos, Clock relogio) {
		return new ConsultarHorariosLivresUseCase(profissionais, estabelecimentos, servicos, bloqueios, agendamentos,
				relogio);
	}

	@Bean
	@SuppressWarnings("java:S107") // Mesmas dependências do construtor do caso de uso.
	CriarAgendamentoUseCase criarAgendamentoUseCase(AgendamentoRepository agendamentos,
			ProfissionalRepository profissionais, ServicoRepository servicos,
			EstabelecimentoRepository estabelecimentos, BloqueioAgendaRepository bloqueios,
			ExecucaoSerializada execucao, Clock relogio) {
		return new CriarAgendamentoUseCase(agendamentos, profissionais, servicos, estabelecimentos, bloqueios,
				execucao, relogio);
	}

	/** Porta do módulo de profissionais, para recusar bloqueio sobre agendamento ativo. */
	@Bean
	AgendamentosAtivos agendamentosAtivos(AgendamentoRepository agendamentos) {
		return new AgendamentosAtivosDoProfissional(agendamentos);
	}

	@Bean
	ListarMeusAgendamentosUseCase listarMeusAgendamentosUseCase(AgendamentoRepository agendamentos) {
		return new ListarMeusAgendamentosUseCase(agendamentos);
	}

	@Bean
	ConfirmarAgendamentoUseCase confirmarAgendamentoUseCase(AgendamentoRepository agendamentos,
			ProfissionalRepository profissionais, EstabelecimentoRepository estabelecimentos, Clock relogio) {
		return new ConfirmarAgendamentoUseCase(agendamentos, profissionais, estabelecimentos, relogio);
	}

	@Bean
	CancelarAgendamentoUseCase cancelarAgendamentoUseCase(AgendamentoRepository agendamentos,
			ProfissionalRepository profissionais, EstabelecimentoRepository estabelecimentos, Clock relogio) {
		return new CancelarAgendamentoUseCase(agendamentos, profissionais, estabelecimentos, relogio);
	}

	@Bean
	ConcluirAgendamentoUseCase concluirAgendamentoUseCase(AgendamentoRepository agendamentos,
			ProfissionalRepository profissionais, EstabelecimentoRepository estabelecimentos, Clock relogio) {
		return new ConcluirAgendamentoUseCase(agendamentos, profissionais, estabelecimentos, relogio);
	}

	@Bean
	RegistrarNaoComparecimentoUseCase registrarNaoComparecimentoUseCase(AgendamentoRepository agendamentos,
			ProfissionalRepository profissionais, EstabelecimentoRepository estabelecimentos, Clock relogio) {
		return new RegistrarNaoComparecimentoUseCase(agendamentos, profissionais, estabelecimentos, relogio);
	}

	@Bean
	ConsultarAgendamentoUseCase consultarAgendamentoUseCase(AgendamentoRepository agendamentos,
			ProfissionalRepository profissionais, EstabelecimentoRepository estabelecimentos) {
		return new ConsultarAgendamentoUseCase(agendamentos, profissionais, estabelecimentos);
	}

}
