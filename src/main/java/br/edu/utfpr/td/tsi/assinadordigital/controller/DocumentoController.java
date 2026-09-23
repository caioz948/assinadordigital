package br.edu.utfpr.td.tsi.assinadordigital.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.edu.utfpr.td.tsi.assinadordigital.dto.DocumentoDTO;
import br.edu.utfpr.td.tsi.assinadordigital.service.DocumentoService;

@RestController
@RequestMapping("/api/v1/documentos")
@CrossOrigin(origins = "*") // Permite requisições de testadores ou front-end
public class DocumentoController {
	
	private final DocumentoService documentoService;
	
	public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }
	
	@PostMapping("/upload")
    public ResponseEntity<?> uploadPdf(@RequestParam("arquivo") MultipartFile arquivo) {
        try {
            DocumentoDTO dto = documentoService.processarEValidarPdf(arquivo);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno no servidor ao processar o arquivo: " + e.getMessage());
        }
    }

}
