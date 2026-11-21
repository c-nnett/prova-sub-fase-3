package br.com.fiap.agendabeleza.api.profissionais.domain;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.obrigatorio;
import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.textoObrigatorio;
import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.textoOpcional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

/**
 * Agregado do profissional: perfil, especialidades, serviços com tarifa e jornada semanal. Pertence a um único
 * estabelecimento e pode ter login próprio ({@code usuarioId}).
 */
public class Profissional {

	private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
	private static final Comparator<PeriodoTrabalho> ORDEM_JORNADA = Comparator
			.comparing(PeriodoTrabalho::diaSemana)
			.thenComparing(PeriodoTrabalho::inicio);

	private final UUID id;
	private final UUID estabelecimentoId;
	private final UUID usuarioId;
	private String nome;
	private String email;
	private String bio;
	private BigDecimal notaMedia;
	private int totalAvaliacoes;
	private boolean ativo;
	private final Set<UUID> especialidadeIds = new LinkedHashSet<>();
	private final Map<UUID, ServicoProfissional> servicos = new LinkedHashMap<>();
	private final List<PeriodoTrabalho> jornada = new ArrayList<>();

	private Profissional(UUID id, UUID estabelecimentoId, UUID usuarioId) {
		this.id = obrigatorio(id, "id");
		this.estabelecimentoId = obrigatorio(estabelecimentoId, "estabelecimentoId");
		this.usuarioId = usuarioId;
	}

	public static Profissional cadastrar(UUID estabelecimentoId, UUID usuarioId, String nome, String email,
			String bio) {
		Profissional profissional = new Profissional(UUID.randomUUID(), estabelecimentoId, usuarioId);
		profissional.atualizarPerfil(nome, email, bio, true);
		return profissional;
	}

	/** Recria o agregado a partir dos dados gravados, aplicando as mesmas validações do cadastro. */
	public static Profissional reconstituir(UUID id, UUID estabelecimentoId, UUID usuarioId, String nome,
			String email, String bio, BigDecimal notaMedia, int totalAvaliacoes, boolean ativo,
			Collection<UUID> especialidadeIds, Collection<ServicoProfissional> servicos,
			Collection<PeriodoTrabalho> jornada) {
		Profissional profissional = new Profissional(id, estabelecimentoId, usuarioId);
		profissional.atualizarPerfil(nome, email, bio, ativo);
		profissional.notaMedia = notaMedia;
		profissional.totalAvaliacoes = totalAvaliacoes;
		profissional.definirEspecialidades(especialidadeIds);
		profissional.definirServicos(servicos);
		profissional.definirJornada(jornada);
		return profissional;
	}

	public void atualizarPerfil(String nome, String email, String bio, boolean ativo) {
		String novoNome = textoObrigatorio(nome, "nome", 120);
		String novoEmail = validarEmail(email);
		String novaBio = textoOpcional(bio, "bio", 2000);

		this.nome = novoNome;
		this.email = novoEmail;
		this.bio = novaBio;
		this.ativo = ativo;
	}

	/** Permite a alteração pelo gestor do estabelecimento ou pelo próprio profissional. */
	public void garantirPodeAlterar(UUID usuarioSolicitante, UUID gestorId) {
		boolean gestor = usuarioSolicitante != null && usuarioSolicitante.equals(gestorId);
		boolean proprio = usuarioSolicitante != null && usuarioSolicitante.equals(usuarioId);
		if (!gestor && !proprio) {
			throw new AcessoNegadoException(
					"Somente o gestor do estabelecimento ou o próprio profissional pode alterá-lo");
		}
	}

	/** Substitui as especialidades, ignorando repetidas. Lista nula ou vazia remove todas. */
	public void definirEspecialidades(Collection<UUID> novasEspecialidades) {
		Set<UUID> ids = new LinkedHashSet<>();
		if (novasEspecialidades != null) {
			novasEspecialidades.forEach(especialidadeId -> ids.add(obrigatorio(especialidadeId, "especialidadeId")));
		}
		especialidadeIds.clear();
		especialidadeIds.addAll(ids);
	}

	/** Substitui os serviços e tarifas. Lista nula ou vazia remove todos. */
	public void definirServicos(Collection<ServicoProfissional> novosServicos) {
		Map<UUID, ServicoProfissional> porServico = new LinkedHashMap<>();
		if (novosServicos != null) {
			for (ServicoProfissional servico : novosServicos) {
				if (porServico.putIfAbsent(servico.servicoId(), servico) != null) {
					throw new RegraNegocioException("Serviço " + servico.servicoId() + " repetido");
				}
			}
		}
		servicos.clear();
		servicos.putAll(porServico);
	}

	/** Substitui a jornada semanal. Recusa períodos sobrepostos no mesmo dia. Lista nula ou vazia remove todos. */
	public void definirJornada(Collection<PeriodoTrabalho> novaJornada) {
		List<PeriodoTrabalho> periodos = novaJornada == null
				? new ArrayList<>()
				: new ArrayList<>(novaJornada.stream().map(periodo -> obrigatorio(periodo, "periodo")).toList());
		periodos.sort(ORDEM_JORNADA);
		for (int i = 1; i < periodos.size(); i++) {
			PeriodoTrabalho anterior = periodos.get(i - 1);
			if (anterior.sobrepoe(periodos.get(i))) {
				throw new RegraNegocioException("Períodos de trabalho sobrepostos em " + anterior.diaSemana());
			}
		}
		jornada.clear();
		jornada.addAll(periodos);
	}

	public boolean executa(UUID servicoId) {
		return servicos.containsKey(servicoId);
	}

	public Optional<BigDecimal> tarifaDe(UUID servicoId) {
		return Optional.ofNullable(servicos.get(servicoId)).map(ServicoProfissional::tarifa);
	}

	/** Períodos do dia, ordenados pelo início. */
	public List<PeriodoTrabalho> jornadaEm(DayOfWeek dia) {
		return jornada.stream().filter(periodo -> periodo.diaSemana() == dia).toList();
	}

	public boolean pertenceA(UUID estabelecimentoId) {
		return Objects.equals(this.estabelecimentoId, estabelecimentoId);
	}

	public UUID id() {
		return id;
	}

	public UUID estabelecimentoId() {
		return estabelecimentoId;
	}

	public UUID usuarioId() {
		return usuarioId;
	}

	public String nome() {
		return nome;
	}

	public String email() {
		return email;
	}

	public String bio() {
		return bio;
	}

	public BigDecimal notaMedia() {
		return notaMedia;
	}

	public int totalAvaliacoes() {
		return totalAvaliacoes;
	}

	public boolean ativo() {
		return ativo;
	}

	public Set<UUID> especialidadeIds() {
		return Collections.unmodifiableSet(especialidadeIds);
	}

	public List<ServicoProfissional> servicos() {
		return List.copyOf(servicos.values());
	}

	/** Jornada ordenada de segunda a domingo e, em cada dia, pelo início. */
	public List<PeriodoTrabalho> jornada() {
		return Collections.unmodifiableList(jornada);
	}

	private static String validarEmail(String email) {
		String texto = textoObrigatorio(email, "email", 180);
		if (!EMAIL.matcher(texto).matches()) {
			throw new RegraNegocioException("email inválido");
		}
		return texto;
	}

}
