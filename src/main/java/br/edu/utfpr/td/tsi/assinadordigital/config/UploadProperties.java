package br.edu.utfpr.td.tsi.assinadordigital.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
