package br.edu.utfpr.td.tsi.assinadordigital.service;

import br.edu.utfpr.td.tsi.assinadordigital.config.PosicaoAssinaturaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.security.cert.X509Certificate;

/**
 * Orquestra o fluxo completo de assinatura: delega a validação do upload ao
 * {@link DocumentoService}, resolve a posição da etiqueta (escolhida pelo
 * usuário ou padrão do servidor) e delega a assinatura do PDF ao
 * {@link PdfService} e o carregamento do certificado ao {@link CertificadoService}.
 */
@Service
public class AssinaturaService {

	private static final Logger log = LoggerFactory.getLogger(AssinaturaService.class);

	private final PdfService pdfService;
	private final CertificadoService certificadoService;
	private final DocumentoService documentoService;
	private final PosicaoAssinaturaProperties posicaoPadrao;

	public AssinaturaService(PdfService pdfService, CertificadoService certificadoService,
			DocumentoService documentoService, PosicaoAssinaturaProperties posicaoPadrao) {
		this.pdfService = pdfService;
		this.certificadoService = certificadoService;
		this.documentoService = documentoService;
		this.posicaoPadrao = posicaoPadrao;
	}

	public ResultadoAssinatura assinar(MultipartFile arquivo, String nomeAssinante,
			PosicaoAssinaturaRequest posicaoRequest) {
		byte[] conteudoOriginal = documentoService.carregarPdfValidado(arquivo);

		String nomeOriginalSanitizado = nomeSanitizado(arquivo.getOriginalFilename());
		log.info("Recebendo PDF para assinatura: {} ({} bytes)", nomeOriginalSanitizado, arquivo.getSize());

		MaterialAssinatura material = certificadoService.carregarMaterialAssinatura();
		String assinante = StringUtils.hasText(nomeAssinante) ? nomeAssinante : nomeDoCertificado(material);
		PosicaoAssinatura posicao = resolverPosicao(posicaoRequest);

		log.info("Iniciando assinatura digital");
		byte[] conteudoAssinado = pdfService.assinar(conteudoOriginal, assinante, material, posicao);
		log.info("Assinatura digital concluída");

		String nomeArquivoSaida = gerarNomeArquivoSaida(nomeOriginalSanitizado);
		return new ResultadoAssinatura(conteudoAssinado, nomeArquivoSaida);
	}

	private PosicaoAssinatura resolverPosicao(PosicaoAssinaturaRequest posicaoRequest) {
		if (posicaoRequest != null && posicaoRequest.temPosicaoCompleta()) {
			int pagina = posicaoRequest.pagina() != null ? posicaoRequest.pagina() : -1;
			return new PosicaoAssinatura(pagina, posicaoRequest.x(), posicaoRequest.y(), posicaoRequest.width(),
					posicaoRequest.height());
		}
		return new PosicaoAssinatura(posicaoPadrao.getPagina(), posicaoPadrao.getX(), posicaoPadrao.getY(),
				posicaoPadrao.getWidth(), posicaoPadrao.getHeight());
	}

	private String nomeDoCertificado(MaterialAssinatura material) {
		if (material.cadeiaCertificados().length > 0
				&& material.cadeiaCertificados()[0] instanceof X509Certificate x509) {
			return NomeCertificado.nomeComum(x509.getSubjectX500Principal());
		}
		return "Assinador Digital UTFPR";
	}

	private String gerarNomeArquivoSaida(String nomeOriginalSanitizado) {
		String base = nomeOriginalSanitizado.substring(0, nomeOriginalSanitizado.length() - 4);
		return base + "_assinado.pdf";
	}

	private String nomeSanitizado(String nomeOriginal) {
		String nome = StringUtils.hasText(nomeOriginal) ? nomeOriginal : "documento.pdf";
		return nome.replaceAll("[\\\\/:*?\"<>|]", "_");
	}
}
