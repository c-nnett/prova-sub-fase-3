package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Carrega o profissional e o estabelecimento dele e garante que o usuário é o gestor ou o próprio profissional.
 * Usado pelos casos de uso de alteração.
 */
record ProfissionalAlteravel(Profissional profissional, Estabelecimento estabelecimento) {

	static ProfissionalAlteravel obter(ProfissionalRepository profissionais, EstabelecimentoRepository estabelecimentos,
			UUID profissionalId, UUID usuarioId) {
		Profissional profissional = ProfissionalEncontrado.obter(profissionais, profissionalId);
		Estabelecimento estabelecimento = estabelecimentos.buscarPorId(profissional.estabelecimentoId())
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Estabelecimento " + profissional.estabelecimentoId() + " não encontrado"));
		profissional.garantirPodeAlterar(usuarioId, estabelecimento.gestorId());
		return new ProfissionalAlteravel(profissional, estabelecimento);
	}

}
