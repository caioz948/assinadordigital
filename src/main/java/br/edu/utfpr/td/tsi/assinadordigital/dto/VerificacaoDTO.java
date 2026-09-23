package br.edu.utfpr.td.tsi.assinadordigital.dto;

import java.util.List;

/**
 * Resultado da verificação de todas as assinaturas de um PDF.
 */
public record VerificacaoDTO(
		String nomeArquivo,
		int quantidadeAssinaturas,
		int quantidadeValidas,
		String mensagem,
		List<AssinaturaVerificadaDTO> assinaturas) {
}
