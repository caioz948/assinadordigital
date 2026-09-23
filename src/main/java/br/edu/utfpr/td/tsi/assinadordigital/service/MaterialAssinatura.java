package br.edu.utfpr.td.tsi.assinadordigital.service;

import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * Material criptográfico (chave privada + cadeia de certificados) obtido a
 * partir do keystore configurado, necessário para assinar um PDF.
 */
public record MaterialAssinatura(PrivateKey chavePrivada, Certificate[] cadeiaCertificados) {
}
