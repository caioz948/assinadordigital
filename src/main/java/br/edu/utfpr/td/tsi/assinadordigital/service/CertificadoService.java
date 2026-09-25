package br.edu.utfpr.td.tsi.assinadordigital.service;

import br.edu.utfpr.td.tsi.assinadordigital.config.AssinaturaKeystoreProperties;
import br.edu.utfpr.td.tsi.assinadordigital.exception.CertificadoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

@Service
public class CertificadoService {

	private static final Logger log = LoggerFactory.getLogger(CertificadoService.class);

	private final AssinaturaKeystoreProperties properties;
	private final ResourceLoader resourceLoader;

	public CertificadoService(AssinaturaKeystoreProperties properties, ResourceLoader resourceLoader) {
		this.properties = properties;
		this.resourceLoader = resourceLoader;
	}

	public MaterialAssinatura carregarMaterialAssinatura() {
		if (properties.getPath() == null || properties.getPath().isBlank()) {
			throw new CertificadoException("Certificado de assinatura não configurado.");
		}

		Resource resource = resourceLoader.getResource(properties.getPath());
		if (!resource.exists()) {
			throw new CertificadoException("Certificado de assinatura não encontrado.");
		}

		KeyStore keyStore = carregarKeyStore(resource);
		String alias = properties.getAlias();

		try {
			if (alias == null || alias.isBlank() || !keyStore.containsAlias(alias)) {
				throw new CertificadoException("Alias do certificado de assinatura não encontrado.");
			}

			Certificate[] cadeia = keyStore.getCertificateChain(alias);
			if (cadeia == null || cadeia.length == 0) {
				throw new CertificadoException("Cadeia de certificados não encontrada para o alias informado.");
			}

			validarValidade(cadeia[0]);

			PrivateKey chavePrivada = (PrivateKey) keyStore.getKey(alias, senhaComoCharArray());
			if (chavePrivada == null) {
				throw new CertificadoException("Chave privada não encontrada para o alias informado.");
			}

			log.info("Certificado de assinatura carregado com sucesso (alias: {})", alias);
			return new MaterialAssinatura(chavePrivada, cadeia);
		} catch (UnrecoverableKeyException e) {
			throw new CertificadoException("Senha do certificado de assinatura incorreta.", e);
		} catch (CertificadoException e) {
			throw e;
		} catch (GeneralSecurityException e) {
			throw new CertificadoException("Não foi possível carregar o certificado de assinatura.", e);
		}
	}

	private KeyStore carregarKeyStore(Resource resource) {
		try (InputStream in = resource.getInputStream()) {
			KeyStore keyStore = KeyStore.getInstance(properties.getType());
			keyStore.load(in, senhaComoCharArray());
			return keyStore;
		} catch (IOException e) {
			if (e.getCause() instanceof UnrecoverableKeyException) {
				throw new CertificadoException("Senha do certificado de assinatura incorreta.", e);
			}
			throw new CertificadoException("Não foi possível ler o certificado de assinatura.", e);
		} catch (GeneralSecurityException e) {
			throw new CertificadoException("Certificado de assinatura inválido.", e);
		}
	}

	private void validarValidade(Certificate certificado) {
		if (certificado instanceof X509Certificate x509) {
			try {
				x509.checkValidity();
			} catch (CertificateExpiredException e) {
				throw new CertificadoException("O certificado de assinatura está expirado.", e);
			} catch (CertificateNotYetValidException e) {
				throw new CertificadoException("O certificado de assinatura ainda não é válido.", e);
			}
		}
	}

	private char[] senhaComoCharArray() {
		return properties.getPassword() == null ? new char[0] : properties.getPassword().toCharArray();
	}
}
