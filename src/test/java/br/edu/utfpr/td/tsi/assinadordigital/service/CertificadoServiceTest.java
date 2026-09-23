package br.edu.utfpr.td.tsi.assinadordigital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import br.edu.utfpr.td.tsi.assinadordigital.config.AssinaturaKeystoreProperties;
import br.edu.utfpr.td.tsi.assinadordigital.exception.CertificadoException;

/**
 * Usa o certificado de desenvolvimento do projeto (certs/dev-keystore.p12,
 * senha "changeit", alias "assinador-dev").
 */
class CertificadoServiceTest {

	private static CertificadoService servico(String path, String senha, String alias) {
		AssinaturaKeystoreProperties properties = new AssinaturaKeystoreProperties();
		properties.setPath(path);
		properties.setPassword(senha);
		properties.setAlias(alias);
		return new CertificadoService(properties, new DefaultResourceLoader());
	}

	@Test
	void carregaChavePrivadaECadeiaDoCertificadoDeDesenvolvimento() {
		MaterialAssinatura material = servico("classpath:certs/dev-keystore.p12", "changeit", "assinador-dev")
				.carregarMaterialAssinatura();

		assertThat(material.chavePrivada()).isNotNull();
		assertThat(material.cadeiaCertificados()).isNotEmpty();
	}

	@Test
	void senhaErrada() {
		assertThatThrownBy(() -> servico("classpath:certs/dev-keystore.p12", "senha-errada", "assinador-dev")
				.carregarMaterialAssinatura())
				.isInstanceOf(CertificadoException.class)
				.hasMessage("Senha do certificado de assinatura incorreta.");
	}

	@Test
	void aliasInexistente() {
		assertThatThrownBy(() -> servico("classpath:certs/dev-keystore.p12", "changeit", "outro-alias")
				.carregarMaterialAssinatura())
				.isInstanceOf(CertificadoException.class)
				.hasMessage("Alias do certificado de assinatura não encontrado.");
	}

	@Test
	void arquivoDoCertificadoInexistente() {
		assertThatThrownBy(() -> servico("classpath:certs/nao-existe.p12", "changeit", "assinador-dev")
				.carregarMaterialAssinatura())
				.isInstanceOf(CertificadoException.class)
				.hasMessage("Certificado de assinatura não encontrado.");
	}

	@Test
	void caminhoDoCertificadoNaoConfigurado() {
		assertThatThrownBy(() -> servico("  ", "changeit", "assinador-dev").carregarMaterialAssinatura())
				.isInstanceOf(CertificadoException.class)
				.hasMessage("Certificado de assinatura não configurado.");
	}
}
