package br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;

/**
 * Atualiza um serviço, inclusive ativando ou desativando. O serviço precisa pertencer ao estabelecimento
 * informado, e somente o gestor dono pode alterar.
 */
public class AtualizarServicoUseCase {

	private final EstabelecimentoRepository estabelecimentos;
	private final ServicoRepository servicos;

	public AtualizarServicoUseCase(EstabelecimentoRepository estabelecimentos, ServicoRepository servicos) {
		this.estabelecimentos = estabelecimentos;
		this.servicos = servicos;
	}

	public Servico executar(Comando comando) {
		EstabelecimentoDoGestor.obter(estabelecimentos, comando.estabelecimentoId(), comando.usuarioId());
		Servico servico = servicos.buscarPorId(comando.servicoId())
				.filter(encontrado -> encontrado.pertenceA(comando.estabelecimentoId()))
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Serviço " + comando.servicoId() + " não encontrado"));
		servico.atualizar(comando.nome(), comando.descricao(), comando.categoria(), comando.duracaoMinutos(),
				comando.precoBase(), comando.ativo());
		return servicos.salvar(servico);
	}

	public record Comando(
			UUID estabelecimentoId,
			UUID servicoId,
			UUID usuarioId,
			String nome,
			String descricao,
			CategoriaServico categoria,
			int duracaoMinutos,
			BigDecimal precoBase,
			boolean ativo) {
	}

}
