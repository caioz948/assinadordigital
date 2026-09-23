package br.edu.utfpr.td.tsi.assinadordigital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignerDigestMismatchException;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import br.edu.utfpr.td.tsi.assinadordigital.PdfTestUtils;

@SpringBootTest
@AutoConfigureMockMvc
class AssinaturaControllerTest {

	private static final String URL = "/api/v1/assinaturas";

	@Autowired
	private MockMvc mockMvc;

	private byte[] assinar(MockMultipartFile arquivo, String... parametros) throws Exception {
		var requisicao = multipart(URL).file(arquivo);
		for (int i = 0; i < parametros.length; i += 2) {
			requisicao.param(parametros[i], parametros[i + 1]);
		}
		return mockMvc.perform(requisicao)
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andReturn().getResponse().getContentAsByteArray();
	}

	private static MockMultipartFile pdf(String nome, int paginas) {
		return new MockMultipartFile("arquivo", nome, "application/pdf", PdfTestUtils.criarPdf(paginas));
	}

	/**
	 * Mesma verificação feita por leitores como o Adobe Reader: a assinatura
	 * CMS confere com o certificado embutido e com os bytes cobertos pelo
	 * ByteRange.
	 */
	private static boolean assinaturaConfere(byte[] pdfAssinado, PDSignature assinatura) throws Exception {
		byte[] cms = assinatura.getContents(pdfAssinado);
		byte[] conteudoAssinado = assinatura.getSignedContent(pdfAssinado);

		// O /Contents é preenchido com zeros até o tamanho reservado; lê-se só
		// o objeto ASN.1 da assinatura, ignorando o preenchimento.
		ContentInfo contentInfo;
		try (ASN1InputStream entrada = new ASN1InputStream(cms)) {
			contentInfo = ContentInfo.getInstance(entrada.readObject());
		}
		CMSSignedData dados = new CMSSignedData(new CMSProcessableByteArray(conteudoAssinado), contentInfo);
		SignerInformation assinante = dados.getSignerInfos().getSigners().iterator().next();
		@SuppressWarnings("unchecked")
		X509CertificateHolder certificado = (X509CertificateHolder) dados.getCertificates()
				.getMatches(assinante.getSID()).iterator().next();

		try {
			return assinante.verify(new JcaSimpleSignerInfoVerifierBuilder()
					.setProvider(BouncyCastleProvider.PROVIDER_NAME).build(certificado));
		} catch (CMSSignerDigestMismatchException e) {
			// Resumo (hash) dos bytes não bate com o que foi assinado: documento alterado.
			return false;
		}
	}

	@Test
	void pdfAssinadoTemAssinaturaDigitalValidaCobrindoODocumentoInteiro() throws Exception {
		byte[] pdfAssinado = assinar(pdf("contrato.pdf", 1));

		try (PDDocument documento = Loader.loadPDF(pdfAssinado)) {
			List<PDSignature> assinaturas = documento.getSignatureDictionaries();
			assertThat(assinaturas).hasSize(1);

			PDSignature assinatura = assinaturas.get(0);
			assertThat(assinatura.getFilter()).isEqualTo(PDSignature.FILTER_ADOBE_PPKLITE.getName());
			assertThat(assinatura.getSubFilter()).isEqualTo(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED.getName());
			assertThat(assinatura.getName()).isNotBlank();
			assertThat(assinatura.getSignDate()).isNotNull();

			// ByteRange [0, a, b, c]: começa no primeiro byte e termina no último
			// byte do arquivo, ou seja, nada ficou de fora da assinatura.
			int[] byteRange = assinatura.getByteRange();
			assertThat(byteRange[0]).isZero();
			assertThat(byteRange[2] + byteRange[3]).isEqualTo(pdfAssinado.length);

			assertThat(assinaturaConfere(pdfAssinado, assinatura)).isTrue();
		}
	}

	@Test
	void alteracaoAposAssinaturaEDetectada() throws Exception {
		byte[] pdfAssinado = assinar(pdf("contrato.pdf", 1));

		try (PDDocument documento = Loader.loadPDF(pdfAssinado)) {
			PDSignature assinatura = documento.getSignatureDictionaries().get(0);

			// Altera um byte dentro do trecho coberto pela assinatura.
			byte[] adulterado = pdfAssinado.clone();
			int posicao = assinatura.getByteRange()[1] / 2;
			adulterado[posicao] = (byte) (adulterado[posicao] ^ 0x01);

			assertThat(assinaturaConfere(adulterado, assinatura)).isFalse();
		}
	}

	@Test
	void nomeDoArquivoAssinadoRecebeSufixo() throws Exception {
		mockMvc.perform(multipart(URL).file(pdf("contrato.pdf", 1)))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition",
						containsString("contrato_assinado.pdf")));
	}

	@Test
	void etiquetaFicaNaPosicaoEscolhidaPeloUsuario() throws Exception {
		byte[] pdfAssinado = assinar(pdf("contrato.pdf", 2),
				"pagina", "1", "x", "100", "y", "150", "width", "180", "height", "60");

		try (PDDocument documento = Loader.loadPDF(pdfAssinado)) {
			PDSignatureField campo = documento.getSignatureFields().get(0);
			PDAnnotationWidget widget = campo.getWidgets().get(0);
			PDRectangle retangulo = widget.getRectangle();

			assertThat(documento.getPage(0).getAnnotations()).anyMatch(a -> a.getCOSObject() == widget.getCOSObject());
			assertThat(retangulo.getLowerLeftX()).isCloseTo(100f, within(1f));
			assertThat(retangulo.getLowerLeftY()).isCloseTo(150f, within(1f));
			assertThat(retangulo.getWidth()).isCloseTo(180f, within(1f));
			assertThat(retangulo.getHeight()).isCloseTo(60f, within(1f));
		}
	}

	@Test
	void semPosicaoInformadaEtiquetaVaiParaAUltimaPagina() throws Exception {
		byte[] pdfAssinado = assinar(pdf("contrato.pdf", 3));

		try (PDDocument documento = Loader.loadPDF(pdfAssinado)) {
			PDAnnotationWidget widget = documento.getSignatureFields().get(0).getWidgets().get(0);

			assertThat(documento.getPage(2).getAnnotations()).anyMatch(a -> a.getCOSObject() == widget.getCOSObject());
		}
	}

	@Test
	void etiquetaComCoordenadasForaDaPaginaEAjustadaParaDentroDela() throws Exception {
		byte[] pdfAssinado = assinar(pdf("contrato.pdf", 1),
				"pagina", "1", "x", "-500", "y", "-500", "width", "5000", "height", "5000");

		try (PDDocument documento = Loader.loadPDF(pdfAssinado)) {
			PDRectangle pagina = documento.getPage(0).getMediaBox();
			PDRectangle retangulo = documento.getSignatureFields().get(0).getWidgets().get(0).getRectangle();

			assertThat(retangulo.getLowerLeftX()).isGreaterThanOrEqualTo(-0.5f);
			assertThat(retangulo.getLowerLeftY()).isGreaterThanOrEqualTo(-0.5f);
			assertThat(retangulo.getUpperRightX()).isLessThanOrEqualTo(pagina.getWidth() + 0.5f);
			assertThat(retangulo.getUpperRightY()).isLessThanOrEqualTo(pagina.getHeight() + 0.5f);
		}
	}

	@Test
	void paginaInexistenteColocaEtiquetaNaUltimaPagina() throws Exception {
		byte[] pdfAssinado = assinar(pdf("contrato.pdf", 2),
				"pagina", "99", "x", "100", "y", "100", "width", "150", "height", "50");

		try (PDDocument documento = Loader.loadPDF(pdfAssinado)) {
			PDAnnotationWidget widget = documento.getSignatureFields().get(0).getWidgets().get(0);

			assertThat(documento.getPage(1).getAnnotations()).anyMatch(a -> a.getCOSObject() == widget.getCOSObject());
		}
	}

	@Test
	void semNomeInformadoAssinanteEOCnDoCertificado() throws Exception {
		byte[] pdfAssinado = assinar(pdf("contrato.pdf", 1));

		try (PDDocument documento = Loader.loadPDF(pdfAssinado)) {
			// Só o CN, sem o restante do nome X.500 ("CN=...,OU=TSI,O=UTFPR,...").
			assertThat(documento.getSignatureDictionaries().get(0).getName())
					.isEqualTo("Assinador Digital UTFPR (Certificado de Desenvolvimento)");
		}
	}

	@Test
	void nomeDoAssinanteInformadoEGravadoNaAssinatura() throws Exception {
		byte[] pdfAssinado = assinar(pdf("contrato.pdf", 1), "nomeAssinante", "Maria da Silva");

		try (PDDocument documento = Loader.loadPDF(pdfAssinado)) {
			assertThat(documento.getSignatureDictionaries().get(0).getName()).isEqualTo("Maria da Silva");
		}
	}

	@Test
	void arquivoQueNaoEPdfRetorna400ComErroPadrao() throws Exception {
		MockMultipartFile falso = new MockMultipartFile("arquivo", "falso.pdf", "application/pdf",
				"nao sou pdf".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart(URL).file(falso))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.sucesso").value(false))
				.andExpect(jsonPath("$.mensagem").value("Formato inválido. O arquivo enviado deve ser um PDF."));
	}
}
