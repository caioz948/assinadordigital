package br.edu.utfpr.td.tsi.assinadordigital.dto;

/**
 * Formato padrão de resposta para erros da API de assinatura, para nunca
 * expor exceções técnicas/stack traces diretamente ao usuário.
 */
public record ErroResponse(boolean sucesso, String mensagem) {

	public static ErroResponse of(String mensagem) {
		return new ErroResponse(false, mensagem);
	}
}
