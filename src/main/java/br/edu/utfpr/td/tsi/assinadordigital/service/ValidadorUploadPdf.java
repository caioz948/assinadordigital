package br.edu.utfpr.td.tsi.assinadordigital.service;

import br.edu.utfpr.td.tsi.assinadordigital.config.UploadProperties;
import br.edu.utfpr.td.tsi.assinadordigital.exception.ArquivoInvalidoException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * Validações básicas de upload de PDF (presença, tamanho, extensão e
 * Content-Type), compartilhadas entre o fluxo de assinatura e o de envio
 * de documentos.
 */
@Component
public class ValidadorUploadPdf {

	private static final Set<String> CONTENT_TYPES_ACEITOS = Set.of("application/pdf", "application/octet-stream");

	private final UploadProperties uploadProperties;

	public ValidadorUploadPdf(UploadProperties uploadProperties) {
		this.uploadProperties = uploadProperties;
	}

	public void validar(MultipartFile arquivo) {
		if (arquivo == null || arquivo.isEmpty()) {
			throw new ArquivoInvalidoException("Selecione um arquivo PDF.");
		}
		if (arquivo.getSize() > uploadProperties.getMaxSizeBytes()) {
			throw new ArquivoInvalidoException("O arquivo excede o tamanho máximo permitido.");
		}

		String nomeOriginal = arquivo.getOriginalFilename();
		if (!StringUtils.hasText(nomeOriginal) || !nomeOriginal.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
			throw new ArquivoInvalidoException("Formato de arquivo inválido. Envie um PDF.");
		}

		String contentType = arquivo.getContentType();
		if (contentType != null && !CONTENT_TYPES_ACEITOS.contains(contentType.toLowerCase(Locale.ROOT))) {
			throw new ArquivoInvalidoException("Formato de arquivo inválido. Envie um PDF.");
		}
	}

	public byte[] lerConteudo(MultipartFile arquivo) {
		try {
			return arquivo.getBytes();
		} catch (IOException e) {
			throw new ArquivoInvalidoException("Não foi possível ler o arquivo enviado.");
		}
	}
}
