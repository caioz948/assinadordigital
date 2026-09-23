package br.edu.utfpr.td.tsi.assinadordigital.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
class VerificacaoAssinaturaControllerTest {

	private static final String URL = "/api/v1/documentos/verificar";

	@Autowired
	private MockMvc mockMvc;

	private static MockMultipartFile arquivo(String nome, byte[] conteudo) {
		return new MockMultipartFile("arquivo", nome, "application/pdf", conteudo);
	}

	private byte[] assinar(byte[] pdf) throws Exception {
		return mockMvc.perform(multipart("/api/v1/assinaturas").file(arquivo("documento.pdf", pdf)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsByteArray();
	}

	@Test
	void pdfAssinadoPeloSistemaTemAssinaturaValida() throws Exception {
		byte[] assinado = assinar(PdfTestUtils.criarPdf(1));

		mockMvc.perform(multipart(URL).file(arquivo("documento_assinado.pdf", assinado)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nomeArquivo").value("documento_assinado.pdf"))
				.andExpect(jsonPath("$.quantidadeAssinaturas").value(1))
				.andExpect(jsonPath("$.quantidadeValidas").value(1))
				.andExpect(jsonPath("$.assinaturas[0].situacao").value("VALIDA"))
				.andExpect(jsonPath("$.assinaturas[0].integra").value(true))
				.andExpect(jsonPath("$.assinaturas[0].cobreDocumentoInteiro").value(true))
				.andExpect(jsonPath("$.assinaturas[0].assinante").value(containsString("Assinador Digital UTFPR")))
				.andExpect(jsonPath("$.assinaturas[0].emissorCertificado").value(containsString("Assinador Digital UTFPR")))
				.andExpect(jsonPath("$.assinaturas[0].dataAssinatura").value(notNullValue()))
				.andExpect(jsonPath("$.assinaturas[0].avisos").value(hasItem(containsString("autoassinado"))));
	}

	@Test
	void documentoAlteradoDepoisDeAssinadoTemAssinaturaInvalida() throws Exception {
		byte[] adulterado = assinar(PdfTestUtils.criarPdf(1));
		// Byte do comentário binário da 2ª linha: está dentro do trecho assinado
		// e não quebra a estrutura do PDF.
		adulterado[10] = (byte) (adulterado[10] ^ 0x01);

		mockMvc.perform(multipart(URL).file(arquivo("adulterado.pdf", adulterado)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantidadeAssinaturas").value(1))
				.andExpect(jsonPath("$.quantidadeValidas").value(0))
				.andExpect(jsonPath("$.assinaturas[0].situacao").value("INVALIDA"))
				.andExpect(jsonPath("$.assinaturas[0].integra").value(false));
	}

	@Test
	void documentoAssinadoDuasVezesMantemAPrimeiraAssinaturaValida() throws Exception {
		byte[] duasAssinaturas = assinar(assinar(PdfTestUtils.criarPdf(1)));

		mockMvc.perform(multipart(URL).file(arquivo("duas.pdf", duasAssinaturas)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantidadeAssinaturas").value(2))
				.andExpect(jsonPath("$.quantidadeValidas").value(2))
				.andExpect(jsonPath("$.assinaturas[0].situacao").value("VALIDA_COM_ALTERACOES_POSTERIORES"))
				.andExpect(jsonPath("$.assinaturas[0].integra").value(true))
				.andExpect(jsonPath("$.assinaturas[0].cobreDocumentoInteiro").value(false))
				.andExpect(jsonPath("$.assinaturas[1].situacao").value("VALIDA"))
				.andExpect(jsonPath("$.assinaturas[1].cobreDocumentoInteiro").value(true));
	}

	@Test
	void pdfSemAssinaturaInformaQueNaoHaAssinaturas() throws Exception {
		mockMvc.perform(multipart(URL).file(arquivo("sem_assinatura.pdf", PdfTestUtils.criarPdf(1))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantidadeAssinaturas").value(0))
				.andExpect(jsonPath("$.mensagem").value("Nenhuma assinatura digital foi encontrada no documento."));
	}

	@Test
	void arquivoQueNaoEPdfRetorna400() throws Exception {
		mockMvc.perform(multipart(URL).file(arquivo("falso.pdf", "nao sou pdf".getBytes(StandardCharsets.UTF_8))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("Formato inválido. O arquivo enviado deve ser um PDF."));
	}
}
