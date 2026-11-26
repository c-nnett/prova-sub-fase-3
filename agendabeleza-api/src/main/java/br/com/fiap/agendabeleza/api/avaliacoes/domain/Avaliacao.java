package br.com.fiap.agendabeleza.api.avaliacoes.domain;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.obrigatorio;
import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.textoOpcional;

import java.time.Instant;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

/**
 * Avaliação de um atendimento concluído: uma nota para o estabelecimento e outra para o profissional, de 1 a 5, e um
 * comentário opcional (funcionalidade 4; modelagem, seções 3.5 e 4.3). Os identificadores do estabelecimento, do
 * profissional e do cliente vêm do agendamento.
 */
public final class Avaliacao {

	public static final int NOTA_MINIMA = 1;
	public static final int NOTA_MAXIMA = 5;
	public static final int TAMANHO_MAXIMO_COMENTARIO = 1000;
	public static final String AGENDAMENTO_NAO_CONCLUIDO = "Somente atendimentos concluídos podem ser avaliados";

	private final UUID id;
	private final UUID agendamentoId;
	private final UUID clienteId;
	private final UUID estabelecimentoId;
	private final UUID profissionalId;
	private final int notaEstabelecimento;
	private final int notaProfissional;
	private final String comentario;
	private final Instant criadaEm;

	@SuppressWarnings("java:S107") // Mesmas colunas da tabela avaliacao.
	private Avaliacao(UUID id, UUID agendamentoId, UUID clienteId, UUID estabelecimentoId, UUID profissionalId,
			Integer notaEstabelecimento, Integer notaProfissional, String comentario, Instant criadaEm) {
		this.id = obrigatorio(id, "id");
		this.agendamentoId = obrigatorio(agendamentoId, "agendamentoId");
		this.clienteId = obrigatorio(clienteId, "clienteId");
		this.estabelecimentoId = obrigatorio(estabelecimentoId, "estabelecimentoId");
		this.profissionalId = obrigatorio(profissionalId, "profissionalId");
		this.notaEstabelecimento = validarNota(notaEstabelecimento, "notaEstabelecimento");
		this.notaProfissional = validarNota(notaProfissional, "notaProfissional");
		this.comentario = textoOpcional(comentario, "comentario", TAMANHO_MAXIMO_COMENTARIO);
		this.criadaEm = obrigatorio(criadaEm, "criadaEm");
	}

	/**
	 * @throws RegraNegocioException se o agendamento não estiver {@code CONCLUIDO}, se uma nota faltar ou estiver fora
	 *                               de 1 a 5, ou se o comentário passar do tamanho máximo
	 */
	public static Avaliacao registrar(Agendamento agendamento, Integer notaEstabelecimento, Integer notaProfissional,
			String comentario, Instant agora) {
		if (agendamento.status() != StatusAgendamento.CONCLUIDO) {
			throw new RegraNegocioException(AGENDAMENTO_NAO_CONCLUIDO);
		}
		return new Avaliacao(UUID.randomUUID(), agendamento.id(), agendamento.clienteId(),
				agendamento.estabelecimentoId(), agendamento.profissionalId(), notaEstabelecimento, notaProfissional,
				comentario, agora);
	}

	@SuppressWarnings("java:S107") // Mesmas colunas da tabela avaliacao.
	public static Avaliacao reconstituir(UUID id, UUID agendamentoId, UUID clienteId, UUID estabelecimentoId,
			UUID profissionalId, int notaEstabelecimento, int notaProfissional, String comentario,
			Instant criadaEm) {
		return new Avaliacao(id, agendamentoId, clienteId, estabelecimentoId, profissionalId, notaEstabelecimento,
				notaProfissional, comentario, criadaEm);
	}

	private static int validarNota(Integer nota, String campo) {
		obrigatorio(nota, campo);
		if (nota < NOTA_MINIMA || nota > NOTA_MAXIMA) {
			throw new RegraNegocioException(campo + " deve ser de " + NOTA_MINIMA + " a " + NOTA_MAXIMA);
		}
		return nota;
	}

	public UUID id() {
		return id;
	}

	public UUID agendamentoId() {
		return agendamentoId;
	}

	public UUID clienteId() {
		return clienteId;
	}

	public UUID estabelecimentoId() {
		return estabelecimentoId;
	}

	public UUID profissionalId() {
		return profissionalId;
	}

	public int notaEstabelecimento() {
		return notaEstabelecimento;
	}

	public int notaProfissional() {
		return notaProfissional;
	}

	public String comentario() {
		return comentario;
	}

	public Instant criadaEm() {
		return criadaEm;
	}

}
