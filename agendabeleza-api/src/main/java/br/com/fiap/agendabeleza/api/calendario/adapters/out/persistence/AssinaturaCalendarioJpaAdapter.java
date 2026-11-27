package br.com.fiap.agendabeleza.api.calendario.adapters.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.agendabeleza.api.calendario.application.port.AssinaturaCalendarioRepository;
import br.com.fiap.agendabeleza.api.calendario.domain.AssinaturaDoCalendario;

/**
 * Implementa {@link AssinaturaCalendarioRepository}. A troca do token apaga a assinatura anterior do titular e
 * grava a nova na mesma transação: não existe instante com duas URLs válidas nem com nenhuma. Duas emissões
 * simultâneas do mesmo titular esbarram na restrição única, e a segunda recebe 409.
 */
@Component
class AssinaturaCalendarioJpaAdapter implements AssinaturaCalendarioRepository {

	private final AssinaturaCalendarioJpaRepository repositorio;

	AssinaturaCalendarioJpaAdapter(AssinaturaCalendarioJpaRepository repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	@Transactional
	public void salvar(AssinaturaDoCalendario assinatura) {
		repositorio.apagarDoTitular(assinatura.papel(), assinatura.titularId());
		repositorio.save(paraEntidade(assinatura));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AssinaturaDoCalendario> buscarPorTokenHash(String tokenHash) {
		return repositorio.findByTokenHash(tokenHash).map(AssinaturaCalendarioJpaAdapter::paraDominio);
	}

	private static AssinaturaCalendarioJpaEntity paraEntidade(AssinaturaDoCalendario assinatura) {
		AssinaturaCalendarioJpaEntity entidade = new AssinaturaCalendarioJpaEntity();
		entidade.setId(assinatura.id());
		entidade.setPapel(assinatura.papel());
		entidade.setTitularId(assinatura.titularId());
		entidade.setTokenHash(assinatura.tokenHash());
		entidade.setCriadaEm(assinatura.criadaEm());
		return entidade;
	}

	private static AssinaturaDoCalendario paraDominio(AssinaturaCalendarioJpaEntity entidade) {
		return AssinaturaDoCalendario.reconstituir(entidade.getId(), entidade.getPapel(), entidade.getTitularId(),
				entidade.getTokenHash(), entidade.getCriadaEm());
	}

}
