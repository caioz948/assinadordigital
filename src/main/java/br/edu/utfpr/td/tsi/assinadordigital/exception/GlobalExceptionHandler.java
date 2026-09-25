package br.edu.utfpr.td.tsi.assinadordigital.exception;

import br.edu.utfpr.td.tsi.assinadordigital.dto.ErroResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ArquivoInvalidoException.class)
	public ResponseEntity<ErroResponse> handleArquivoInvalido(ArquivoInvalidoException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErroResponse.of(ex.getMessage()));
	}

	@ExceptionHandler({ MissingServletRequestPartException.class, HttpMediaTypeNotSupportedException.class })
	public ResponseEntity<ErroResponse> handleArquivoAusente(Exception ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErroResponse.of("Selecione um arquivo PDF."));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErroResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErroResponse.of("O arquivo excede o tamanho máximo permitido."));
	}

	@ExceptionHandler(MultipartException.class)
	public ResponseEntity<ErroResponse> handleRequisicaoSemArquivo(MultipartException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErroResponse.of("Selecione um arquivo PDF."));
	}

	@ExceptionHandler(PdfInvalidoException.class)
	public ResponseEntity<ErroResponse> handlePdfInvalido(PdfInvalidoException ex) {
		log.warn("PDF não processável: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(ErroResponse.of(ex.getMessage()));
	}

	@ExceptionHandler(CertificadoException.class)
	public ResponseEntity<ErroResponse> handleCertificado(CertificadoException ex) {
		log.error("Erro de certificado durante assinatura digital: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErroResponse.of("Não foi possível realizar a assinatura digital."));
	}

	@ExceptionHandler(AssinaturaDigitalException.class)
	public ResponseEntity<ErroResponse> handleAssinaturaDigital(AssinaturaDigitalException ex) {
		log.error("Erro ao assinar documento", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErroResponse.of("Não foi possível realizar a assinatura digital."));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Void> handleRecursoNaoEncontrado(NoResourceFoundException ex) {
		return ResponseEntity.notFound().build();
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErroResponse> handleGeneric(Exception ex) {
		log.error("Erro inesperado ao processar a requisição", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErroResponse.of("Ocorreu um erro inesperado. Tente novamente mais tarde."));
	}
}
