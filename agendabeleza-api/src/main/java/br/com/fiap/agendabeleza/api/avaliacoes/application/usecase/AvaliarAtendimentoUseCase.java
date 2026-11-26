package br.com.fiap.agendabeleza.api.avaliacoes.application.usecase;

import java.time.Clock;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.avaliacoes.application.port.AvaliacaoRepository;
import br.com.fiap.agendabeleza.api.avaliacoes.domain.Avaliacao;
import br.com.fiap.agendabeleza.api.compartilhado.application.port.ExecucaoSerializada;
import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;

/**
 * O cliente avalia um atendimento concluído, e as notas médias do estabelecimento e do profissional são recalculadas
 * na mesma transação (modelagem, seção 4.3).
 * <p>
 * A gravação roda em {@link ExecucaoSerializada} com a chave do estabelecimento: duas avaliações do mesmo
 * estabelecimento não recalculam as médias ao mesmo tempo, e nenhuma se perde. Como o profissional pertence a um
 * único estabelecimento, a mesma trava também protege a média dele.
 */
public class AvaliarAtendimentoUseCase {

	public static final String SOMENTE_O_CLIENTE = "Somente o cliente do agendamento pode avaliá-lo";
	public static final String JA_AVALIADO = "Este atendimento já foi avaliado";

	private final AgendamentoRepository agendamentos;
	private final AvaliacaoRepository avaliacoes;
	private final EstabelecimentoRepository estabelecimentos;
	private final ProfissionalRepository profissionais;
	private final ExecucaoSerializada execucao;
	private final Clock relogio;

	@SuppressWarnings("java:S107") // Portas de três módulos, a trava e o relógio.
	public AvaliarAtendimentoUseCase(AgendamentoRepository agendamentos, AvaliacaoRepository avaliacoes,
			EstabelecimentoRepository estabelecimentos, ProfissionalRepository profissionais,
			ExecucaoSerializada execucao, Clock relogio) {
		this.agendamentos = agendamentos;
		this.avaliacoes = avaliacoes;
		this.estabelecimentos = estabelecimentos;
		this.profissionais = profissionais;
		this.execucao = execucao;
		this.relogio = relogio;
	}

	/**
	 * @throws RecursoNaoEncontradoException se o agendamento não existir
	 * @throws AcessoNegadoException         se o usuário não for o cliente do agendamento
	 * @throws RegraNegocioException         se o atendimento não estiver concluído, já tiver sido avaliado ou as
	 *                                       notas forem inválidas
	 */
	public Avaliacao executar(Comando comando) {
		Agendamento agendamento = agendamentos.buscarPorId(comando.agendamentoId())
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Agendamento " + comando.agendamentoId() + " não encontrado"));
		if (!agendamento.pertenceAoCliente(comando.usuarioId())) {
			throw new AcessoNegadoException(SOMENTE_O_CLIENTE);
		}
		return execucao.executar(agendamento.estabelecimentoId(), () -> registrar(agendamento, comando));
	}

	private Avaliacao registrar(Agendamento agendamento, Comando comando) {
		if (avaliacoes.existeDoAgendamento(agendamento.id())) {
			throw new RegraNegocioException(JA_AVALIADO);
		}
		Avaliacao avaliacao = avaliacoes.salvar(Avaliacao.registrar(agendamento, comando.notaEstabelecimento(),
				comando.notaProfissional(), comando.comentario(), relogio.instant()));
		estabelecimentos.atualizarAvaliacoes(avaliacao.estabelecimentoId(),
				avaliacoes.notasDoEstabelecimento(avaliacao.estabelecimentoId()));
		profissionais.atualizarAvaliacoes(avaliacao.profissionalId(),
				avaliacoes.notasDoProfissional(avaliacao.profissionalId()));
		return avaliacao;
	}

	public record Comando(UUID agendamentoId, UUID usuarioId, Integer notaEstabelecimento, Integer notaProfissional,
			String comentario) {
	}

}
