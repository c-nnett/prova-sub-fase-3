package br.com.fiap.agendabeleza.api.avaliacoes.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.avaliacoes.application.port.AvaliacaoRepository;
import br.com.fiap.agendabeleza.api.avaliacoes.application.usecase.AvaliarAtendimentoUseCase;
import br.com.fiap.agendabeleza.api.avaliacoes.application.usecase.ListarAvaliacoesUseCase;
import br.com.fiap.agendabeleza.api.compartilhado.application.port.ExecucaoSerializada;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;

/**
 * Registra os casos de uso do módulo como beans. As portas dos módulos de agendamentos, estabelecimentos e
 * profissionais são recebidas pela interface (arquitetura, seção 4.2).
 */
@Configuration(proxyBeanMethods = false)
class AvaliacoesConfig {

	@Bean
	@SuppressWarnings("java:S107") // Mesmas dependências do construtor do caso de uso.
	AvaliarAtendimentoUseCase avaliarAtendimentoUseCase(AgendamentoRepository agendamentos,
			AvaliacaoRepository avaliacoes, EstabelecimentoRepository estabelecimentos,
			ProfissionalRepository profissionais, ExecucaoSerializada execucao, Clock relogio) {
		return new AvaliarAtendimentoUseCase(agendamentos, avaliacoes, estabelecimentos, profissionais, execucao,
				relogio);
	}

	@Bean
	ListarAvaliacoesUseCase listarAvaliacoesUseCase(AvaliacaoRepository avaliacoes,
			EstabelecimentoRepository estabelecimentos, ProfissionalRepository profissionais) {
		return new ListarAvaliacoesUseCase(avaliacoes, estabelecimentos, profissionais);
	}

}
