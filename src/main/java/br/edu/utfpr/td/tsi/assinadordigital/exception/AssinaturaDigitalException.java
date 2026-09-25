package br.edu.utfpr.td.tsi.assinadordigital.exception;

public class AssinaturaDigitalException extends RuntimeException {

	public AssinaturaDigitalException(String message) {
		super(message);
	}

	public AssinaturaDigitalException(String message, Throwable cause) {
		super(message, cause);
	}
}
