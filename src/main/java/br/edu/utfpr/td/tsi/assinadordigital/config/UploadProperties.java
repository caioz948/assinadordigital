package br.edu.utfpr.td.tsi.assinadordigital.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Limite de tamanho aplicado pela própria aplicação, além do limite já
 * imposto pelo Spring (spring.servlet.multipart.max-file-size), como
 * camada extra de validação de negócio.
 */
@Component
@ConfigurationProperties(prefix = "assinatura.upload")
public class UploadProperties {

	private long maxSizeBytes = 10L * 1024 * 1024;

	public long getMaxSizeBytes() {
		return maxSizeBytes;
	}

	public void setMaxSizeBytes(long maxSizeBytes) {
		this.maxSizeBytes = maxSizeBytes;
	}
}
