package br.edu.utfpr.td.tsi.assinadordigital.service;

import java.security.PrivateKey;
import java.security.cert.Certificate;

public record MaterialAssinatura(PrivateKey chavePrivada, Certificate[] cadeiaCertificados) {
}
