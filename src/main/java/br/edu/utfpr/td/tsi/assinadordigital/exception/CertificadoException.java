package br.edu.utfpr.td.tsi.assinadordigital.exception;

public class CertificadoException extends RuntimeException {

	public CertificadoException(String message) {
		super(message);
	}

	public CertificadoException(String message, Throwable cause) {
		super(message, cause);
	}
}
