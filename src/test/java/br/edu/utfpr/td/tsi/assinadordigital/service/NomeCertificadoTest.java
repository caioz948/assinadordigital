package br.edu.utfpr.td.tsi.assinadordigital.service;

import static org.assertj.core.api.Assertions.assertThat;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.X500Name;
import org.junit.jupiter.api.Test;

class NomeCertificadoTest {

	@Test
	void extraiApenasOCnDoNome() {
		X500Name nome = new X500Name("CN=Maria da Silva,OU=TSI,O=UTFPR,C=BR");

		assertThat(NomeCertificado.nomeComum(nome, "padrao")).isEqualTo("Maria da Silva");
	}

	@Test
	void cnComVirgulaEParenteses() {
		X500Name nome = new X500Name("CN=Silva\\, Maria (Professora),O=UTFPR");

		assertThat(NomeCertificado.nomeComum(nome, "padrao")).isEqualTo("Silva, Maria (Professora)");
	}

	@Test
	void semCnRetornaOValorPadrao() {
		X500Name nome = new X500Name("OU=TSI,O=UTFPR,C=BR");

		assertThat(NomeCertificado.nomeComum(nome, "padrao")).isEqualTo("padrao");
	}

	@Test
	void aPartirDoPrincipalDoCertificadoJava() {
		X500Principal principal = new X500Principal("CN=Assinador Digital UTFPR,OU=TSI,O=UTFPR,C=BR");

		assertThat(NomeCertificado.nomeComum(principal)).isEqualTo("Assinador Digital UTFPR");
	}

	@Test
	void principalSemCnRetornaONomeCompleto() {
		X500Principal principal = new X500Principal("OU=TSI,O=UTFPR,C=BR");

		assertThat(NomeCertificado.nomeComum(principal)).isEqualTo(principal.getName());
	}
}
