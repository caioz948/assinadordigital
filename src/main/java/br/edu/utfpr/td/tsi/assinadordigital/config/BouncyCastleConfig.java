package br.edu.utfpr.td.tsi.assinadordigital.config;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BouncyCastleConfig {

	public BouncyCastleConfig() {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}
}
