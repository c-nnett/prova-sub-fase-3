package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;

/**
 * Quem pode consultar ou alterar um agendamento (Strategy). Cada caso de uso escolhe a regra da sua ação, conforme a seção
 * 5.3 da arquitetura.
 */
enum PermissaoNoAgendamento {

	/** Consulta dos detalhes. */
	PARTES_ENVOLVIDAS("o cliente, o gestor do estabelecimento ou o profissional") {
		@Override
		boolean permite(PartesDoAgendamento partes, UUID usuario) {
			return partes.ehCliente(usuario) || partes.ehGestor(usuario) || partes.ehProfissional(usuario);
		}
	},

	/** Confirmar, concluir e registrar o não comparecimento. */
	GESTOR_OU_PROFISSIONAL("o gestor do estabelecimento ou o profissional") {
		@Override
		boolean permite(PartesDoAgendamento partes, UUID usuario) {
			return partes.ehGestor(usuario) || partes.ehProfissional(usuario);
		}
	},

	/** Cancelar e reagendar. */
	CLIENTE_OU_GESTOR("o cliente ou o gestor do estabelecimento") {
		@Override
		boolean permite(PartesDoAgendamento partes, UUID usuario) {
			return partes.ehCliente(usuario) || partes.ehGestor(usuario);
		}
	};

	private final String quemPode;

	PermissaoNoAgendamento(String quemPode) {
		this.quemPode = quemPode;
	}

	abstract boolean permite(PartesDoAgendamento partes, UUID usuario);

	/**
	 * @param acao verbo da operação, completando a frase "Somente ... podem ... este agendamento"
	 * @throws AcessoNegadoException se o usuário não tiver a permissão
	 */
	void exigir(PartesDoAgendamento partes, UUID usuario, String acao) {
		if (!permite(partes, usuario)) {
			throw new AcessoNegadoException("Somente " + quemPode + " podem " + acao + " este agendamento");
		}
	}

}
