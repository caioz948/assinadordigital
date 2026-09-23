package br.edu.utfpr.td.tsi.assinadordigital.controller;

import br.edu.utfpr.td.tsi.assinadordigital.service.AssinaturaService;
import br.edu.utfpr.td.tsi.assinadordigital.service.PosicaoAssinaturaRequest;
import br.edu.utfpr.td.tsi.assinadordigital.service.ResultadoAssinatura;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * Endpoint de upload e assinatura digital de PDF. O documento assinado é
 * retornado diretamente na resposta (application/pdf + Content-Disposition:
 * attachment), sem sobrescrever o arquivo original enviado.
 *
 * Os parâmetros pagina/x/y/width/height são opcionais: quando o usuário
 * arrasta a etiqueta no preview (frontend), eles trafegam a posição
 * escolhida; quando ausentes, o servidor usa a posição padrão configurada
 * (assinatura.posicao.*).
 */
@RestController
public class AssinaturaController {

	private final AssinaturaService assinaturaService;

	public AssinaturaController(AssinaturaService assinaturaService) {
		this.assinaturaService = assinaturaService;
	}

	@PostMapping(value = "/api/assinaturas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<byte[]> assinar(
			@RequestParam("arquivo") MultipartFile arquivo,
			@RequestParam(value = "nomeAssinante", required = false) String nomeAssinante,
			@RequestParam(value = "pagina", required = false) Integer pagina,
			@RequestParam(value = "x", required = false) Float x,
			@RequestParam(value = "y", required = false) Float y,
			@RequestParam(value = "width", required = false) Float width,
			@RequestParam(value = "height", required = false) Float height) {

		PosicaoAssinaturaRequest posicaoRequest = new PosicaoAssinaturaRequest(pagina, x, y, width, height);
		ResultadoAssinatura resultado = assinaturaService.assinar(arquivo, nomeAssinante, posicaoRequest);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDisposition(
				ContentDisposition.attachment().filename(resultado.nomeArquivo(), StandardCharsets.UTF_8).build());

		return ResponseEntity.ok().headers(headers).body(resultado.conteudo());
	}
}
