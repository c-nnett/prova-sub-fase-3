package br.com.fiap.agendabeleza.api.profissionais.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.AgendamentosAtivos;
import br.com.fiap.agendabeleza.api.profissionais.application.port.BloqueioAgendaRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.EspecialidadeRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.AtualizarProfissionalUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.CadastrarProfissionalUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.ConsultarProfissionalUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.CriarBloqueioUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.DefinirEspecialidadesUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.DefinirJornadaUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.DefinirServicosProfissionalUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.DetalharProfissionaisUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.ListarBloqueiosUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.ListarEspecialidadesUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.ListarProfissionaisUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.RemoverBloqueioUseCase;

/**
 * Registra os casos de uso do módulo como beans. Os que dependem do estabelecimento ou dos serviços recebem as
 * portas do módulo {@code estabelecimentos} pela interface (arquitetura, seção 4.2).
 */
@Configuration(proxyBeanMethods = false)
class ProfissionaisConfig {

	@Bean
	CadastrarProfissionalUseCase cadastrarProfissionalUseCase(EstabelecimentoRepository estabelecimentos,
			ProfissionalRepository profissionais) {
		return new CadastrarProfissionalUseCase(estabelecimentos, profissionais);
	}

	@Bean
	AtualizarProfissionalUseCase atualizarProfissionalUseCase(EstabelecimentoRepository estabelecimentos,
			ProfissionalRepository profissionais) {
		return new AtualizarProfissionalUseCase(estabelecimentos, profissionais);
	}

	@Bean
	ConsultarProfissionalUseCase consultarProfissionalUseCase(ProfissionalRepository profissionais) {
		return new ConsultarProfissionalUseCase(profissionais);
	}

	@Bean
	ListarProfissionaisUseCase listarProfissionaisUseCase(EstabelecimentoRepository estabelecimentos,
			ProfissionalRepository profissionais) {
		return new ListarProfissionaisUseCase(estabelecimentos, profissionais);
	}

	@Bean
	DefinirEspecialidadesUseCase definirEspecialidadesUseCase(EstabelecimentoRepository estabelecimentos,
			ProfissionalRepository profissionais, EspecialidadeRepository especialidades) {
		return new DefinirEspecialidadesUseCase(estabelecimentos, profissionais, especialidades);
	}

	@Bean
	DefinirServicosProfissionalUseCase definirServicosProfissionalUseCase(EstabelecimentoRepository estabelecimentos,
			ProfissionalRepository profissionais, ServicoRepository servicos) {
		return new DefinirServicosProfissionalUseCase(estabelecimentos, profissionais, servicos);
	}

	@Bean
	DefinirJornadaUseCase definirJornadaUseCase(EstabelecimentoRepository estabelecimentos,
			ProfissionalRepository profissionais) {
		return new DefinirJornadaUseCase(estabelecimentos, profissionais);
	}

	@Bean
	CriarBloqueioUseCase criarBloqueioUseCase(EstabelecimentoRepository estabelecimentos,
			ProfissionalRepository profissionais, BloqueioAgendaRepository bloqueios,
			AgendamentosAtivos agendamentosAtivos, Clock relogio) {
		return new CriarBloqueioUseCase(estabelecimentos, profissionais, bloqueios, agendamentosAtivos, relogio);
	}

	@Bean
	ListarBloqueiosUseCase listarBloqueiosUseCase(EstabelecimentoRepository estabelecimentos,
			ProfissionalRepository profissionais, BloqueioAgendaRepository bloqueios, Clock relogio) {
		return new ListarBloqueiosUseCase(estabelecimentos, profissionais, bloqueios, relogio);
	}

	@Bean
	RemoverBloqueioUseCase removerBloqueioUseCase(EstabelecimentoRepository estabelecimentos,
			ProfissionalRepository profissionais, BloqueioAgendaRepository bloqueios) {
		return new RemoverBloqueioUseCase(estabelecimentos, profissionais, bloqueios);
	}

	@Bean
	ListarEspecialidadesUseCase listarEspecialidadesUseCase(EspecialidadeRepository especialidades) {
		return new ListarEspecialidadesUseCase(especialidades);
	}

	@Bean
	DetalharProfissionaisUseCase detalharProfissionaisUseCase(EspecialidadeRepository especialidades) {
		return new DetalharProfissionaisUseCase(especialidades);
	}

}
