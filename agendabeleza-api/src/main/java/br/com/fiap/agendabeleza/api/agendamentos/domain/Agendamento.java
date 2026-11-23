package br.com.fiap.agendabeleza.api.agendamentos.domain;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.obrigatorio;
import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.textoOpcional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

/**
 * Agregado do agendamento: um serviço com um profissional, em um intervalo {@code [inicio, fim)}.
 * <p>
 * Toda mudança de status segue {@link StatusAgendamento#podeIrPara} e gera uma {@link AlteracaoAgendamento}, que o
 * adaptador de persistência grava em {@code agendamento_historico}. O instante atual é sempre recebido como
 * parâmetro, para que as regras de horário sejam testáveis.
 */
public class Agendamento {

	private final UUID id;
	private final UUID clienteId;
	private final UUID estabelecimentoId;
	private final UUID profissionalId;
	private final UUID servicoId;
	private final BigDecimal preco;
	private final String observacoes;
	private final Long versao;
	private Instant inicio;
	private Instant fim;
	private StatusAgendamento status;
	private final List<AlteracaoAgendamento> alteracoesPendentes = new ArrayList<>();

	private Agendamento(UUID id, UUID clienteId, UUID estabelecimentoId, UUID profissionalId, UUID servicoId,
			Instant inicio, Instant fim, StatusAgendamento status, BigDecimal preco, String observacoes,
			Long versao) {
		this.id = obrigatorio(id, "id");
		this.clienteId = obrigatorio(clienteId, "clienteId");
		this.estabelecimentoId = obrigatorio(estabelecimentoId, "estabelecimentoId");
		this.profissionalId = obrigatorio(profissionalId, "profissionalId");
		this.servicoId = obrigatorio(servicoId, "servicoId");
		this.inicio = obrigatorio(inicio, "inicio");
		this.fim = obrigatorio(fim, "fim");
		if (!fim.isAfter(inicio)) {
			throw new RegraNegocioException("fim deve ser posterior ao inicio");
		}
		this.status = obrigatorio(status, "status");
		this.preco = validarPreco(preco);
		this.observacoes = textoOpcional(observacoes, "observacoes", 500);
		this.versao = versao;
	}

	/**
	 * Reserva um horário no futuro. O preço é a tarifa congelada no momento da reserva, e o fim é calculado pela
	 * duração do serviço.
	 */
	public static Agendamento reservar(UUID clienteId, UUID estabelecimentoId, UUID profissionalId, UUID servicoId,
			Instant inicio, int duracaoMinutos, BigDecimal preco, String observacoes, Instant agora) {
		exigirFuturo(obrigatorio(inicio, "inicio"), agora);
		if (duracaoMinutos <= 0) {
			throw new RegraNegocioException("duracaoMinutos deve ser maior que zero");
		}
		Agendamento agendamento = new Agendamento(UUID.randomUUID(), clienteId, estabelecimentoId, profissionalId,
				servicoId, inicio, inicio.plus(Duration.ofMinutes(duracaoMinutos)), StatusAgendamento.AGENDADO, preco,
				observacoes, null);
		agendamento.alteracoesPendentes.add(
				AlteracaoAgendamento.deStatus(null, StatusAgendamento.AGENDADO, null, clienteId, agora));
		return agendamento;
	}

	/** Recria o agregado a partir dos dados gravados, sem gerar histórico nem exigir início no futuro. */
	public static Agendamento reconstituir(UUID id, UUID clienteId, UUID estabelecimentoId, UUID profissionalId,
			UUID servicoId, Instant inicio, Instant fim, StatusAgendamento status, BigDecimal preco,
			String observacoes, Long versao) {
		return new Agendamento(id, clienteId, estabelecimentoId, profissionalId, servicoId, inicio, fim, status,
				preco, observacoes, versao);
	}

	/** O estabelecimento confirma a reserva: {@code AGENDADO -> CONFIRMADO}. */
	public void confirmar(UUID alteradoPor, Instant agora) {
		mudarStatus(StatusAgendamento.CONFIRMADO, "confirmar", null, alteradoPor, agora);
	}

	/** Cancela um agendamento ativo, liberando o horário. O motivo é opcional. */
	public void cancelar(String motivo, UUID alteradoPor, Instant agora) {
		exigirTransicao(StatusAgendamento.CANCELADO, "cancelar");
		String motivoValidado = textoOpcional(motivo, "motivo", 250);
		mudarStatus(StatusAgendamento.CANCELADO, "cancelar", motivoValidado, alteradoPor, agora);
	}

	/** Move o agendamento para outro início, mantendo a duração. O status volta para {@code AGENDADO}. */
	public void reagendar(Instant novoInicio, UUID alteradoPor, Instant agora) {
		exigirTransicao(StatusAgendamento.AGENDADO, "reagendar");
		exigirFuturo(obrigatorio(novoInicio, "novoInicio"), agora);
		if (novoInicio.equals(inicio)) {
			throw new RegraNegocioException("o novo início deve ser diferente do atual");
		}
		Duration duracao = Duration.between(inicio, fim);
		Instant inicioAnterior = inicio;
		StatusAgendamento statusAnterior = status;

		inicio = novoInicio;
		fim = novoInicio.plus(duracao);
		status = StatusAgendamento.AGENDADO;
		alteracoesPendentes.add(new AlteracaoAgendamento(statusAnterior, status, inicioAnterior, novoInicio, null,
				obrigatorio(alteradoPor, "alteradoPor"), obrigatorio(agora, "agora")));
	}

	/** Registra o atendimento realizado. Só vale para agendamento confirmado e a partir do início. */
	public void concluir(UUID alteradoPor, Instant agora) {
		exigirTransicao(StatusAgendamento.CONCLUIDO, "concluir");
		exigirInicioAlcancado(agora, "o atendimento só pode ser concluído depois do início");
		mudarStatus(StatusAgendamento.CONCLUIDO, "concluir", null, alteradoPor, agora);
	}

	/** Registra que o cliente faltou. Só vale para agendamento confirmado e a partir do início. */
	public void registrarNaoComparecimento(UUID alteradoPor, Instant agora) {
		String acao = "registrar o não comparecimento de";
		exigirTransicao(StatusAgendamento.NAO_COMPARECEU, acao);
		exigirInicioAlcancado(agora, "o não comparecimento só pode ser registrado depois do início");
		mudarStatus(StatusAgendamento.NAO_COMPARECEU, acao, null, alteradoPor, agora);
	}

	public boolean ocupaHorario() {
		return status.ocupaHorario();
	}

	/** Intervalo {@code [inicio, fim)} ocupado pelo atendimento. */
	public Intervalo intervalo() {
		return new Intervalo(inicio, fim);
	}

	public boolean pertenceAoCliente(UUID clienteId) {
		return this.clienteId.equals(clienteId);
	}

	/** Alterações feitas desde a reserva ou a reconstituição, na ordem em que ocorreram. */
	public List<AlteracaoAgendamento> alteracoesPendentes() {
		return Collections.unmodifiableList(alteracoesPendentes);
	}

	public UUID id() {
		return id;
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

	public UUID servicoId() {
		return servicoId;
	}

	public Instant inicio() {
		return inicio;
	}

	public Instant fim() {
		return fim;
	}

	public int duracaoMinutos() {
		return (int) Duration.between(inicio, fim).toMinutes();
	}

	public StatusAgendamento status() {
		return status;
	}

	public BigDecimal preco() {
		return preco;
	}

	public String observacoes() {
		return observacoes;
	}

	/** Versão do bloqueio otimista. Nula enquanto o agendamento não foi gravado. */
	public Long versao() {
		return versao;
	}

	private void mudarStatus(StatusAgendamento novo, String acao, String motivo, UUID alteradoPor, Instant agora) {
		exigirTransicao(novo, acao);
		StatusAgendamento anterior = status;
		alteracoesPendentes.add(AlteracaoAgendamento.deStatus(anterior, novo, motivo,
				obrigatorio(alteradoPor, "alteradoPor"), obrigatorio(agora, "agora")));
		status = novo;
	}

	private void exigirTransicao(StatusAgendamento destino, String acao) {
		if (!status.podeIrPara(destino)) {
			throw new TransicaoInvalidaException(acao, status);
		}
	}

	private void exigirInicioAlcancado(Instant agora, String mensagem) {
		if (obrigatorio(agora, "agora").isBefore(inicio)) {
			throw new RegraNegocioException(mensagem);
		}
	}

	private static void exigirFuturo(Instant inicio, Instant agora) {
		if (!inicio.isAfter(obrigatorio(agora, "agora"))) {
			throw new RegraNegocioException("inicio deve estar no futuro");
		}
	}

	private static BigDecimal validarPreco(BigDecimal preco) {
		obrigatorio(preco, "preco");
		if (preco.signum() < 0) {
			throw new RegraNegocioException("preco não pode ser negativo");
		}
		if (preco.stripTrailingZeros().scale() > 2) {
			throw new RegraNegocioException("preco deve ter no máximo duas casas decimais");
		}
		return preco.setScale(2, RoundingMode.UNNECESSARY);
	}

}
