package br.edu.utfpr.td.tsi.assinadordigital.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.edu.utfpr.td.tsi.assinadordigital.dto.DocumentoDTO;
import br.edu.utfpr.td.tsi.assinadordigital.dto.VerificacaoDTO;
import br.edu.utfpr.td.tsi.assinadordigital.service.DocumentoService;
import br.edu.utfpr.td.tsi.assinadordigital.service.VerificacaoAssinaturaService;

/**
 * Envio, validação e verificação de assinaturas de PDF. Os erros são tratados
 * pelo GlobalExceptionHandler e retornados no formato padrão da API
 * (ErroResponse).
 */
@RestController
@RequestMapping("/api/v1/documentos")
public class DocumentoController {

	private final DocumentoService documentoService;
	private final VerificacaoAssinaturaService verificacaoService;

	public DocumentoController(DocumentoService documentoService, VerificacaoAssinaturaService verificacaoService) {
		this.documentoService = documentoService;
		this.verificacaoService = verificacaoService;
	}

	@PostMapping("/upload")
	public ResponseEntity<DocumentoDTO> uploadPdf(@RequestParam("arquivo") MultipartFile arquivo) {
		return ResponseEntity.ok(documentoService.processarEValidarPdf(arquivo));
	}

	@PostMapping("/verificar")
	public ResponseEntity<VerificacaoDTO> verificarAssinaturas(@RequestParam("arquivo") MultipartFile arquivo) {
		return ResponseEntity.ok(verificacaoService.verificar(arquivo));
	}

}
