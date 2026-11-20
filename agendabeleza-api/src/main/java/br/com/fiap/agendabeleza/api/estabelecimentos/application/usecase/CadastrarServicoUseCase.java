package br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;

/**
 * Cadastra um serviço no estabelecimento. Somente o gestor dono pode cadastrar.
 */
public class CadastrarServicoUseCase {

	private final EstabelecimentoRepository estabelecimentos;
	private final ServicoRepository servicos;

	public CadastrarServicoUseCase(EstabelecimentoRepository estabelecimentos, ServicoRepository servicos) {
		this.estabelecimentos = estabelecimentos;
		this.servicos = servicos;
	}

	public Servico executar(Comando comando) {
		Estabelecimento estabelecimento = EstabelecimentoDoGestor.obter(
				estabelecimentos, comando.estabelecimentoId(), comando.usuarioId());
		Servico servico = Servico.cadastrar(estabelecimento.id(), comando.nome(), comando.descricao(),
				comando.categoria(), comando.duracaoMinutos(), comando.precoBase());
		return servicos.salvar(servico);
	}

	public record Comando(
			UUID estabelecimentoId,
			UUID usuarioId,
			String nome,
			String descricao,
			CategoriaServico categoria,
			int duracaoMinutos,
			BigDecimal precoBase) {
	}

}
