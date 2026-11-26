package br.com.fiap.agendabeleza.api.notificacoes.adapters.out.persistence;

import java.time.Instant;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.notificacoes.domain.PapelDestinatario;
import br.com.fiap.agendabeleza.api.notificacoes.domain.StatusNotificacao;
import br.com.fiap.agendabeleza.api.notificacoes.domain.TipoNotificacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Linha da outbox. Só o resultado do envio muda depois da gravação; o conteúdo do e-mail é fixo. */
@Entity
@Table(name = "notificacao")
@Getter
@Setter
@NoArgsConstructor
class NotificacaoJpaEntity {

	@Id
	private UUID id;

	@Column(name = "agendamento_id", nullable = false, updatable = false)
	private UUID agendamentoId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20, updatable = false)
	private TipoNotificacao tipo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20, updatable = false)
	private PapelDestinatario papel;

	@Column(name = "destinatario_email", nullable = false, length = 180, updatable = false)
	private String destinatarioEmail;

	@Column(nullable = false, length = 200, updatable = false)
	private String assunto;

	@Column(nullable = false, updatable = false, columnDefinition = "text")
	private String corpo;

	@Column(name = "enviar_em", nullable = false, updatable = false)
	private Instant enviarEm;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusNotificacao status;

	@Column(nullable = false)
	private int tentativas;

	@Column(name = "enviada_em")
	private Instant enviadaEm;

	@Column(name = "ultimo_erro", columnDefinition = "text")
	private String ultimoErro;

}
