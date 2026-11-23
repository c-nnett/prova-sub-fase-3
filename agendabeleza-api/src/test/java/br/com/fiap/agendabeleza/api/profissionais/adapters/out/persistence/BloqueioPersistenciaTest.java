package br.com.fiap.agendabeleza.api.profissionais.adapters.out.persistence;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.endereco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.fiap.agendabeleza.api.TestcontainersConfiguration;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.profissionais.application.port.BloqueioAgendaRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.BloqueioAgenda;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Persistência real dos bloqueios no PostgreSQL do Testcontainers. Cada teste usa um profissional novo, porque
 * não há rollback entre os testes.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BloqueioPersistenciaTest {

	private static final Instant DIA = Instant.parse("2026-11-30T03:00:00Z");

	@Autowired
	private EstabelecimentoRepository estabelecimentos;

	@Autowired
	private ProfissionalRepository profissionais;

	@Autowired
	private BloqueioAgendaRepository bloqueios;

	private static Instant hora(int horas) {
		return DIA.plusSeconds(horas * 3600L);
	}

	private Profissional novoProfissional() {
		Estabelecimento estabelecimento = estabelecimentos.salvar(Estabelecimento.cadastrar(
				UUID.randomUUID(), "Salão Bela Vista", null, endereco(), null));
		return profissionais.salvar(
				Profissional.cadastrar(estabelecimento.id(), null, "Ana", "ana@salao.com", null));
	}

	private BloqueioAgenda bloquear(Profissional profissional, int inicio, int fim, String motivo) {
		return bloqueios.salvar(BloqueioAgenda.reconstituir(UUID.randomUUID(), profissional.id(), hora(inicio),
				hora(fim), motivo));
	}

	@Test
	void salvaELeBloqueio() {
		Profissional ana = novoProfissional();
		BloqueioAgenda original = bloquear(ana, 10, 11, "Consulta médica");

		BloqueioAgenda lido = bloqueios.buscarPorId(original.id()).orElseThrow();

		assertThat(lido.profissionalId()).isEqualTo(ana.id());
		assertThat(lido.periodo()).isEqualTo(original.periodo());
		assertThat(lido.motivo()).isEqualTo("Consulta médica");
		assertThat(bloqueios.buscarPorId(UUID.randomUUID())).isEmpty();
	}

	@Test
	void listaNoPeriodoSomenteOsQueSeSobrepoemEmOrdem() {
		Profissional ana = novoProfissional();
		Profissional bia = novoProfissional();
		BloqueioAgenda tarde = bloquear(ana, 14, 15, null);
		BloqueioAgenda ferias = bloquear(ana, -72, 10, "Férias");
		bloquear(ana, 6, 9, "Encosta no início");
		bloquear(ana, 18, 20, "Encosta no fim");
		bloquear(bia, 9, 18, "Outro profissional");

		assertThat(bloqueios.listarNoPeriodo(ana.id(), new Intervalo(hora(9), hora(18))))
				.extracting(BloqueioAgenda::id)
				.containsExactly(ferias.id(), tarde.id());
	}

	@Test
	void listaVigentesOsQueTerminamDepoisDoInstante() {
		Profissional ana = novoProfissional();
		bloquear(ana, 1, 2, "Encerrado");
		bloquear(ana, 2, 3, "Termina no instante");
		BloqueioAgenda emAndamento = bloquear(ana, 2, 5, null);
		BloqueioAgenda futuro = bloquear(ana, 8, 9, null);

		assertThat(bloqueios.listarVigentes(ana.id(), hora(3)))
				.extracting(BloqueioAgenda::id)
				.containsExactly(emAndamento.id(), futuro.id());
	}

	@Test
	void removeBloqueio() {
		Profissional ana = novoProfissional();
		BloqueioAgenda bloqueio = bloquear(ana, 10, 11, null);

		bloqueios.remover(bloqueio.id());

		assertThat(bloqueios.buscarPorId(bloqueio.id())).isEmpty();
	}

	@Test
	void recusaProfissionalInexistente() {
		BloqueioAgenda orfao = BloqueioAgenda.reconstituir(UUID.randomUUID(), UUID.randomUUID(), hora(10), hora(11),
				null);

		assertThatThrownBy(() -> bloqueios.salvar(orfao)).isInstanceOf(DataIntegrityViolationException.class);
	}

}
