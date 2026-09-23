package br.edu.utfpr.td.tsi.assinadordigital.service;

/**
 * Resultado da assinatura de um documento: os bytes do PDF assinado e o
 * nome de arquivo sugerido para download, mantendo o arquivo original intacto.
 */
public record ResultadoAssinatura(byte[] conteudo, String nomeArquivo) {
}
