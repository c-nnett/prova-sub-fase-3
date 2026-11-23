package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.BloqueioAgendaRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.BloqueioAgenda;

/**
 * Remove um bloqueio do profissional. Gestor ou o próprio profissional. Um bloqueio de outro profissional é
 * tratado como inexistente.
 */
public class RemoverBloqueioUseCase {

	private final EstabelecimentoRepository estabelecimentos;
	private final ProfissionalRepository profissionais;
	private final BloqueioAgendaRepository bloqueios;

	public RemoverBloqueioUseCase(EstabelecimentoRepository estabelecimentos, ProfissionalRepository profissionais,
			BloqueioAgendaRepository bloqueios) {
		this.estabelecimentos = estabelecimentos;
		this.profissionais = profissionais;
		this.bloqueios = bloqueios;
	}

	public void executar(Comando comando) {
		ProfissionalAlteravel.obter(profissionais, estabelecimentos, comando.profissionalId(), comando.usuarioId());
		BloqueioAgenda bloqueio = bloqueios.buscarPorId(comando.bloqueioId())
				.filter(encontrado -> encontrado.pertenceA(comando.profissionalId()))
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Bloqueio " + comando.bloqueioId() + " não encontrado"));
		bloqueios.remover(bloqueio.id());
	}

	public record Comando(UUID profissionalId, UUID bloqueioId, UUID usuarioId) {
	}

}
