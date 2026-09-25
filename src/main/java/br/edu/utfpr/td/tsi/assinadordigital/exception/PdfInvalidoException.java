package br.edu.utfpr.td.tsi.assinadordigital.exception;

public class PdfInvalidoException extends RuntimeException {

	public PdfInvalidoException(String message) {
		super(message);
	}

	public PdfInvalidoException(String message, Throwable cause) {
		super(message, cause);
	}
}
