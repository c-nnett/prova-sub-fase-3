package br.com.fiap.agendabeleza.api.profissionais.adapters.out.persistence;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.endereco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.fiap.agendabeleza.api.TestcontainersConfiguration;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.profissionais.application.port.EspecialidadeRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Especialidade;
import br.com.fiap.agendabeleza.api.profissionais.domain.PeriodoTrabalho;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;
import br.com.fiap.agendabeleza.api.profissionais.domain.ServicoProfissional;

/**
 * Persistência real no PostgreSQL do Testcontainers, com as migrações do Flyway. Sem {@code @Transactional}:
 * cada chamada ao adaptador faz commit.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProfissionalPersistenciaTest {

	@Autowired
	private EstabelecimentoRepository estabelecimentos;

	@Autowired
	private ServicoRepository servicos;

	@Autowired
	private ProfissionalRepository profissionais;

	@Autowired
	private EspecialidadeRepository especialidades;

	private final PeriodoTrabalho segundaManha = periodo(DayOfWeek.MONDAY, 9, 12);
	private final PeriodoTrabalho segundaTarde = periodo(DayOfWeek.MONDAY, 13, 18);
	private final PeriodoTrabalho sabado = periodo(DayOfWeek.SATURDAY, 8, 14);

	private static PeriodoTrabalho periodo(DayOfWeek dia, int inicio, int fim) {
		return new PeriodoTrabalho(dia, LocalTime.of(inicio, 0), LocalTime.of(fim, 0));
	}

	private Estabelecimento novoEstabelecimento() {
		return estabelecimentos.salvar(Estabelecimento.cadastrar(
				UUID.randomUUID(), "Salão Bela Vista", null, endereco(), null));
	}

	private Servico novoServico(Estabelecimento estabelecimento, String nome) {
		return servicos.salvar(Servico.cadastrar(estabelecimento.id(), nome, null, CategoriaServico.CABELO, 60,
				new BigDecimal("80.00")));
	}

	private Especialidade especialidadeDoCatalogo(String nome) {
		return especialidades.listarTodas().stream()
				.filter(especialidade -> especialidade.nome().equals(nome))
				.findFirst()
				.orElseThrow();
	}

	@Test
	void salvaELeProfissionalComEspecialidadesServicosEJornada() {
		Estabelecimento estabelecimento = novoEstabelecimento();
		Servico corte = novoServico(estabelecimento, "Corte");
		Servico escova = novoServico(estabelecimento, "Escova");
		Especialidade coloracao = especialidadeDoCatalogo("Coloração");
		Especialidade corteEspecialidade = especialidadeDoCatalogo("Corte");
		UUID usuarioId = UUID.randomUUID();
		Profissional original = Profissional.cadastrar(estabelecimento.id(), usuarioId, "Ana Souza",
				"ana@salao.com", "Colorista");
		original.definirEspecialidades(List.of(coloracao.id(), corteEspecialidade.id()));
		original.definirServicos(List.of(
				new ServicoProfissional(corte.id(), new BigDecimal("95.00")),
				new ServicoProfissional(escova.id(), new BigDecimal("50.00"))));
		original.definirJornada(List.of(sabado, segundaTarde, segundaManha));
		profissionais.salvar(original);

		Profissional lido = profissionais.buscarPorId(original.id()).orElseThrow();

		assertThat(lido.estabelecimentoId()).isEqualTo(estabelecimento.id());
		assertThat(lido.usuarioId()).isEqualTo(usuarioId);
		assertThat(lido.nome()).isEqualTo("Ana Souza");
		assertThat(lido.email()).isEqualTo("ana@salao.com");
		assertThat(lido.bio()).isEqualTo("Colorista");
		assertThat(lido.notaMedia()).isNull();
		assertThat(lido.totalAvaliacoes()).isZero();
		assertThat(lido.ativo()).isTrue();
		assertThat(lido.especialidadeIds()).containsExactlyInAnyOrder(coloracao.id(), corteEspecialidade.id());
		assertThat(lido.servicos()).containsExactlyInAnyOrderElementsOf(original.servicos());
		assertThat(lido.jornada()).containsExactly(segundaManha, segundaTarde, sabado);
	}

	@Test
	void substituiEspecialidadesServicosEJornada() {
		Estabelecimento estabelecimento = novoEstabelecimento();
		Servico corte = novoServico(estabelecimento, "Corte");
		Servico escova = novoServico(estabelecimento, "Escova");
		Profissional profissional = Profissional.cadastrar(estabelecimento.id(), null, "Bia", "bia@salao.com", null);
		profissional.definirEspecialidades(List.of(especialidadeDoCatalogo("Corte").id()));
		profissional.definirServicos(List.of(new ServicoProfissional(corte.id(), BigDecimal.TEN)));
		profissional.definirJornada(List.of(segundaManha, segundaTarde));
		profissionais.salvar(profissional);

		Profissional lido = profissionais.buscarPorId(profissional.id()).orElseThrow();
		Especialidade manicure = especialidadeDoCatalogo("Manicure");
		lido.atualizarPerfil("Bia Lima", "bia.lima@salao.com", "Manicure", false);
		lido.definirEspecialidades(List.of(manicure.id()));
		lido.definirServicos(List.of(
				new ServicoProfissional(corte.id(), new BigDecimal("12.50")),
				new ServicoProfissional(escova.id(), BigDecimal.ONE)));
		lido.definirJornada(List.of(sabado));
		profissionais.salvar(lido);

		Profissional atualizado = profissionais.buscarPorId(profissional.id()).orElseThrow();
		assertThat(atualizado.nome()).isEqualTo("Bia Lima");
		assertThat(atualizado.ativo()).isFalse();
		assertThat(atualizado.especialidadeIds()).containsExactly(manicure.id());
		assertThat(atualizado.tarifaDe(corte.id())).contains(new BigDecimal("12.50"));
		assertThat(atualizado.tarifaDe(escova.id())).contains(new BigDecimal("1.00"));
		assertThat(atualizado.jornada()).containsExactly(sabado);
	}

	@Test
	void listaPorEstabelecimentoOrdenadoPorNome() {
		Estabelecimento estabelecimento = novoEstabelecimento();
		Estabelecimento outro = novoEstabelecimento();
		Profissional carla = profissionais.salvar(
				Profissional.cadastrar(estabelecimento.id(), null, "Carla", "carla@salao.com", null));
		Profissional ana = profissionais.salvar(
				Profissional.cadastrar(estabelecimento.id(), null, "Ana", "ana@salao.com", null));
		profissionais.salvar(Profissional.cadastrar(outro.id(), null, "Bruno", "bruno@salao.com", null));

		assertThat(profissionais.listarPorEstabelecimento(estabelecimento.id()))
				.extracting(Profissional::id)
				.containsExactly(ana.id(), carla.id());
		assertThat(profissionais.buscarPorId(UUID.randomUUID())).isEmpty();
	}

	@Test
	void catalogoDeEspecialidadesVemDaMigracao() {
		Especialidade manicure = especialidadeDoCatalogo("Manicure");

		assertThat(especialidades.listarTodas())
				.extracting(Especialidade::nome)
				.contains("Corte", "Coloração", "Manicure", "Barba");
		assertThat(especialidades.buscarPorIds(List.of(manicure.id(), UUID.randomUUID())))
				.extracting(Especialidade::id)
				.containsExactly(manicure.id());
	}

	@Test
	void recusaUsuarioDeOutroProfissional() {
		Estabelecimento estabelecimento = novoEstabelecimento();
		UUID usuarioId = UUID.randomUUID();
		profissionais.salvar(Profissional.cadastrar(estabelecimento.id(), usuarioId, "Ana", "ana@salao.com", null));
		Profissional repetido = Profissional.cadastrar(estabelecimento.id(), usuarioId, "Outra", "outra@salao.com", null);

		assertThatThrownBy(() -> profissionais.salvar(repetido)).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void recusaServicoInexistente() {
		Estabelecimento estabelecimento = novoEstabelecimento();
		Profissional profissional = Profissional.cadastrar(estabelecimento.id(), null, "Ana", "ana@salao.com", null);
		profissional.definirServicos(List.of(new ServicoProfissional(UUID.randomUUID(), BigDecimal.TEN)));

		assertThatThrownBy(() -> profissionais.salvar(profissional))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

}
