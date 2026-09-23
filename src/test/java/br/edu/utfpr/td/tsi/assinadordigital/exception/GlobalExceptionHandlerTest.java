package br.edu.utfpr.td.tsi.assinadordigital.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import br.edu.utfpr.td.tsi.assinadordigital.dto.ErroResponse;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private GlobalExceptionHandler handler;

	@Test
	void arquivoEstaticoInexistenteRetorna404() throws Exception {
		mockMvc.perform(get("/favicon.ico")).andExpect(status().isNotFound());
		mockMvc.perform(get("/pagina-que-nao-existe.html")).andExpect(status().isNotFound());
	}

	@Test
	void paginasDoSistemaSaoServidas() throws Exception {
		mockMvc.perform(get("/")).andExpect(status().isOk());
		mockMvc.perform(get("/verificar.html")).andExpect(status().isOk());
		mockMvc.perform(get("/js/index.js")).andExpect(status().isOk());
		mockMvc.perform(get("/js/verificar.js")).andExpect(status().isOk());
		mockMvc.perform(get("/css/index.css")).andExpect(status().isOk());
	}

	/**
	 * O limite do Spring (spring.servlet.multipart.max-file-size) é aplicado
	 * antes de a requisição chegar ao controller e não pode ser simulado pelo
	 * MockMvc; por isso o tratamento é testado diretamente.
	 */
	@Test
	void arquivoAcimaDoLimiteDoSpringRetorna400() {
		ResponseEntity<ErroResponse> resposta = handler.handleMaxUploadSize(new MaxUploadSizeExceededException(10));

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(resposta.getBody().mensagem()).isEqualTo("O arquivo excede o tamanho máximo permitido.");
	}

	@Test
	void erroInesperadoRetorna500SemExporAMensagemTecnica() {
		ResponseEntity<ErroResponse> resposta = handler
				.handleGeneric(new IllegalStateException("NullPointer em PdfService linha 42"));

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(resposta.getBody().sucesso()).isFalse();
		assertThat(resposta.getBody().mensagem())
				.isEqualTo("Ocorreu um erro inesperado. Tente novamente mais tarde.")
				.doesNotContain("PdfService");
	}
}
