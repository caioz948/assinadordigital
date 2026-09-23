package br.edu.utfpr.td.tsi.assinadordigital.service;

/**
 * Posição opcional informada pelo usuário na requisição (por exemplo, ao
 * arrastar/redimensionar a etiqueta no preview do PDF antes de assinar).
 * Todos os campos são anuláveis: quando x/y/width/height não vêm completos,
 * {@link AssinaturaService} utiliza a posição padrão configurada no servidor.
 */
public record PosicaoAssinaturaRequest(Integer pagina, Float x, Float y, Float width, Float height) {

	public boolean temPosicaoCompleta() {
		return x != null && y != null && width != null && height != null;
	}
}
