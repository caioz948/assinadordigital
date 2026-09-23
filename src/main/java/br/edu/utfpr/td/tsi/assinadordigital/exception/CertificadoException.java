package br.edu.utfpr.td.tsi.assinadordigital.exception;

/**
 * Lançada quando o certificado digital configurado para assinatura não pode
 * ser carregado ou utilizado (inexistente, senha incorreta, alias inválido,
 * certificado expirado, etc).
 */
public class CertificadoException extends RuntimeException {

	public CertificadoException(String message) {
		super(message);
	}

	public CertificadoException(String message, Throwable cause) {
		super(message, cause);
	}
}
