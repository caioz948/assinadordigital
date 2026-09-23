package br.edu.utfpr.td.tsi.assinadordigital.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resultado da verificação de uma assinatura encontrada no PDF.
 *
 * @param situacao           VALIDA, VALIDA_COM_ALTERACOES_POSTERIORES ou INVALIDA
 * @param integra            os bytes cobertos pela assinatura não foram alterados
 * @param cobreDocumentoInteiro a assinatura cobre o arquivo até o último byte
 *                           (false quando algo foi acrescentado depois dela,
 *                           por exemplo outra assinatura)
 * @param avisos             observações que não invalidam a assinatura (ex.:
 *                           certificado autoassinado)
 */
public record AssinaturaVerificadaDTO(
		String assinante,
		String emissorCertificado,
		OffsetDateTime dataAssinatura,
		String motivo,
		String situacao,
		String mensagem,
		boolean integra,
		boolean cobreDocumentoInteiro,
		List<String> avisos) {
}
