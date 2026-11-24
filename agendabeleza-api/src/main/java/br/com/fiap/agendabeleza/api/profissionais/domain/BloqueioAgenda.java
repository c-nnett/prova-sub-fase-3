package br.com.fiap.agendabeleza.api.profissionais.domain;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.obrigatorio;
import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.textoOpcional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

/**
 * Ausência pontual do profissional, como férias, folga ou consulta médica. O período é semiaberto
 * {@code [inicio, fim)} e é descontado dos horários livres.
 */
public class BloqueioAgenda {

	private final UUID id;
	private final UUID profissionalId;
	private final Intervalo periodo;
	private final String motivo;

	private BloqueioAgenda(UUID id, UUID profissionalId, Instant inicio, Instant fim, String motivo) {
		this.id = obrigatorio(id, "id");
		this.profissionalId = obrigatorio(profissionalId, "profissionalId");
		this.periodo = new Intervalo(inicio, fim);
		this.motivo = textoOpcional(motivo, "motivo", 150);
	}

	/** Cria um bloqueio. Recusa um período que já terminou em {@code agora}. */
	public static BloqueioAgenda criar(UUID profissionalId, Instant inicio, Instant fim, String motivo,
			Instant agora) {
		BloqueioAgenda bloqueio = new BloqueioAgenda(UUID.randomUUID(), profissionalId, inicio, fim, motivo);
		obrigatorio(agora, "agora");
		if (!bloqueio.periodo.fim().isAfter(agora)) {
			throw new RegraNegocioException("O bloqueio já terminou");
		}
		return bloqueio;
	}

	/** Recria o bloqueio a partir dos dados gravados. Não recusa bloqueios que já terminaram. */
	public static BloqueioAgenda reconstituir(UUID id, UUID profissionalId, Instant inicio, Instant fim,
			String motivo) {
		return new BloqueioAgenda(id, profissionalId, inicio, fim, motivo);
	}

	public boolean pertenceA(UUID profissionalId) {
		return Objects.equals(this.profissionalId, profissionalId);
	}

	public UUID id() {
		return id;
	}

	public UUID profissionalId() {
		return profissionalId;
	}

	public Intervalo periodo() {
		return periodo;
	}

	public String motivo() {
		return motivo;
	}

}
