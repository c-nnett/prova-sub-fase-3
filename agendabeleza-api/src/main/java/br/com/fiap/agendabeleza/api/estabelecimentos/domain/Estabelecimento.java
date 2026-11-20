package br.com.fiap.agendabeleza.api.estabelecimentos.domain;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.obrigatorio;
import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.textoObrigatorio;
import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.textoOpcional;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

/**
 * Agregado do estabelecimento: dados cadastrais, fotos e horários de funcionamento.
 * Os serviços oferecidos são um agregado à parte ({@link Servico}).
 */
public class Estabelecimento {

	public static final int MAXIMO_FOTOS = 10;
	public static final ZoneId TIMEZONE_PADRAO = ZoneId.of("America/Sao_Paulo");

	private final UUID id;
	private final UUID gestorId;
	private String nome;
	private String descricao;
	private Endereco endereco;
	private ZoneId timezone;
	private boolean ativo;
	private final List<FotoEstabelecimento> fotos = new ArrayList<>();
	private final Map<DayOfWeek, HorarioFuncionamento> horarios = new EnumMap<>(DayOfWeek.class);

	private Estabelecimento(UUID id, UUID gestorId) {
		this.id = obrigatorio(id, "id");
		this.gestorId = obrigatorio(gestorId, "gestorId");
		this.ativo = true;
	}

	public static Estabelecimento cadastrar(UUID gestorId, String nome, String descricao, Endereco endereco,
			String timezone) {
		Estabelecimento estabelecimento = new Estabelecimento(UUID.randomUUID(), gestorId);
		estabelecimento.atualizarDados(nome, descricao, endereco, timezone);
		return estabelecimento;
	}

	/** Recria o agregado a partir dos dados gravados, aplicando as mesmas validações do cadastro. */
	public static Estabelecimento reconstituir(UUID id, UUID gestorId, String nome, String descricao,
			Endereco endereco, String timezone, boolean ativo, Collection<FotoEstabelecimento> fotos,
			Collection<HorarioFuncionamento> horarios) {
		Estabelecimento estabelecimento = new Estabelecimento(id, gestorId);
		estabelecimento.atualizarDados(nome, descricao, endereco, timezone);
		estabelecimento.ativo = ativo;
		estabelecimento.definirHorarios(horarios);
		if (fotos != null) {
			fotos.stream()
					.sorted(Comparator.comparingInt(FotoEstabelecimento::ordem))
					.forEach(estabelecimento.fotos::add);
		}
		return estabelecimento;
	}

	/** Atualiza os dados cadastrais. Fuso nulo ou vazio volta para {@link #TIMEZONE_PADRAO}. */
	public void atualizarDados(String nome, String descricao, Endereco endereco, String timezone) {
		String novoNome = textoObrigatorio(nome, "nome", 150);
		String novaDescricao = textoOpcional(descricao, "descricao", 2000);
		Endereco novoEndereco = obrigatorio(endereco, "endereco");
		ZoneId novoTimezone = converterTimezone(timezone);

		this.nome = novoNome;
		this.descricao = novaDescricao;
		this.endereco = novoEndereco;
		this.timezone = novoTimezone;
	}

	public void garantirGestor(UUID usuarioId) {
		if (!gestorId.equals(usuarioId)) {
			throw new AcessoNegadoException("Somente o gestor do estabelecimento pode alterá-lo");
		}
	}

	/** Substitui todos os horários. Lista nula ou vazia deixa o estabelecimento sem dias de funcionamento. */
	public void definirHorarios(Collection<HorarioFuncionamento> novosHorarios) {
		Map<DayOfWeek, HorarioFuncionamento> porDia = new EnumMap<>(DayOfWeek.class);
		if (novosHorarios != null) {
			for (HorarioFuncionamento horario : novosHorarios) {
				if (porDia.putIfAbsent(horario.diaSemana(), horario) != null) {
					throw new RegraNegocioException("Horário de funcionamento repetido para " + horario.diaSemana());
				}
			}
		}
		horarios.clear();
		horarios.putAll(porDia);
	}

	public FotoEstabelecimento adicionarFoto(String url) {
		String urlFoto = textoObrigatorio(url, "url da foto", 500);
		if (!urlFoto.startsWith("http://") && !urlFoto.startsWith("https://")) {
			throw new RegraNegocioException("url da foto deve começar com http:// ou https://");
		}
		if (fotos.size() >= MAXIMO_FOTOS) {
			throw new RegraNegocioException("O estabelecimento pode ter no máximo " + MAXIMO_FOTOS + " fotos");
		}
		FotoEstabelecimento foto = new FotoEstabelecimento(UUID.randomUUID(), urlFoto, fotos.size() + 1);
		fotos.add(foto);
		return foto;
	}

	/** Remove a foto e reordena as restantes a partir de 1. */
	public void removerFoto(UUID fotoId) {
		boolean removida = fotos.removeIf(foto -> foto.id().equals(fotoId));
		if (!removida) {
			throw new RecursoNaoEncontradoException("Foto " + fotoId + " não encontrada");
		}
		for (int i = 0; i < fotos.size(); i++) {
			fotos.set(i, fotos.get(i).comOrdem(i + 1));
		}
	}

	public Optional<HorarioFuncionamento> horarioEm(DayOfWeek dia) {
		return Optional.ofNullable(horarios.get(dia));
	}

	public UUID id() {
		return id;
	}

	public UUID gestorId() {
		return gestorId;
	}

	public String nome() {
		return nome;
	}

	public String descricao() {
		return descricao;
	}

	public Endereco endereco() {
		return endereco;
	}

	public ZoneId timezone() {
		return timezone;
	}

	public boolean ativo() {
		return ativo;
	}

	public List<FotoEstabelecimento> fotos() {
		return Collections.unmodifiableList(fotos);
	}

	/** Horários ordenados de segunda a domingo. */
	public List<HorarioFuncionamento> horarios() {
		return List.copyOf(horarios.values());
	}

	private static ZoneId converterTimezone(String timezone) {
		if (timezone == null || timezone.isBlank()) {
			return TIMEZONE_PADRAO;
		}
		try {
			return ZoneId.of(timezone.strip());
		} catch (DateTimeException e) {
			throw new RegraNegocioException("timezone inválido: " + timezone);
		}
	}

}
