package br.com.fiap.agendabeleza.api.notificacoes.domain;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.obrigatorio;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

/**
 * E-mail registrado na outbox, gravado na mesma transação da alteração do agendamento e enviado depois pelo job
 * (modelagem, seção 3.6). Cada falha de envio conta uma tentativa; na {@value #MAXIMO_DE_TENTATIVAS}ª, o envio é
 * abandonado.
 */
public class Notificacao {

	public static final int MAXIMO_DE_TENTATIVAS = 5;
	public static final String SOMENTE_PENDENTE = "Somente notificação pendente pode ser enviada";
	/** Quanto antes do início o lembrete sai (planejamento, meta de 25/11). */
	public static final Duration ANTECEDENCIA_DO_LEMBRETE = Duration.ofHours(24);

	private final UUID id;
	private final UUID agendamentoId;
	private final TipoNotificacao tipo;
	private final PapelDestinatario papel;
	private final Mensagem mensagem;
	private final Instant enviarEm;
	private StatusNotificacao status;
	private int tentativas;
	private Instant enviadaEm;
	private String ultimoErro;

	@SuppressWarnings("java:S107") // Uma coluna da outbox por parâmetro, como nos demais reconstituir.
	private Notificacao(UUID id, UUID agendamentoId, TipoNotificacao tipo, PapelDestinatario papel,
			Mensagem mensagem, Instant enviarEm, StatusNotificacao status, int tentativas, Instant enviadaEm,
			String ultimoErro) {
		this.id = obrigatorio(id, "id");
		this.agendamentoId = obrigatorio(agendamentoId, "agendamentoId");
		this.tipo = obrigatorio(tipo, "tipo");
		this.papel = obrigatorio(papel, "papel");
		this.mensagem = obrigatorio(mensagem, "mensagem");
		this.enviarEm = obrigatorio(enviarEm, "enviarEm");
		this.status = obrigatorio(status, "status");
		this.tentativas = tentativas;
		this.enviadaEm = enviadaEm;
		this.ultimoErro = ultimoErro;
	}

	/** Registra um e-mail para ser enviado a partir de {@code enviarEm}. */
	public static Notificacao pendente(UUID agendamentoId, TipoNotificacao tipo, PapelDestinatario papel,
			Mensagem mensagem, Instant enviarEm) {
		return new Notificacao(UUID.randomUUID(), agendamentoId, tipo, papel, mensagem, enviarEm,
				StatusNotificacao.PENDENTE, 0, null, null);
	}

	/** Recria a notificação a partir dos dados gravados. */
	@SuppressWarnings("java:S107") // Uma coluna da outbox por parâmetro.
	public static Notificacao reconstituir(UUID id, UUID agendamentoId, TipoNotificacao tipo,
			PapelDestinatario papel, Mensagem mensagem, Instant enviarEm, StatusNotificacao status, int tentativas,
			Instant enviadaEm, String ultimoErro) {
		return new Notificacao(id, agendamentoId, tipo, papel, mensagem, enviarEm, status, tentativas, enviadaEm,
				ultimoErro);
	}

	public void marcarEnviada(Instant agora) {
		exigirPendente();
		this.status = StatusNotificacao.ENVIADA;
		this.enviadaEm = obrigatorio(agora, "agora");
	}

	/** Desiste do envio. Só a pendente pode ser cancelada: a que já saiu não volta atrás. */
	public void cancelar() {
		exigirPendente();
		this.status = StatusNotificacao.CANCELADA;
	}

	/** Conta uma tentativa. Continua pendente, para a próxima rodada, até o máximo de tentativas. */
	public void registrarFalha(String erro) {
		exigirPendente();
		this.tentativas++;
		this.ultimoErro = erro;
		if (tentativas >= MAXIMO_DE_TENTATIVAS) {
			this.status = StatusNotificacao.FALHA;
		}
	}

	public UUID id() {
		return id;
	}

	public UUID agendamentoId() {
		return agendamentoId;
	}

	public TipoNotificacao tipo() {
		return tipo;
	}

	public PapelDestinatario papel() {
		return papel;
	}

	public Mensagem mensagem() {
		return mensagem;
	}

	public Instant enviarEm() {
		return enviarEm;
	}

	public StatusNotificacao status() {
		return status;
	}

	public int tentativas() {
		return tentativas;
	}

	public Instant enviadaEm() {
		return enviadaEm;
	}

	public String ultimoErro() {
		return ultimoErro;
	}

	private void exigirPendente() {
		if (status != StatusNotificacao.PENDENTE) {
			throw new RegraNegocioException(SOMENTE_PENDENTE);
		}
	}

}
