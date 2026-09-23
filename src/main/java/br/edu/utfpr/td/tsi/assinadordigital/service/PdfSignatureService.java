package br.edu.utfpr.td.tsi.assinadordigital.service;

import br.edu.utfpr.td.tsi.assinadordigital.exception.AssinaturaDigitalException;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * Implementa a assinatura criptográfica propriamente dita: recebe o range de
 * bytes do PDF (calculado internamente pelo PDFBox) e devolve uma assinatura
 * CMS/PKCS#7 detached, compatível com leitores de PDF como o Adobe Acrobat Reader.
 *
 * Esta classe cuida apenas da parte criptográfica; a manipulação do documento
 * PDF em si (campo de assinatura, aparência visual, página) fica a cargo do
 * {@link PdfService}.
 */
class PdfSignatureService implements SignatureInterface {

	static {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private final MaterialAssinatura material;

	PdfSignatureService(MaterialAssinatura material) {
		this.material = material;
	}

	@Override
	public byte[] sign(InputStream content) throws IOException {
		try {
			X509Certificate certificado = (X509Certificate) material.cadeiaCertificados()[0];

			ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA")
					.setProvider(BouncyCastleProvider.PROVIDER_NAME)
					.build(material.chavePrivada());

			CMSSignedDataGenerator gerador = new CMSSignedDataGenerator();
			gerador.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
					new JcaDigestCalculatorProviderBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME).build())
					.build(contentSigner, certificado));
			gerador.addCertificates(new JcaCertStore(Arrays.asList(material.cadeiaCertificados())));

			byte[] conteudo = content.readAllBytes();
			CMSSignedData signedData = gerador.generate(new CMSProcessableByteArray(conteudo), false);
			return signedData.getEncoded();
		} catch (GeneralSecurityException | OperatorCreationException | CMSException e) {
			throw new AssinaturaDigitalException("Não foi possível realizar a assinatura digital.", e);
		}
	}
}
