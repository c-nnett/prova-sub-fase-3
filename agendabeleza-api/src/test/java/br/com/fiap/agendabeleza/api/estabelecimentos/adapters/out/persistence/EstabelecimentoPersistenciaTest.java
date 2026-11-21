package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.out.persistence;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.endereco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
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
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.FotoEstabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.HorarioFuncionamento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;

/**
 * Persistência real no PostgreSQL do Testcontainers, com a migração do Flyway. Sem {@code @Transactional}:
 * cada chamada ao adaptador faz commit, para as constraints adiadas serem verificadas de verdade.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EstabelecimentoPersistenciaTest {

	@Autowired
	private EstabelecimentoRepository estabelecimentos;

	@Autowired
	private ServicoRepository servicos;

	private final HorarioFuncionamento segunda = new HorarioFuncionamento(
			DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0));
	private final HorarioFuncionamento sabado = new HorarioFuncionamento(
			DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(14, 0));

	private Estabelecimento novoEstabelecimento() {
		Estabelecimento estabelecimento = Estabelecimento.cadastrar(
				UUID.randomUUID(), "Salão Bela Vista", "Cabelo e unhas", endereco(), "America/Sao_Paulo");
		estabelecimento.definirHorarios(List.of(segunda, sabado));
		estabelecimento.adicionarFoto("https://cdn.exemplo.com/1.jpg");
		estabelecimento.adicionarFoto("https://cdn.exemplo.com/2.jpg");
		estabelecimento.adicionarFoto("https://cdn.exemplo.com/3.jpg");
		return estabelecimento;
	}

	@Test
	void salvaELeEstabelecimentoComFotosEHorarios() {
		Estabelecimento original = estabelecimentos.salvar(novoEstabelecimento());

		Estabelecimento lido = estabelecimentos.buscarPorId(original.id()).orElseThrow();

		assertThat(lido.gestorId()).isEqualTo(original.gestorId());
		assertThat(lido.nome()).isEqualTo("Salão Bela Vista");
		assertThat(lido.descricao()).isEqualTo("Cabelo e unhas");
		assertThat(lido.endereco()).isEqualTo(endereco());
		assertThat(lido.timezone()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
		assertThat(lido.ativo()).isTrue();
		assertThat(lido.fotos()).containsExactlyElementsOf(original.fotos());
		assertThat(lido.horarios()).containsExactly(segunda, sabado);
	}

	@Test
	void atualizaFotosReordenadasEHorariosSubstituidos() {
		Estabelecimento estabelecimento = estabelecimentos.salvar(novoEstabelecimento());
		FotoEstabelecimento primeira = estabelecimento.fotos().get(0);
		HorarioFuncionamento segundaEstendida = new HorarioFuncionamento(
				DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(20, 0));

		Estabelecimento lido = estabelecimentos.buscarPorId(estabelecimento.id()).orElseThrow();
		lido.removerFoto(primeira.id());
		lido.definirHorarios(List.of(segundaEstendida));
		lido.atualizarDados("Studio Paulista", null, endereco(), null);
		estabelecimentos.salvar(lido);

		Estabelecimento atualizado = estabelecimentos.buscarPorId(estabelecimento.id()).orElseThrow();
		assertThat(atualizado.nome()).isEqualTo("Studio Paulista");
		assertThat(atualizado.descricao()).isNull();
		assertThat(atualizado.fotos())
				.extracting(FotoEstabelecimento::url, FotoEstabelecimento::ordem)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("https://cdn.exemplo.com/2.jpg", 1),
						org.assertj.core.groups.Tuple.tuple("https://cdn.exemplo.com/3.jpg", 2));
		assertThat(atualizado.horarios()).containsExactly(segundaEstendida);
	}

	@Test
	void buscaDeIdInexistenteDevolveVazio() {
		assertThat(estabelecimentos.buscarPorId(UUID.randomUUID())).isEmpty();
	}

	@Test
	void salvaAtualizaEListaServicosDoEstabelecimento() {
		Estabelecimento estabelecimento = estabelecimentos.salvar(novoEstabelecimento());
		Estabelecimento outro = estabelecimentos.salvar(novoEstabelecimento());
		Servico manicure = servicos.salvar(Servico.cadastrar(estabelecimento.id(), "Manicure", null,
				CategoriaServico.UNHAS, 45, new BigDecimal("35.00")));
		Servico corte = servicos.salvar(Servico.cadastrar(estabelecimento.id(), "Corte", "Lavagem inclusa",
				CategoriaServico.CABELO, 60, new BigDecimal("80")));
		servicos.salvar(Servico.cadastrar(outro.id(), "Barba", null, CategoriaServico.BARBA, 30, BigDecimal.TEN));

		Servico lido = servicos.buscarPorId(corte.id()).orElseThrow();
		lido.atualizar("Corte e escova", null, CategoriaServico.CABELO, 90, new BigDecimal("120.50"), false);
		servicos.salvar(lido);

		Servico atualizado = servicos.buscarPorId(corte.id()).orElseThrow();
		assertThat(atualizado.nome()).isEqualTo("Corte e escova");
		assertThat(atualizado.descricao()).isNull();
		assertThat(atualizado.duracaoMinutos()).isEqualTo(90);
		assertThat(atualizado.precoBase()).isEqualTo(new BigDecimal("120.50"));
		assertThat(atualizado.ativo()).isFalse();
		assertThat(servicos.listarPorEstabelecimento(estabelecimento.id()))
				.extracting(Servico::id)
				.containsExactly(corte.id(), manicure.id());
		assertThat(servicos.buscarPorId(UUID.randomUUID())).isEmpty();
	}

	@Test
	void recusaServicoDeEstabelecimentoInexistente() {
		Servico orfao = Servico.cadastrar(UUID.randomUUID(), "Corte", null, CategoriaServico.CABELO, 30, BigDecimal.TEN);

		assertThatThrownBy(() -> servicos.salvar(orfao)).isInstanceOf(DataIntegrityViolationException.class);
	}

}
