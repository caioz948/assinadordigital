package br.edu.utfpr.td.tsi.assinadordigital.dto;

public class DocumentoDTO {

	private String nomeArquivo;
	private String tipoConteudo;
	private long tamanhoBytes;
	private int quantidadePaginas;
	private String mensagem;

	
	public DocumentoDTO() {
	}
	
	public DocumentoDTO(String nomeArquivo, String tipoConteudo, long tamanhoBytes, int quantidadePaginas,
			String mensagem) {
		super();
		this.nomeArquivo = nomeArquivo;
		this.tipoConteudo = tipoConteudo;
		this.tamanhoBytes = tamanhoBytes;
		this.quantidadePaginas = quantidadePaginas;
		this.mensagem = mensagem;
	}

	public String getNomeArquivo() {
		return nomeArquivo;
	}

	public void setNomeArquivo(String nomeArquivo) {
		this.nomeArquivo = nomeArquivo;
	}

	public String getTipoConteudo() {
		return tipoConteudo;
	}

	public void setTipoConteudo(String tipoConteudo) {
		this.tipoConteudo = tipoConteudo;
	}

	public long getTamanhoBytes() {
		return tamanhoBytes;
	}

	public void setTamanhoBytes(long tamanhoBytes) {
		this.tamanhoBytes = tamanhoBytes;
	}

	public int getQuantidadePaginas() {
		return quantidadePaginas;
	}

	public void setQuantidadePaginas(int quantidadePaginas) {
		this.quantidadePaginas = quantidadePaginas;
	}

	public String getMensagem() {
		return mensagem;
	}

	public void setMensagem(String mensagem) {
		this.mensagem = mensagem;
	}

}
