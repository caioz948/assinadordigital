package br.edu.utfpr.td.tsi.assinadordigital.dto;

public record ErroResponse(boolean sucesso, String mensagem) {

	public static ErroResponse of(String mensagem) {
		return new ErroResponse(false, mensagem);
	}
}
