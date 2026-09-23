package br.edu.utfpr.td.tsi.assinadordigital.service;

import br.edu.utfpr.td.tsi.assinadordigital.exception.ArquivoInvalidoException;
import br.edu.utfpr.td.tsi.assinadordigital.exception.AssinaturaDigitalException;
import br.edu.utfpr.td.tsi.assinadordigital.exception.PdfInvalidoException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Responsável pela manipulação do PDF: validação do conteúdo enviado e
 * inserção da assinatura (criptográfica + representação visual), preservando
 * o máximo possível da estrutura original do documento.
 *
 * A parte puramente criptográfica é delegada ao {@link PdfSignatureService}.
 * A posição da etiqueta é sempre recebida como parâmetro (ver
 * {@link PosicaoAssinatura}): quem decide se é a posição padrão do servidor
 * ou uma posição escolhida manualmente pelo usuário é o {@link AssinaturaService}.
 */
@Service
public class PdfService {

	private static final Logger log = LoggerFactory.getLogger(PdfService.class);
	private static final byte[] ASSINATURA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);
	private static final int ESCALA_IMAGEM = 4;
	private static final int TAMANHO_FONTE_MINIMO = 6;

	/**
	 * Valida se o conteúdo recebido é, de fato, um PDF processável - não
	 * confiando apenas na extensão do arquivo enviado.
	 *
	 * @return quantidade de páginas do documento
	 */
	public int validarConteudo(byte[] conteudo) {
		if (!iniciaComAssinaturaPdf(conteudo)) {
			throw new ArquivoInvalidoException("Formato inválido. O arquivo enviado deve ser um PDF.");
		}

		try (PDDocument documento = Loader.loadPDF(conteudo)) {
			if (documento.getNumberOfPages() == 0) {
				throw new PdfInvalidoException("Erro ao processar a estrutura do PDF. O arquivo pode estar corrompido.");
			}
			return documento.getNumberOfPages();
		} catch (IOException e) {
			throw new PdfInvalidoException("Erro ao processar a estrutura do PDF. O arquivo pode estar corrompido.", e);
		}
	}

	private boolean iniciaComAssinaturaPdf(byte[] conteudo) {
		if (conteudo.length < ASSINATURA_PDF.length) {
			return false;
		}
		for (int i = 0; i < ASSINATURA_PDF.length; i++) {
			if (conteudo[i] != ASSINATURA_PDF[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Assina digitalmente o PDF e insere a representação visual da assinatura
	 * na posição informada.
	 */
	public byte[] assinar(byte[] pdfOriginal, String nomeAssinante, MaterialAssinatura material,
			PosicaoAssinatura posicaoSolicitada) {
		try (PDDocument documento = Loader.loadPDF(pdfOriginal)) {
			int indicePagina = calcularIndicePagina(posicaoSolicitada.pagina(), documento.getNumberOfPages());
			PDPage pagina = documento.getPage(indicePagina);
			PosicaoAssinatura posicao = ajustarAosLimitesDaPagina(posicaoSolicitada, pagina.getMediaBox());

			PDSignature assinatura = new PDSignature();
			assinatura.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
			assinatura.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
			assinatura.setName(nomeAssinante);
			assinatura.setReason("Assinatura digital via Assinador Digital UTFPR");
			assinatura.setSignDate(Calendar.getInstance());

			byte[] imagemEtiqueta = gerarImagemEtiqueta(nomeAssinante, assinatura.getSignDate(), posicao);

			// PDVisibleSignDesigner.yAxis() conta a partir do TOPO da página
			// (como uma coordenada de tela), diferente da convenção do PDF
			// (origem no canto inferior). PosicaoAssinatura.y() é sempre
			// "distância a partir de baixo", então é preciso converter aqui.
			float yAPartirDoTopo = pagina.getMediaBox().getHeight() - posicao.y() - posicao.height();

			PDVisibleSignDesigner designer = new PDVisibleSignDesigner(documento,
					new ByteArrayInputStream(imagemEtiqueta), indicePagina + 1);
			designer.xAxis(posicao.x())
					.yAxis(yAPartirDoTopo)
					.width(posicao.width())
					.height(posicao.height())
					.adjustForRotation();

			PDVisibleSigProperties propriedadesVisuais = new PDVisibleSigProperties();
			propriedadesVisuais.signerName(nomeAssinante)
					.signatureReason(assinatura.getReason())
					.preferredSize(0)
					.page(indicePagina + 1)
					.visualSignEnabled(true)
					.setPdVisibleSignature(designer)
					.buildSignature();

			SignatureOptions opcoes = new SignatureOptions();
			opcoes.setVisualSignature(propriedadesVisuais.getVisibleSignature());
			opcoes.setPage(indicePagina);

			documento.addSignature(assinatura, new PdfSignatureService(material), opcoes);

			ByteArrayOutputStream saida = new ByteArrayOutputStream();
			documento.saveIncremental(saida);
			return saida.toByteArray();
		} catch (IOException e) {
			throw new AssinaturaDigitalException("Não foi possível realizar a assinatura digital.", e);
		}
	}

	private int calcularIndicePagina(int pagina, int totalPaginas) {
		if (pagina <= 0 || pagina > totalPaginas) {
			return totalPaginas - 1;
		}
		return pagina - 1;
	}

	/**
	 * Garante que a etiqueta (informada pelo servidor ou escolhida pelo
	 * usuário no preview) fique dentro dos limites reais da página, mesmo
	 * que o cliente envie coordenadas inválidas/fora do documento.
	 */
	private PosicaoAssinatura ajustarAosLimitesDaPagina(PosicaoAssinatura posicao, PDRectangle mediaBox) {
		float larguraPagina = mediaBox.getWidth();
		float alturaPagina = mediaBox.getHeight();

		float largura = clamp(posicao.width(), 1f, larguraPagina);
		float altura = clamp(posicao.height(), 1f, alturaPagina);
		float x = clamp(posicao.x(), 0f, Math.max(0f, larguraPagina - largura));
		float y = clamp(posicao.y(), 0f, Math.max(0f, alturaPagina - altura));

		return new PosicaoAssinatura(posicao.pagina(), x, y, largura, altura);
	}

	private float clamp(float valor, float minimo, float maximo) {
		if (Float.isNaN(valor)) {
			return minimo;
		}
		return Math.max(minimo, Math.min(valor, maximo));
	}

	private byte[] gerarImagemEtiqueta(String nomeAssinante, Calendar dataAssinatura, PosicaoAssinatura posicao) {
		int largura = Math.max(1, Math.round(posicao.width() * ESCALA_IMAGEM));
		int altura = Math.max(1, Math.round(posicao.height() * ESCALA_IMAGEM));

		BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = imagem.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(new Color(240, 248, 255, 235));
			g.fillRoundRect(0, 0, largura - 1, altura - 1, 12, 12);
			g.setColor(new Color(30, 80, 150));
			g.setStroke(new BasicStroke(2f));
			g.drawRoundRect(1, 1, largura - 3, altura - 3, 12, 12);

			int padding = ESCALA_IMAGEM * 2;
			int larguraUtil = largura - (padding * 2);
			String dataFormatada = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(dataAssinatura.getTime());

			int y = padding;
			y = desenharLinha(g, "DOCUMENTO ASSINADO DIGITALMENTE", Font.BOLD, ESCALA_IMAGEM * 4,
					new Color(20, 60, 120), larguraUtil, padding, y);
			y = desenharLinha(g, "Assinado por: " + nomeAssinante, Font.PLAIN, ESCALA_IMAGEM * 3, Color.DARK_GRAY,
					larguraUtil, padding, y);
			desenharLinha(g, "Data: " + dataFormatada, Font.PLAIN, ESCALA_IMAGEM * 3, Color.DARK_GRAY, larguraUtil,
					padding, y);
		} finally {
			g.dispose();
		}

		try {
			ByteArrayOutputStream saida = new ByteArrayOutputStream();
			ImageIO.write(imagem, "png", saida);
			return saida.toByteArray();
		} catch (IOException e) {
			log.error("Erro ao gerar a representação visual da assinatura", e);
			throw new AssinaturaDigitalException("Não foi possível gerar a representação visual da assinatura.", e);
		}
	}

	/**
	 * Desenha uma linha de texto reduzindo a fonte até caber na largura
	 * disponível (e truncando com reticências como último recurso), para que
	 * nomes de assinante longos não estourem visualmente a etiqueta.
	 */
	private int desenharLinha(Graphics2D g, String texto, int estiloFonte, int tamanhoInicial, Color cor,
			int larguraDisponivel, int x, int y) {
		int tamanhoFonte = tamanhoInicial;
		Font fonte = new Font("SansSerif", estiloFonte, tamanhoFonte);
		FontMetrics metrics = g.getFontMetrics(fonte);
		String textoExibido = texto;

		while (metrics.stringWidth(textoExibido) > larguraDisponivel && tamanhoFonte > TAMANHO_FONTE_MINIMO) {
			tamanhoFonte--;
			fonte = new Font("SansSerif", estiloFonte, tamanhoFonte);
			metrics = g.getFontMetrics(fonte);
		}

		while (metrics.stringWidth(textoExibido + "...") > larguraDisponivel && textoExibido.length() > 1) {
			textoExibido = textoExibido.substring(0, textoExibido.length() - 1);
		}
		if (!textoExibido.equals(texto)) {
			textoExibido = textoExibido + "...";
		}

		g.setFont(fonte);
		g.setColor(cor);
		int linhaBase = y + metrics.getAscent();
		g.drawString(textoExibido, x, linhaBase);
		return linhaBase + metrics.getDescent() + (ESCALA_IMAGEM / 2);
	}
}
