package br.com.fiap.agendabeleza.api.calendario.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.calendario.domain.PapelNoCalendario;
import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Feed com os atendimentos do profissional. Assinam o próprio profissional e o gestor do estabelecimento, as mesmas
 * pessoas que podem alterar a agenda dele. O título do evento é o serviço: o estabelecimento é sempre o mesmo.
 */
public class CalendarioDoProfissional implements TitularDoCalendario {

	public static final String SOMENTE_GESTOR_OU_PROFISSIONAL =
			"Somente o gestor do estabelecimento ou o próprio profissional pode assinar a agenda dele";

	private final ProfissionalRepository profissionais;
	private final EstabelecimentoRepository estabelecimentos;
	private final AgendamentoRepository agendamentos;

	public CalendarioDoProfissional(ProfissionalRepository profissionais, EstabelecimentoRepository estabelecimentos,
			AgendamentoRepository agendamentos) {
		this.profissionais = profissionais;
		this.estabelecimentos = estabelecimentos;
		this.agendamentos = agendamentos;
	}

	@Override
	public PapelNoCalendario papel() {
		return PapelNoCalendario.PROFISSIONAL;
	}

	@Override
	public void exigirAcesso(UUID titularId, UUID usuarioId) {
		Profissional profissional = profissional(titularId);
		Estabelecimento estabelecimento = estabelecimentos.buscarPorId(profissional.estabelecimentoId())
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Estabelecimento " + profissional.estabelecimentoId() + " não encontrado"));
		boolean gestor = usuarioId.equals(estabelecimento.gestorId());
		boolean proprioProfissional = usuarioId.equals(profissional.usuarioId());
		if (!gestor && !proprioProfissional) {
			throw new AcessoNegadoException(SOMENTE_GESTOR_OU_PROFISSIONAL);
		}
	}

	@Override
	public List<Agendamento> agendamentosDesde(UUID titularId, Instant desde) {
		return agendamentos.listarDoProfissionalDesde(titularId, desde);
	}

	@Override
	public String nomeDoCalendario(UUID titularId) {
		return "AgendaBeleza - " + profissional(titularId).nome();
	}

	@Override
	public String resumoDoEvento(Servico servico, Estabelecimento estabelecimento) {
		return servico.nome();
	}

	private Profissional profissional(UUID profissionalId) {
		return profissionais.buscarPorId(profissionalId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Profissional " + profissionalId
						+ " não encontrado"));
	}

}
