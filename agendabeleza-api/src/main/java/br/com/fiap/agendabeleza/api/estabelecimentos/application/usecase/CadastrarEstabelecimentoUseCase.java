package br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase;

import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Endereco;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.HorarioFuncionamento;

/**
 * Cadastra um estabelecimento para o gestor identificado, já com horários e fotos opcionais.
 */
public class CadastrarEstabelecimentoUseCase {

	private final EstabelecimentoRepository repositorio;

	public CadastrarEstabelecimentoUseCase(EstabelecimentoRepository repositorio) {
		this.repositorio = repositorio;
	}

	public Estabelecimento executar(Comando comando) {
		Estabelecimento estabelecimento = Estabelecimento.cadastrar(
				comando.gestorId(), comando.nome(), comando.descricao(), comando.endereco(), comando.timezone());
		estabelecimento.definirHorarios(comando.horarios());
		if (comando.fotos() != null) {
			comando.fotos().forEach(estabelecimento::adicionarFoto);
		}
		return repositorio.salvar(estabelecimento);
	}

	public record Comando(
			UUID gestorId,
			String nome,
			String descricao,
			Endereco endereco,
			String timezone,
			List<HorarioFuncionamento> horarios,
			List<String> fotos) {
	}

}
