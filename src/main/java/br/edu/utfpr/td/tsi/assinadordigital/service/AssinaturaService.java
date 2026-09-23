package br.edu.utfpr.td.tsi.assinadordigital.service;

import br.edu.utfpr.td.tsi.assinadordigital.config.PosicaoAssinaturaProperties;
import br.edu.utfpr.td.tsi.assinadordigital.config.UploadProperties;
import br.edu.utfpr.td.tsi.assinadordigital.exception.ArquivoInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.Set;

/**
 * Orquestra o fluxo completo de assinatura: valida o upload, resolve a
 * posição da etiqueta (escolhida pelo usuário ou padrão do servidor) e
 * delega a validação/assinatura do PDF ao {@link PdfService} e o
 * carregamento do certificado ao {@link CertificadoService}.
 */
@Service
public class AssinaturaService {

	private static final Logger log = LoggerFactory.getLogger(AssinaturaService.class);
	private static final Set<String> CONTENT_TYPES_ACEITOS = Set.of("application/pdf", "application/octet-stream");

	private final PdfService pdfService;
	private final CertificadoService certificadoService;
	private final UploadProperties uploadProperties;
	private final PosicaoAssinaturaProperties posicaoPadrao;

	public AssinaturaService(PdfService pdfService, CertificadoService certificadoService,
			UploadProperties uploadProperties, PosicaoAssinaturaProperties posicaoPadrao) {
		this.pdfService = pdfService;
		this.certificadoService = certificadoService;
		this.uploadProperties = uploadProperties;
		this.posicaoPadrao = posicaoPadrao;
	}

	public ResultadoAssinatura assinar(MultipartFile arquivo, String nomeAssinante,
			PosicaoAssinaturaRequest posicaoRequest) {
		validarArquivo(arquivo);

		String nomeOriginalSanitizado = nomeSanitizado(arquivo.getOriginalFilename());
		log.info("Recebendo PDF para assinatura: {} ({} bytes)", nomeOriginalSanitizado, arquivo.getSize());

		byte[] conteudoOriginal = lerConteudo(arquivo);
		pdfService.validarConteudo(conteudoOriginal);

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

	private void validarArquivo(MultipartFile arquivo) {
		if (arquivo == null || arquivo.isEmpty()) {
			throw new ArquivoInvalidoException("Selecione um arquivo PDF.");
		}
		if (arquivo.getSize() > uploadProperties.getMaxSizeBytes()) {
			throw new ArquivoInvalidoException("O arquivo excede o tamanho máximo permitido.");
		}

		String nomeOriginal = arquivo.getOriginalFilename();
		if (!StringUtils.hasText(nomeOriginal) || !nomeOriginal.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
			throw new ArquivoInvalidoException("Formato de arquivo inválido. Envie um PDF.");
		}

		String contentType = arquivo.getContentType();
		if (contentType != null && !CONTENT_TYPES_ACEITOS.contains(contentType.toLowerCase(Locale.ROOT))) {
			throw new ArquivoInvalidoException("Formato de arquivo inválido. Envie um PDF.");
		}
	}

	private byte[] lerConteudo(MultipartFile arquivo) {
		try {
			return arquivo.getBytes();
		} catch (IOException e) {
			throw new ArquivoInvalidoException("Não foi possível ler o arquivo enviado.");
		}
	}

	private String nomeDoCertificado(MaterialAssinatura material) {
		if (material.cadeiaCertificados().length > 0
				&& material.cadeiaCertificados()[0] instanceof X509Certificate x509) {
			String cn = extrairCommonName(x509.getSubjectX500Principal().getName());
			return cn != null ? cn : x509.getSubjectX500Principal().getName();
		}
		return "Assinador Digital UTFPR";
	}

	/**
	 * Extrai apenas o atributo CN (Common Name) do Subject DN do certificado,
	 * em vez de exibir a string inteira (ex: "CN=X,OU=Y,O=Z,C=BR") como nome
	 * do assinante na etiqueta visual.
	 */
	private String extrairCommonName(String subjectDn) {
		try {
			LdapName ldapName = new LdapName(subjectDn);
			for (Rdn rdn : ldapName.getRdns()) {
				if ("CN".equalsIgnoreCase(rdn.getType())) {
					return rdn.getValue().toString();
				}
			}
		} catch (InvalidNameException e) {
			log.warn("Não foi possível interpretar o Subject DN do certificado para exibição amigável");
		}
		return null;
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
