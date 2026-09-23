package br.edu.utfpr.td.tsi.assinadordigital.exception;

/**
 * Lançada quando o conteúdo enviado tem extensão/Content-Type de PDF, mas o
 * arquivo em si está corrompido ou não pôde ser processado pela biblioteca de PDF.
 */
public class PdfInvalidoException extends RuntimeException {

	public PdfInvalidoException(String message) {
		super(message);
	}

	public PdfInvalidoException(String message, Throwable cause) {
		super(message, cause);
	}
}
