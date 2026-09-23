package br.edu.utfpr.td.tsi.assinadordigital.exception;

/**
 * Lançada quando ocorre uma falha durante a construção da assinatura
 * criptográfica ou da representação visual dentro do PDF.
 */
public class AssinaturaDigitalException extends RuntimeException {

	public AssinaturaDigitalException(String message) {
		super(message);
	}

	public AssinaturaDigitalException(String message, Throwable cause) {
		super(message, cause);
	}
}
