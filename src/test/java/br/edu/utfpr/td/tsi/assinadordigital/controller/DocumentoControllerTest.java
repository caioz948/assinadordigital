package br.edu.utfpr.td.tsi.assinadordigital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import br.edu.utfpr.td.tsi.assinadordigital.PdfTestUtils;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentoControllerTest {

	private static final String URL = "/api/v1/documentos/upload";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void pdfValidoRetorna200ComDadosDoDocumento() throws Exception {
		byte[] pdf = PdfTestUtils.criarPdf(2);
		MockMultipartFile arquivo = new MockMultipartFile("arquivo", "relatorio.pdf", "application/pdf", pdf);

		mockMvc.perform(multipart(URL).file(arquivo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nomeArquivo").value("relatorio.pdf"))
				.andExpect(jsonPath("$.tipoConteudo").value("application/pdf"))
				.andExpect(jsonPath("$.tamanhoBytes").value(pdf.length))
				.andExpect(jsonPath("$.quantidadePaginas").value(2))
				.andExpect(jsonPath("$.mensagem").value("PDF carregado e validado com sucesso!"));
	}

	@Test
	void arquivoQueNaoEPdfRetorna400ComErroPadrao() throws Exception {
		MockMultipartFile arquivo = new MockMultipartFile("arquivo", "falso.pdf", "application/pdf",
				"nao sou pdf".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart(URL).file(arquivo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.sucesso").value(false))
				.andExpect(jsonPath("$.mensagem").value("Formato inválido. O arquivo enviado deve ser um PDF."));
	}

	@Test
	void pdfCorrompidoRetorna422() throws Exception {
		MockMultipartFile arquivo = new MockMultipartFile("arquivo", "corrompido.pdf", "application/pdf",
				"%PDF-1.7\nquebrado".getBytes(StandardCharsets.US_ASCII));

		mockMvc.perform(multipart(URL).file(arquivo))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.sucesso").value(false));
	}

	@Test
	void requisicaoSemCampoArquivoRetorna400() throws Exception {
		MockMultipartFile outroCampo = new MockMultipartFile("outro", "a.pdf", "application/pdf",
				PdfTestUtils.criarPdf(1));

		mockMvc.perform(multipart(URL).file(outroCampo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("Selecione um arquivo PDF."));
	}

	@Test
	void requisicaoQueNaoEMultipartRetorna400() throws Exception {
		mockMvc.perform(post(URL))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("Selecione um arquivo PDF."));
	}
}
