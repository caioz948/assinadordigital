package br.edu.utfpr.td.tsi.assinadordigital.service;

public record PosicaoAssinaturaRequest(Integer pagina, Float x, Float y, Float width, Float height) {

	public boolean temPosicaoCompleta() {
		return x != null && y != null && width != null && height != null;
	}
}
