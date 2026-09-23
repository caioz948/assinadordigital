package br.edu.utfpr.td.tsi.assinadordigital;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 * Gera PDFs em memória para os testes, sem depender de arquivos externos.
 */
public final class PdfTestUtils {

	private PdfTestUtils() {
	}

	public static byte[] criarPdf(int quantidadePaginas) {
		try (PDDocument documento = new PDDocument()) {
			for (int i = 0; i < quantidadePaginas; i++) {
				documento.addPage(new PDPage(PDRectangle.A4));
			}
			ByteArrayOutputStream saida = new ByteArrayOutputStream();
			documento.save(saida);
			return saida.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("Não foi possível gerar o PDF de teste", e);
		}
	}
}
