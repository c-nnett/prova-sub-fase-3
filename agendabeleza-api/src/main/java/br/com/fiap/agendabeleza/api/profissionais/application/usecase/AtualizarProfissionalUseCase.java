package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Atualiza o perfil do profissional, inclusive ativando ou desativando. Gestor ou o próprio profissional.
 */
public class AtualizarProfissionalUseCase {

	private final EstabelecimentoRepository estabelecimentos;
	private final ProfissionalRepository profissionais;

	public AtualizarProfissionalUseCase(EstabelecimentoRepository estabelecimentos,
			ProfissionalRepository profissionais) {
		this.estabelecimentos = estabelecimentos;
		this.profissionais = profissionais;
	}

	public Profissional executar(Comando comando) {
		Profissional profissional = ProfissionalAlteravel.obter(profissionais, estabelecimentos,
				comando.profissionalId(), comando.usuarioId()).profissional();
		profissional.atualizarPerfil(comando.nome(), comando.email(), comando.bio(), comando.ativo());
		return profissionais.salvar(profissional);
	}

	public record Comando(
			UUID profissionalId,
			UUID usuarioId,
			String nome,
			String email,
			String bio,
			boolean ativo) {
	}

}
