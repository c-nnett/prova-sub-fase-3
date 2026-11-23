package br.com.fiap.agendabeleza.api.agendamentos.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConsultarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConsultarHorariosLivresUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.CriarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ListarMeusAgendamentosUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
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
			BloqueioAgendaRepository bloqueios, Clock relogio) {
		return new ConsultarHorariosLivresUseCase(profissionais, estabelecimentos, servicos, bloqueios, relogio);
	}

	@Bean
	CriarAgendamentoUseCase criarAgendamentoUseCase(AgendamentoRepository agendamentos,
			ProfissionalRepository profissionais, ServicoRepository servicos,
			EstabelecimentoRepository estabelecimentos, Clock relogio) {
		return new CriarAgendamentoUseCase(agendamentos, profissionais, servicos, estabelecimentos, relogio);
	}

	@Bean
	ListarMeusAgendamentosUseCase listarMeusAgendamentosUseCase(AgendamentoRepository agendamentos) {
		return new ListarMeusAgendamentosUseCase(agendamentos);
	}

	@Bean
	ConsultarAgendamentoUseCase consultarAgendamentoUseCase(AgendamentoRepository agendamentos,
			ProfissionalRepository profissionais, EstabelecimentoRepository estabelecimentos) {
		return new ConsultarAgendamentoUseCase(agendamentos, profissionais, estabelecimentos);
	}

}
