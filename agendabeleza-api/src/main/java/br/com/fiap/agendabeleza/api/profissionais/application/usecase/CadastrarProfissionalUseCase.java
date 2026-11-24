package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Cadastra um profissional no estabelecimento. Somente o gestor dono pode cadastrar.
 */
public class CadastrarProfissionalUseCase {

	private final EstabelecimentoRepository estabelecimentos;
	private final ProfissionalRepository profissionais;

	public CadastrarProfissionalUseCase(EstabelecimentoRepository estabelecimentos,
			ProfissionalRepository profissionais) {
		this.estabelecimentos = estabelecimentos;
		this.profissionais = profissionais;
	}

	public Profissional executar(Comando comando) {
		Estabelecimento estabelecimento = estabelecimentos.buscarPorId(comando.estabelecimentoId())
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Estabelecimento " + comando.estabelecimentoId() + " não encontrado"));
		estabelecimento.garantirGestor(comando.usuarioId());
		Profissional profissional = Profissional.cadastrar(estabelecimento.id(), comando.usuarioProfissionalId(),
				comando.nome(), comando.email(), comando.bio());
		return profissionais.salvar(profissional);
	}

	/**
	 * @param usuarioId             quem faz a requisição (o gestor)
	 * @param usuarioProfissionalId login do profissional, quando ele tiver um
	 */
	public record Comando(
			UUID estabelecimentoId,
			UUID usuarioId,
			UUID usuarioProfissionalId,
			String nome,
			String email,
			String bio) {
	}

}
