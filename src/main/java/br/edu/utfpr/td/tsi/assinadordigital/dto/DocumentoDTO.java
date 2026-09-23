package br.edu.utfpr.td.tsi.assinadordigital.dto;

/**
 * Resposta do envio de um PDF: dados do arquivo recebido após a validação.
 */
public record DocumentoDTO(
		String nomeArquivo,
		String tipoConteudo,
		long tamanhoBytes,
		int quantidadePaginas,
		String mensagem) {
}
