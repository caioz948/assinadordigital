package br.edu.utfpr.td.tsi.assinadordigital.exception;

/**
 * Lançada quando o arquivo enviado pelo usuário não atende aos requisitos
 * básicos de upload (ausente, vazio, extensão/Content-Type incorretos ou
 * acima do tamanho máximo permitido).
 */
public class ArquivoInvalidoException extends RuntimeException {

	public ArquivoInvalidoException(String message) {
		super(message);
	}
}
