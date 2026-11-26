package br.com.fiap.agendabeleza.api.calendario.infrastructure;

import java.time.Clock;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.calendario.application.port.AssinaturaCalendarioRepository;
import br.com.fiap.agendabeleza.api.calendario.application.port.CalendarioGateway;
import br.com.fiap.agendabeleza.api.calendario.application.usecase.CalendarioDoCliente;
import br.com.fiap.agendabeleza.api.calendario.application.usecase.CalendarioDoProfissional;
import br.com.fiap.agendabeleza.api.calendario.application.usecase.EmitirAssinaturaDoCalendarioUseCase;
import br.com.fiap.agendabeleza.api.calendario.application.usecase.GerarFeedDoCalendarioUseCase;
import br.com.fiap.agendabeleza.api.calendario.application.usecase.TitularesDoCalendario;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;

/**
 * Registra as estratégias de cada papel e os casos de uso do feed como beans. O calendário só lê agendamentos e
 * cadastros pelas portas de {@code application} dos outros módulos (arquitetura, seção 4.2).
 */
@Configuration(proxyBeanMethods = false)
class CalendarioConfig {

	@Bean
	TitularesDoCalendario titularesDoCalendario(ProfissionalRepository profissionais,
			EstabelecimentoRepository estabelecimentos, AgendamentoRepository agendamentos) {
		return new TitularesDoCalendario(List.of(
				new CalendarioDoProfissional(profissionais, estabelecimentos, agendamentos),
				new CalendarioDoCliente(agendamentos)));
	}

	@Bean
	EmitirAssinaturaDoCalendarioUseCase emitirAssinaturaDoCalendarioUseCase(TitularesDoCalendario titulares,
			AssinaturaCalendarioRepository assinaturas, Clock relogio) {
		return new EmitirAssinaturaDoCalendarioUseCase(titulares, assinaturas, relogio);
	}

	@Bean
	GerarFeedDoCalendarioUseCase gerarFeedDoCalendarioUseCase(TitularesDoCalendario titulares,
			AssinaturaCalendarioRepository assinaturas, EstabelecimentoRepository estabelecimentos,
			ServicoRepository servicos, ProfissionalRepository profissionais, CalendarioGateway calendario,
			Clock relogio) {
		return new GerarFeedDoCalendarioUseCase(titulares, assinaturas, estabelecimentos, servicos, profissionais,
				calendario, relogio);
	}

}
