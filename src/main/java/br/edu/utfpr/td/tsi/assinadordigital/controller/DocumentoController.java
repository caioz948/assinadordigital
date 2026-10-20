package br.edu.utfpr.td.tsi.assinadordigital.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.edu.utfpr.td.tsi.assinadordigital.dto.DocumentoDTO;
import br.edu.utfpr.td.tsi.assinadordigital.service.DocumentoService;
import br.edu.utfpr.td.tsi.assinadordigital.service.PdfService;

@RestController
@RequestMapping("/api/v1/documentos")
public class DocumentoController {

	private final DocumentoService documentoService;

	public DocumentoController(DocumentoService documentoService) {
		this.documentoService = documentoService;
	}

	@PostMapping("/upload")
	public ResponseEntity<DocumentoDTO> uploadPdf(@RequestParam("arquivo") MultipartFile arquivo) {
		return ResponseEntity.ok(documentoService.processarEValidarPdf(arquivo));
	}

	@PostMapping("/verificar")
	public ResponseEntity<List<PdfService.InformacaoAssinatura>> verificarAssinatura(@RequestParam("arquivo") MultipartFile arquivo) {
		List<PdfService.InformacaoAssinatura> resultados = documentoService.verificarAssinaturaPdf(arquivo);
		return ResponseEntity.ok(resultados);
	}

}