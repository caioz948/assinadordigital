package br.edu.utfpr.td.tsi.assinadordigital.service;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;

/**
 * Extrai o nome amigável (atributo CN - Common Name) de um nome X.500 de
 * certificado, em vez de exibir a string inteira (ex: "CN=X,OU=Y,O=Z,C=BR").
 * Usado tanto na etiqueta visual da assinatura quanto na verificação.
 */
final class NomeCertificado {

	private NomeCertificado() {
	}

	/**
	 * @return o CN, ou {@code padrao} quando o nome não possui CN
	 */
	static String nomeComum(X500Name nome, String padrao) {
		RDN[] cns = nome.getRDNs(BCStyle.CN);
		if (cns.length == 0) {
			return padrao;
		}
		ASN1Encodable valor = cns[0].getFirst().getValue();
		// Texto puro do CN. IETFUtils.valueToString devolve o formato RFC 4514,
		// com escapes (ex.: "Silva\, Maria"), impróprio para exibição.
		if (valor instanceof ASN1String texto) {
			return texto.getString();
		}
		return IETFUtils.valueToString(valor);
	}

	static String nomeComum(X500Principal principal) {
		return nomeComum(X500Name.getInstance(principal.getEncoded()), principal.getName());
	}
}
