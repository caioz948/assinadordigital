package br.edu.utfpr.td.tsi.assinadordigital.service;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;

final class NomeCertificado {

	private NomeCertificado() {
	}

	static String nomeComum(X500Name nome, String padrao) {
		RDN[] cns = nome.getRDNs(BCStyle.CN);
		if (cns.length == 0) {
			return padrao;
		}
		ASN1Encodable valor = cns[0].getFirst().getValue();
		if (valor instanceof ASN1String texto) {
			return texto.getString();
		}
		return IETFUtils.valueToString(valor);
	}

	static String nomeComum(X500Principal principal) {
		return nomeComum(X500Name.getInstance(principal.getEncoded()), principal.getName());
	}
}
