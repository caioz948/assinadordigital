package br.edu.utfpr.td.tsi.assinadordigital.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Centraliza o posicionamento da representação visual da assinatura no PDF.
 * Para a Sprint #1 é utilizada uma posição padrão (última página, canto inferior
 * direito), mas manter esses valores aqui - em vez de espalhados pelo código -
 * permite evoluir futuramente para um posicionamento escolhido pelo usuário.
 */
@Component
@ConfigurationProperties(prefix = "assinatura.posicao")
public class PosicaoAssinaturaProperties {

	/** Página onde a etiqueta é inserida. Valores menores ou iguais a 0 (ou maiores que o total de páginas) representam a última página do documento. */
	private int pagina = -1;
	private float x = 350f;
	private float y = 100f;
	private float width = 200f;
	private float height = 80f;

	public int getPagina() {
		return pagina;
	}

	public void setPagina(int pagina) {
		this.pagina = pagina;
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getWidth() {
		return width;
	}

	public void setWidth(float width) {
		this.width = width;
	}

	public float getHeight() {
		return height;
	}

	public void setHeight(float height) {
		this.height = height;
	}
}
