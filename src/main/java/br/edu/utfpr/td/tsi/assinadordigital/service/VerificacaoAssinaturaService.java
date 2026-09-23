package br.edu.utfpr.td.tsi.assinadordigital.service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignerDigestMismatchException;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import br.edu.utfpr.td.tsi.assinadordigital.dto.AssinaturaVerificadaDTO;
import br.edu.utfpr.td.tsi.assinadordigital.dto.VerificacaoDTO;
import br.edu.utfpr.td.tsi.assinadordigital.exception.PdfInvalidoException;

/**
 * Verifica as assinaturas digitais (CMS/PKCS#7) existentes em um PDF: se os
 * bytes assinados continuam íntegros, se algo foi acrescentado depois de
 * cada assinatura e quem assinou.
 *
 * Não valida a cadeia de confiança até uma Autoridade Certificadora raiz
 * (ICP-Brasil/ICPEdu): certificados autoassinados geram apenas um aviso.
 */
@Service
public class VerificacaoAssinaturaService {

	public static final String VALIDA = "VALIDA";
	public static final String VALIDA_COM_ALTERACOES_POSTERIORES = "VALIDA_COM_ALTERACOES_POSTERIORES";
	public static final String INVALIDA = "INVALIDA";

	private static final Logger log = LoggerFactory.getLogger(VerificacaoAssinaturaService.class);

	private final DocumentoService documentoService;

	public VerificacaoAssinaturaService(DocumentoService documentoService) {
		this.documentoService = documentoService;
	}

	public VerificacaoDTO verificar(MultipartFile arquivo) {
		byte[] conteudo = documentoService.carregarPdfValidado(arquivo);

		List<AssinaturaVerificadaDTO> resultados = new ArrayList<>();
		try (PDDocument documento = Loader.loadPDF(conteudo)) {
			for (PDSignature assinatura : documento.getSignatureDictionaries()) {
				resultados.add(verificarAssinatura(assinatura, conteudo));
			}
		} catch (IOException e) {
			throw new PdfInvalidoException("Erro ao processar a estrutura do PDF. O arquivo pode estar corrompido.", e);
		}

		int validas = (int) resultados.stream().filter(r -> !INVALIDA.equals(r.situacao())).count();
		log.info("Verificação concluída: {} assinatura(s), {} válida(s)", resultados.size(), validas);

		return new VerificacaoDTO(arquivo.getOriginalFilename(), resultados.size(), validas,
				mensagemGeral(resultados.size(), validas), resultados);
	}

	private String mensagemGeral(int total, int validas) {
		if (total == 0) {
			return "Nenhuma assinatura digital foi encontrada no documento.";
		}
		if (validas == total) {
			return total == 1 ? "A assinatura do documento é válida." : "Todas as " + total + " assinaturas são válidas.";
		}
		return (total - validas) + " de " + total + " assinatura(s) inválida(s): o documento foi alterado depois de assinado.";
	}

	private AssinaturaVerificadaDTO verificarAssinatura(PDSignature assinatura, byte[] pdf) {
		List<String> avisos = new ArrayList<>();
		String assinante = assinatura.getName();
		String emissor = null;
		OffsetDateTime data = converterData(assinatura.getSignDate());

		boolean integra = false;
		try {
			CMSSignedData dados = lerCms(assinatura, pdf);
			SignerInformation signatario = dados.getSignerInfos().getSigners().iterator().next();
			X509CertificateHolder certificado = certificadoDoSignatario(dados, signatario);

			assinante = NomeCertificado.nomeComum(certificado.getSubject(), assinante);
			emissor = NomeCertificado.nomeComum(certificado.getIssuer(), certificado.getIssuer().toString());

			if (certificado.getSubject().equals(certificado.getIssuer())) {
				avisos.add("Certificado autoassinado: a identidade do assinante não é garantida por uma Autoridade Certificadora.");
			}
			if (assinatura.getSignDate() != null && !certificado.isValidOn(assinatura.getSignDate().getTime())) {
				avisos.add("O certificado não estava dentro do prazo de validade na data da assinatura.");
			}

			integra = signatario.verify(new JcaSimpleSignerInfoVerifierBuilder()
					.setProvider(BouncyCastleProvider.PROVIDER_NAME).build(certificado));
		} catch (CMSSignerDigestMismatchException e) {
			integra = false;
		} catch (CMSException | OperatorCreationException | IOException | java.security.cert.CertificateException
				| RuntimeException e) {
			log.warn("Não foi possível interpretar uma assinatura do documento: {}", e.getMessage());
			avisos.add("Não foi possível interpretar os dados desta assinatura.");
		}

		boolean cobreInteiro = cobreDocumentoInteiro(assinatura, pdf.length);

		String situacao;
		String mensagem;
		if (!integra) {
			situacao = INVALIDA;
			mensagem = "O documento foi alterado ou corrompido depois que esta assinatura foi aplicada.";
		} else if (cobreInteiro) {
			situacao = VALIDA;
			mensagem = "O documento não foi modificado desde que esta assinatura foi aplicada.";
		} else {
			situacao = VALIDA_COM_ALTERACOES_POSTERIORES;
			mensagem = "A assinatura é válida, mas o documento recebeu acréscimos depois dela (por exemplo, outra assinatura).";
		}

		return new AssinaturaVerificadaDTO(assinante, emissor, data, assinatura.getReason(), situacao, mensagem,
				integra, cobreInteiro, avisos);
	}

	/**
	 * O /Contents é preenchido com zeros até o tamanho reservado; lê-se só o
	 * objeto ASN.1 da assinatura, ignorando o preenchimento.
	 */
	private CMSSignedData lerCms(PDSignature assinatura, byte[] pdf) throws IOException, CMSException {
		byte[] cms = assinatura.getContents(pdf);
		byte[] conteudoAssinado = assinatura.getSignedContent(pdf);

		ContentInfo contentInfo;
		try (ASN1InputStream entrada = new ASN1InputStream(cms)) {
			contentInfo = ContentInfo.getInstance(entrada.readObject());
		}
		return new CMSSignedData(new CMSProcessableByteArray(conteudoAssinado), contentInfo);
	}

	private X509CertificateHolder certificadoDoSignatario(CMSSignedData dados, SignerInformation signatario) {
		@SuppressWarnings("unchecked")
		Collection<X509CertificateHolder> certificados = dados.getCertificates().getMatches(signatario.getSID());
		if (certificados.isEmpty()) {
			throw new IllegalStateException("Certificado do signatário não incluído na assinatura");
		}
		return certificados.iterator().next();
	}

	/**
	 * ByteRange [0, a, b, c]: a assinatura cobre o documento inteiro quando o
	 * último trecho termina no último byte do arquivo.
	 */
	private boolean cobreDocumentoInteiro(PDSignature assinatura, int tamanhoArquivo) {
		int[] byteRange = assinatura.getByteRange();
		return byteRange != null && byteRange.length == 4 && byteRange[0] == 0
				&& (long) byteRange[2] + byteRange[3] == tamanhoArquivo;
	}

	private OffsetDateTime converterData(Calendar data) {
		if (data == null) {
			return null;
		}
		return OffsetDateTime.ofInstant(data.toInstant(), data.getTimeZone().toZoneId());
	}
}
