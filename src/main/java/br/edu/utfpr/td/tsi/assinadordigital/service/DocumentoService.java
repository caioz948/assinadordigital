package br.edu.utfpr.td.tsi.assinadordigital.service;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import br.edu.utfpr.td.tsi.assinadordigital.config.UploadProperties;
import br.edu.utfpr.td.tsi.assinadordigital.dto.DocumentoDTO;
import br.edu.utfpr.td.tsi.assinadordigital.exception.ArquivoInvalidoException;

@Service
public class DocumentoService {

	private static final Set<String> CONTENT_TYPES_ACEITOS = Set.of("application/pdf", "application/octet-stream");

	private final UploadProperties uploadProperties;
	private final PdfService pdfService;

	public DocumentoService(UploadProperties uploadProperties, PdfService pdfService) {
		this.uploadProperties = uploadProperties;
		this.pdfService = pdfService;
	}

	public DocumentoDTO processarEValidarPdf(MultipartFile arquivo) {
		validarArquivo(arquivo);
		byte[] conteudo = lerConteudo(arquivo);
		int quantidadePaginas = pdfService.validarConteudo(conteudo);

		return new DocumentoDTO(
				arquivo.getOriginalFilename(),
				arquivo.getContentType(),
				arquivo.getSize(),
				quantidadePaginas,
				"PDF carregado e validado com sucesso!");
	}

	public byte[] carregarPdfValidado(MultipartFile arquivo) {
		validarArquivo(arquivo);
		byte[] conteudo = lerConteudo(arquivo);
		pdfService.validarConteudo(conteudo);
		return conteudo;
	}

	private void validarArquivo(MultipartFile arquivo) {
		if (arquivo == null || arquivo.isEmpty()) {
			throw new ArquivoInvalidoException("Nenhum arquivo foi selecionado para envio.");
		}
		if (arquivo.getSize() > uploadProperties.getMaxSizeBytes()) {
			throw new ArquivoInvalidoException("O arquivo excede o tamanho máximo permitido.");
		}

		String nomeArquivo = arquivo.getOriginalFilename();
		if (!StringUtils.hasText(nomeArquivo) || !nomeArquivo.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
			throw new ArquivoInvalidoException("Formato inválido. O arquivo enviado deve ser um PDF.");
		}

		String contentType = arquivo.getContentType();
		if (contentType != null && !CONTENT_TYPES_ACEITOS.contains(contentType.toLowerCase(Locale.ROOT))) {
			throw new ArquivoInvalidoException("Formato inválido. O arquivo enviado deve ser um PDF.");
		}
	}

	private byte[] lerConteudo(MultipartFile arquivo) {
		try {
			return arquivo.getBytes();
		} catch (IOException e) {
			throw new ArquivoInvalidoException("Não foi possível ler o arquivo enviado.");
		}
	}
}
