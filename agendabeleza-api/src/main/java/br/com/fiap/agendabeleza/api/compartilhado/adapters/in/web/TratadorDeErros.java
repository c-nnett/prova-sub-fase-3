package br.com.fiap.agendabeleza.api.compartilhado.adapters.in.web;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.ConflitoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

/**
 * Traduz as exceções para {@code application/problem+json} (RFC 9457). A classe base já cobre os erros do Spring
 * MVC, como corpo ilegível, UUID malformado e cabeçalho obrigatório ausente.
 */
@RestControllerAdvice
class TratadorDeErros extends ResponseEntityExceptionHandler {

	@ExceptionHandler(RegraNegocioException.class)
	ProblemDetail regraNegocio(RegraNegocioException excecao) {
		return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Regra de negócio violada", excecao.getMessage());
	}

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	ProblemDetail naoEncontrado(RecursoNaoEncontradoException excecao) {
		return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", excecao.getMessage());
	}

	@ExceptionHandler(AcessoNegadoException.class)
	ProblemDetail acessoNegado(AcessoNegadoException excecao) {
		return problema(HttpStatus.FORBIDDEN, "Acesso negado", excecao.getMessage());
	}

	/** Conflito reconhecido pelo domínio, como um horário já ocupado. */
	@ExceptionHandler(ConflitoException.class)
	ProblemDetail conflitoDeNegocio(ConflitoException excecao) {
		return problema(HttpStatus.CONFLICT, "Conflito", excecao.getMessage());
	}

	/** Restrição do banco violada, como um {@code usuarioId} já vinculado a outro profissional. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail conflito(DataIntegrityViolationException excecao) {
		return problema(HttpStatus.CONFLICT, "Conflito",
				"A operação conflita com dados já cadastrados ou referencia um registro inexistente");
	}

	/** Bloqueio otimista: outra operação gravou o mesmo registro depois que ele foi lido. */
	@ExceptionHandler(OptimisticLockingFailureException.class)
	ProblemDetail versaoDesatualizada(OptimisticLockingFailureException excecao) {
		return problema(HttpStatus.CONFLICT, "Conflito",
				"O registro foi alterado por outra operação. Consulte-o de novo e repita a operação");
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException excecao,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
				"Um ou mais campos da requisição são inválidos");
		List<ErroDeCampo> erros = excecao.getBindingResult().getFieldErrors().stream()
				.map(erro -> new ErroDeCampo(erro.getField(), mensagem(erro)))
				.sorted((a, b) -> a.campo().compareTo(b.campo()))
				.toList();
		problema.setProperty("erros", erros);
		return ResponseEntity.badRequest().body(problema);
	}

	private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
		problema.setTitle(titulo);
		return problema;
	}

	private static String mensagem(FieldError erro) {
		return erro.getDefaultMessage() == null ? "inválido" : erro.getDefaultMessage();
	}

	record ErroDeCampo(String campo, String mensagem) {
	}

}
