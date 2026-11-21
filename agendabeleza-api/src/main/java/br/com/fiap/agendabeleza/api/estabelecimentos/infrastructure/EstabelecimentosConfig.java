package br.com.fiap.agendabeleza.api.estabelecimentos.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.AdicionarFotoEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.AtualizarEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.AtualizarServicoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.CadastrarEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.CadastrarServicoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.ConsultarEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.DefinirHorariosFuncionamentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.ListarServicosUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.RemoverFotoEstabelecimentoUseCase;

/**
 * Registra os casos de uso do módulo como beans. Eles são classes Java comuns, sem anotações do Spring
 * (arquitetura, seção 4.5).
 */
@Configuration(proxyBeanMethods = false)
class EstabelecimentosConfig {

	@Bean
	CadastrarEstabelecimentoUseCase cadastrarEstabelecimentoUseCase(EstabelecimentoRepository repositorio) {
		return new CadastrarEstabelecimentoUseCase(repositorio);
	}

	@Bean
	ConsultarEstabelecimentoUseCase consultarEstabelecimentoUseCase(EstabelecimentoRepository repositorio) {
		return new ConsultarEstabelecimentoUseCase(repositorio);
	}

	@Bean
	AtualizarEstabelecimentoUseCase atualizarEstabelecimentoUseCase(EstabelecimentoRepository repositorio) {
		return new AtualizarEstabelecimentoUseCase(repositorio);
	}

	@Bean
	DefinirHorariosFuncionamentoUseCase definirHorariosFuncionamentoUseCase(EstabelecimentoRepository repositorio) {
		return new DefinirHorariosFuncionamentoUseCase(repositorio);
	}

	@Bean
	AdicionarFotoEstabelecimentoUseCase adicionarFotoEstabelecimentoUseCase(EstabelecimentoRepository repositorio) {
		return new AdicionarFotoEstabelecimentoUseCase(repositorio);
	}

	@Bean
	RemoverFotoEstabelecimentoUseCase removerFotoEstabelecimentoUseCase(EstabelecimentoRepository repositorio) {
		return new RemoverFotoEstabelecimentoUseCase(repositorio);
	}

	@Bean
	CadastrarServicoUseCase cadastrarServicoUseCase(EstabelecimentoRepository estabelecimentos,
			ServicoRepository servicos) {
		return new CadastrarServicoUseCase(estabelecimentos, servicos);
	}

	@Bean
	ListarServicosUseCase listarServicosUseCase(EstabelecimentoRepository estabelecimentos,
			ServicoRepository servicos) {
		return new ListarServicosUseCase(estabelecimentos, servicos);
	}

	@Bean
	AtualizarServicoUseCase atualizarServicoUseCase(EstabelecimentoRepository estabelecimentos,
			ServicoRepository servicos) {
		return new AtualizarServicoUseCase(estabelecimentos, servicos);
	}

}
