package br.edu.utfpr.td.tsi.assinadordigital.config;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Configuration;

/**
 * Registra o provedor criptográfico do Bouncy Castle uma única vez, na
 * inicialização da aplicação. Ele é usado para gerar (PdfSignatureService) e
 * verificar (VerificacaoAssinaturaService) as assinaturas CMS/PKCS#7.
 */
@Configuration
public class BouncyCastleConfig {

	public BouncyCastleConfig() {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}
}
