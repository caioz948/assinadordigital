package br.edu.utfpr.td.tsi.assinadordigital.dto;

public record DocumentoDTO(
		String nomeArquivo,
		String tipoConteudo,
		long tamanhoBytes,
		int quantidadePaginas,
		String mensagem) {
}
