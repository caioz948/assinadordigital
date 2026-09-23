package br.edu.utfpr.td.tsi.assinadordigital.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import br.edu.utfpr.td.tsi.assinadordigital.PdfTestUtils;

/**
 * Com a senha do certificado do servidor errada, a assinatura falha com uma
 * mensagem genérica, sem expor ao usuário detalhes do certificado.
 */
@SpringBootTest(properties = "signature.keystore.password=senha-errada")
@AutoConfigureMockMvc
class CertificadoMalConfiguradoTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void assinaturaFalhaComMensagemGenericaSemExporDetalhes() throws Exception {
		MockMultipartFile arquivo = new MockMultipartFile("arquivo", "contrato.pdf", "application/pdf",
				PdfTestUtils.criarPdf(1));

		mockMvc.perform(multipart("/api/v1/assinaturas").file(arquivo))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.sucesso").value(false))
				.andExpect(jsonPath("$.mensagem").value("Não foi possível realizar a assinatura digital."))
				.andExpect(content().string(not(containsString("senha"))))
				.andExpect(content().string(not(containsString("keystore"))));
	}

	@Test
	void uploadEVerificacaoNaoDependemDoCertificado() throws Exception {
		MockMultipartFile arquivo = new MockMultipartFile("arquivo", "contrato.pdf", "application/pdf",
				PdfTestUtils.criarPdf(1));

		mockMvc.perform(multipart("/api/v1/documentos/upload").file(arquivo))
				.andExpect(status().isOk());
		mockMvc.perform(multipart("/api/v1/documentos/verificar").file(arquivo))
				.andExpect(status().isOk());
	}
}
