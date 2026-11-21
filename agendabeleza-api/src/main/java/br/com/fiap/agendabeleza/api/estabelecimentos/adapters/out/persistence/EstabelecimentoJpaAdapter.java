package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.out.persistence;

import java.time.DayOfWeek;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Endereco;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.FotoEstabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.HorarioFuncionamento;

/**
 * Implementa {@link EstabelecimentoRepository} com JPA. A cada gravação, monta um grafo novo a partir do agregado:
 * o {@code merge} atualiza as linhas existentes, e o {@code orphanRemoval} apaga fotos e horários retirados.
 */
@Component
class EstabelecimentoJpaAdapter implements EstabelecimentoRepository {

	private final EstabelecimentoJpaRepository repositorio;

	EstabelecimentoJpaAdapter(EstabelecimentoJpaRepository repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	@Transactional
	public Estabelecimento salvar(Estabelecimento estabelecimento) {
		repositorio.save(paraEntidade(estabelecimento));
		return estabelecimento;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Estabelecimento> buscarPorId(UUID id) {
		return repositorio.findById(id).map(EstabelecimentoJpaAdapter::paraDominio);
	}

	private static EstabelecimentoJpaEntity paraEntidade(Estabelecimento origem) {
		EstabelecimentoJpaEntity entidade = new EstabelecimentoJpaEntity();
		entidade.setId(origem.id());
		entidade.setGestorId(origem.gestorId());
		entidade.setNome(origem.nome());
		entidade.setDescricao(origem.descricao());
		Endereco endereco = origem.endereco();
		entidade.setLogradouro(endereco.logradouro());
		entidade.setNumero(endereco.numero());
		entidade.setComplemento(endereco.complemento());
		entidade.setBairro(endereco.bairro());
		entidade.setCidade(endereco.cidade());
		entidade.setUf(endereco.uf());
		entidade.setCep(endereco.cep());
		entidade.setTimezone(origem.timezone().getId());
		entidade.setAtivo(origem.ativo());

		for (FotoEstabelecimento foto : origem.fotos()) {
			FotoEstabelecimentoJpaEntity fotoEntidade = new FotoEstabelecimentoJpaEntity();
			fotoEntidade.setId(foto.id());
			fotoEntidade.setEstabelecimento(entidade);
			fotoEntidade.setUrl(foto.url());
			fotoEntidade.setOrdem(foto.ordem());
			entidade.getFotos().add(fotoEntidade);
		}
		for (HorarioFuncionamento horario : origem.horarios()) {
			HorarioFuncionamentoJpaEntity horarioEntidade = new HorarioFuncionamentoJpaEntity();
			// O horário não tem identidade no domínio: a lista é sempre substituída inteira.
			horarioEntidade.setId(UUID.randomUUID());
			horarioEntidade.setEstabelecimento(entidade);
			horarioEntidade.setDiaSemana((short) horario.diaSemana().getValue());
			horarioEntidade.setAbreAs(horario.abreAs());
			horarioEntidade.setFechaAs(horario.fechaAs());
			entidade.getHorarios().add(horarioEntidade);
		}
		return entidade;
	}

	private static Estabelecimento paraDominio(EstabelecimentoJpaEntity entidade) {
		Endereco endereco = new Endereco(entidade.getLogradouro(), entidade.getNumero(), entidade.getComplemento(),
				entidade.getBairro(), entidade.getCidade(), entidade.getUf(), entidade.getCep());
		return Estabelecimento.reconstituir(
				entidade.getId(),
				entidade.getGestorId(),
				entidade.getNome(),
				entidade.getDescricao(),
				endereco,
				entidade.getTimezone(),
				entidade.isAtivo(),
				entidade.getFotos().stream()
						.map(foto -> new FotoEstabelecimento(foto.getId(), foto.getUrl(), foto.getOrdem()))
						.toList(),
				entidade.getHorarios().stream()
						.map(horario -> new HorarioFuncionamento(DayOfWeek.of(horario.getDiaSemana()),
								horario.getAbreAs(), horario.getFechaAs()))
						.toList());
	}

}
