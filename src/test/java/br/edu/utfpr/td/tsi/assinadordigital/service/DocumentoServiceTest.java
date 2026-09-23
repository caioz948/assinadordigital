package br.edu.utfpr.td.tsi.assinadordigital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import br.edu.utfpr.td.tsi.assinadordigital.PdfTestUtils;
import br.edu.utfpr.td.tsi.assinadordigital.config.UploadProperties;
import br.edu.utfpr.td.tsi.assinadordigital.dto.DocumentoDTO;
import br.edu.utfpr.td.tsi.assinadordigital.exception.ArquivoInvalidoException;
import br.edu.utfpr.td.tsi.assinadordigital.exception.PdfInvalidoException;

class DocumentoServiceTest {

	private static final long TAMANHO_MAXIMO = 1024 * 1024;

	private DocumentoService documentoService;

	@BeforeEach
	void setUp() {
		UploadProperties uploadProperties = new UploadProperties();
		uploadProperties.setMaxSizeBytes(TAMANHO_MAXIMO);
		documentoService = new DocumentoService(uploadProperties, new PdfService());
	}

	private static MockMultipartFile arquivo(String nome, String contentType, byte[] conteudo) {
		return new MockMultipartFile("arquivo", nome, contentType, conteudo);
	}

	@Test
	void pdfValidoRetornaInformacoesDoDocumento() {
		byte[] pdf = PdfTestUtils.criarPdf(3);

		DocumentoDTO dto = documentoService.processarEValidarPdf(arquivo("contrato.pdf", "application/pdf", pdf));

		assertThat(dto.getNomeArquivo()).isEqualTo("contrato.pdf");
		assertThat(dto.getTipoConteudo()).isEqualTo("application/pdf");
		assertThat(dto.getTamanhoBytes()).isEqualTo(pdf.length);
		assertThat(dto.getQuantidadePaginas()).isEqualTo(3);
		assertThat(dto.getMensagem()).isEqualTo("PDF carregado e validado com sucesso!");
	}

	@Test
	void carregarPdfValidadoRetornaOConteudoOriginal() {
		byte[] pdf = PdfTestUtils.criarPdf(1);

		byte[] conteudo = documentoService.carregarPdfValidado(arquivo("a.pdf", "application/pdf", pdf));

		assertThat(conteudo).isEqualTo(pdf);
	}

	@Test
	void aceitaExtensaoEmMaiusculasEContentTypeOctetStream() {
		byte[] pdf = PdfTestUtils.criarPdf(1);

		DocumentoDTO dto = documentoService
				.processarEValidarPdf(arquivo("CONTRATO.PDF", "application/octet-stream", pdf));

		assertThat(dto.getQuantidadePaginas()).isEqualTo(1);
	}

	@Test
	void recusaArquivoVazio() {
		assertThatThrownBy(() -> documentoService
				.processarEValidarPdf(arquivo("vazio.pdf", "application/pdf", new byte[0])))
				.isInstanceOf(ArquivoInvalidoException.class)
				.hasMessage("Nenhum arquivo foi selecionado para envio.");
	}

	@Test
	void recusaArquivoAusente() {
		assertThatThrownBy(() -> documentoService.processarEValidarPdf(null))
				.isInstanceOf(ArquivoInvalidoException.class)
				.hasMessage("Nenhum arquivo foi selecionado para envio.");
	}

	@Test
	void recusaArquivoAcimaDoTamanhoMaximo() {
		byte[] grande = new byte[(int) TAMANHO_MAXIMO + 1];

		assertThatThrownBy(() -> documentoService
				.processarEValidarPdf(arquivo("grande.pdf", "application/pdf", grande)))
				.isInstanceOf(ArquivoInvalidoException.class)
				.hasMessage("O arquivo excede o tamanho máximo permitido.");
	}

	@Test
	void recusaExtensaoDiferenteDePdf() {
		byte[] pdf = PdfTestUtils.criarPdf(1);

		assertThatThrownBy(() -> documentoService.processarEValidarPdf(arquivo("documento.txt", "text/plain", pdf)))
				.isInstanceOf(ArquivoInvalidoException.class)
				.hasMessage("Formato inválido. O arquivo enviado deve ser um PDF.");
	}

	@Test
	void recusaContentTypeDiferenteDePdf() {
		byte[] pdf = PdfTestUtils.criarPdf(1);

		assertThatThrownBy(() -> documentoService.processarEValidarPdf(arquivo("documento.pdf", "image/png", pdf)))
				.isInstanceOf(ArquivoInvalidoException.class)
				.hasMessage("Formato inválido. O arquivo enviado deve ser um PDF.");
	}

	@Test
	void recusaArquivoComExtensaoPdfMasConteudoDeOutroTipo() {
		byte[] texto = "isto nao e um pdf".getBytes(StandardCharsets.UTF_8);

		assertThatThrownBy(() -> documentoService
				.processarEValidarPdf(arquivo("falso.pdf", "application/pdf", texto)))
				.isInstanceOf(ArquivoInvalidoException.class)
				.hasMessage("Formato inválido. O arquivo enviado deve ser um PDF.");
	}

	@Test
	void recusaPdfCorrompido() {
		byte[] corrompido = "%PDF-1.7\nconteudo quebrado".getBytes(StandardCharsets.US_ASCII);

		assertThatThrownBy(() -> documentoService
				.processarEValidarPdf(arquivo("corrompido.pdf", "application/pdf", corrompido)))
				.isInstanceOf(PdfInvalidoException.class)
				.hasMessage("Erro ao processar a estrutura do PDF. O arquivo pode estar corrompido.");
	}
}
