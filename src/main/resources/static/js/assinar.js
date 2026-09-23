import * as pdfjsLib from '/vendor/pdfjs/pdf.min.mjs';

pdfjsLib.GlobalWorkerOptions.workerSrc = '/vendor/pdfjs/pdf.worker.min.mjs';

const LARGURA_MAXIMA_CANVAS = 560;
const MARGEM_PADRAO_PT = 24;
const LARGURA_ETIQUETA_MAXIMA_PT = 200;
const ALTURA_ETIQUETA_MAXIMA_PT = 80;
const LARGURA_ETIQUETA_MINIMA_PX = 60;
const ALTURA_ETIQUETA_MINIMA_PX = 30;

const form = document.getElementById('formAssinatura');
const arquivoInput = document.getElementById('arquivoInput');
const nomeArquivoEl = document.getElementById('nomeArquivo');
const botaoAssinar = document.getElementById('botaoAssinar');
const mensagemErroEl = document.getElementById('mensagemErro');
const cartaoEl = document.querySelector('.cartao');

const areaPreview = document.getElementById('areaPreview');
const previewContainer = document.getElementById('previewContainer');
const canvas = document.getElementById('previewCanvas');
const etiqueta = document.getElementById('etiquetaArrastavel');
const alcaRedimensionar = document.getElementById('alcaRedimensionar');
const indicadorPagina = document.getElementById('indicadorPagina');
const botaoPaginaAnterior = document.getElementById('botaoPaginaAnterior');
const botaoProximaPagina = document.getElementById('botaoProximaPagina');

let pdfDocument = null;
let paginaAtual = 1;
let escalaRenderizacao = 1;
let larguraPaginaPt = 0;
let alturaPaginaPt = 0;
let previewDisponivel = false;

function formatarTamanho(bytes) {
	if (bytes < 1024) {
		return bytes + ' B';
	}
	if (bytes < 1024 * 1024) {
		return (bytes / 1024).toFixed(1) + ' KB';
	}
	return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function limparErro() {
	mensagemErroEl.hidden = true;
	mensagemErroEl.textContent = '';
}

function mostrarErro(mensagem) {
	mensagemErroEl.textContent = mensagem;
	mensagemErroEl.hidden = false;
}

function limparSelecao() {
	arquivoInput.value = '';
	nomeArquivoEl.textContent = 'Nenhum arquivo escolhido';
	botaoAssinar.disabled = true;
	esconderPreview();
}

function esconderPreview() {
	areaPreview.hidden = true;
	cartaoEl.classList.remove('cartao--com-preview');
	previewDisponivel = false;
	pdfDocument = null;
}

async function carregarPreview(arquivo) {
	try {
		const bytes = await arquivo.arrayBuffer();
		pdfDocument = await pdfjsLib.getDocument({ data: bytes }).promise;
		paginaAtual = pdfDocument.numPages;

		areaPreview.hidden = false;
		cartaoEl.classList.add('cartao--com-preview');
		previewDisponivel = true;

		await renderizarPagina(paginaAtual);
	} catch (erro) {
		// Preview é um recurso auxiliar: se o PDF não puder ser pré-visualizado
		// no navegador (ex.: arquivo protegido por senha), a assinatura ainda
		// segue normalmente usando a posição padrão do servidor.
		esconderPreview();
	}
}

async function renderizarPagina(numero) {
	const pagina = await pdfDocument.getPage(numero);
	const viewportBase = pagina.getViewport({ scale: 1 });
	larguraPaginaPt = viewportBase.width;
	alturaPaginaPt = viewportBase.height;

	escalaRenderizacao = Math.min(LARGURA_MAXIMA_CANVAS / larguraPaginaPt, 1.5);
	const viewport = pagina.getViewport({ scale: escalaRenderizacao });

	canvas.width = viewport.width;
	canvas.height = viewport.height;
	const contexto = canvas.getContext('2d');
	await pagina.render({ canvasContext: contexto, viewport }).promise;

	posicionarEtiquetaPadrao();
	atualizarNavegacao();
}

function posicionarEtiquetaPadrao() {
	const larguraBoxPt = Math.min(LARGURA_ETIQUETA_MAXIMA_PT, larguraPaginaPt * 0.5);
	const alturaBoxPt = Math.min(ALTURA_ETIQUETA_MAXIMA_PT, alturaPaginaPt * 0.25);
	const xPt = larguraPaginaPt - larguraBoxPt - MARGEM_PADRAO_PT;
	const yPt = MARGEM_PADRAO_PT;

	definirRetanguloEtiqueta({
		larguraPx: larguraBoxPt * escalaRenderizacao,
		alturaPx: alturaBoxPt * escalaRenderizacao,
		esquerdaPx: xPt * escalaRenderizacao,
		topoPx: (alturaPaginaPt - yPt - alturaBoxPt) * escalaRenderizacao
	});
}

function definirRetanguloEtiqueta({ larguraPx, alturaPx, esquerdaPx, topoPx }) {
	etiqueta.style.width = larguraPx + 'px';
	etiqueta.style.height = alturaPx + 'px';
	etiqueta.style.left = esquerdaPx + 'px';
	etiqueta.style.top = topoPx + 'px';
}

function atualizarNavegacao() {
	indicadorPagina.textContent = `Página ${paginaAtual} de ${pdfDocument.numPages}`;
	botaoPaginaAnterior.disabled = paginaAtual <= 1;
	botaoProximaPagina.disabled = paginaAtual >= pdfDocument.numPages;
}

botaoPaginaAnterior.addEventListener('click', () => {
	if (paginaAtual > 1) {
		paginaAtual--;
		renderizarPagina(paginaAtual);
	}
});

botaoProximaPagina.addEventListener('click', () => {
	if (pdfDocument && paginaAtual < pdfDocument.numPages) {
		paginaAtual++;
		renderizarPagina(paginaAtual);
	}
});

function iniciarArraste(evento) {
	if (evento.target === alcaRedimensionar) {
		return;
	}
	evento.preventDefault();

	const retanguloContainer = previewContainer.getBoundingClientRect();
	const retanguloEtiqueta = etiqueta.getBoundingClientRect();
	const deslocamentoX = evento.clientX - retanguloEtiqueta.left;
	const deslocamentoY = evento.clientY - retanguloEtiqueta.top;

	function mover(eventoMovimento) {
		const larguraPx = retanguloEtiqueta.width;
		const alturaPx = retanguloEtiqueta.height;
		let esquerdaPx = eventoMovimento.clientX - retanguloContainer.left - deslocamentoX;
		let topoPx = eventoMovimento.clientY - retanguloContainer.top - deslocamentoY;

		esquerdaPx = Math.max(0, Math.min(esquerdaPx, canvas.width - larguraPx));
		topoPx = Math.max(0, Math.min(topoPx, canvas.height - alturaPx));

		etiqueta.style.left = esquerdaPx + 'px';
		etiqueta.style.top = topoPx + 'px';
	}

	function pararMovimento() {
		document.removeEventListener('pointermove', mover);
		document.removeEventListener('pointerup', pararMovimento);
	}

	document.addEventListener('pointermove', mover);
	document.addEventListener('pointerup', pararMovimento);
}

function iniciarRedimensionamento(evento) {
	evento.preventDefault();
	evento.stopPropagation();

	const retanguloContainer = previewContainer.getBoundingClientRect();
	const retanguloEtiqueta = etiqueta.getBoundingClientRect();
	const esquerdaPx = retanguloEtiqueta.left - retanguloContainer.left;
	const topoPx = retanguloEtiqueta.top - retanguloContainer.top;

	function redimensionar(eventoMovimento) {
		let novaLarguraPx = eventoMovimento.clientX - retanguloContainer.left - esquerdaPx;
		let novaAlturaPx = eventoMovimento.clientY - retanguloContainer.top - topoPx;

		novaLarguraPx = Math.max(LARGURA_ETIQUETA_MINIMA_PX, Math.min(novaLarguraPx, canvas.width - esquerdaPx));
		novaAlturaPx = Math.max(ALTURA_ETIQUETA_MINIMA_PX, Math.min(novaAlturaPx, canvas.height - topoPx));

		etiqueta.style.width = novaLarguraPx + 'px';
		etiqueta.style.height = novaAlturaPx + 'px';
	}

	function pararRedimensionamento() {
		document.removeEventListener('pointermove', redimensionar);
		document.removeEventListener('pointerup', pararRedimensionamento);
	}

	document.addEventListener('pointermove', redimensionar);
	document.addEventListener('pointerup', pararRedimensionamento);
}

etiqueta.addEventListener('pointerdown', iniciarArraste);
alcaRedimensionar.addEventListener('pointerdown', iniciarRedimensionamento);

function calcularPosicaoEscolhida() {
	const esquerdaPx = parseFloat(etiqueta.style.left);
	const topoPx = parseFloat(etiqueta.style.top);
	const larguraPx = parseFloat(etiqueta.style.width);
	const alturaPx = parseFloat(etiqueta.style.height);

	const xPt = esquerdaPx / escalaRenderizacao;
	const larguraPt = larguraPx / escalaRenderizacao;
	const alturaPt = alturaPx / escalaRenderizacao;
	const yPt = alturaPaginaPt - (topoPx / escalaRenderizacao) - alturaPt;

	return { pagina: paginaAtual, x: xPt, y: yPt, width: larguraPt, height: alturaPt };
}

arquivoInput.addEventListener('change', () => {
	limparErro();
	const arquivo = arquivoInput.files[0];

	if (!arquivo) {
		limparSelecao();
		return;
	}

	const nomeValido = arquivo.name.toLowerCase().endsWith('.pdf');
	const tipoValido = arquivo.type === 'application/pdf' || arquivo.type === '';

	if (!nomeValido || !tipoValido) {
		limparSelecao();
		mostrarErro('Selecione um arquivo PDF válido.');
		return;
	}

	nomeArquivoEl.textContent = `${arquivo.name} (${formatarTamanho(arquivo.size)})`;
	botaoAssinar.disabled = false;
	carregarPreview(arquivo);
});

form.addEventListener('submit', async (evento) => {
	evento.preventDefault();
	limparErro();

	const arquivo = arquivoInput.files[0];
	if (!arquivo) {
		return;
	}

	botaoAssinar.disabled = true;
	const textoOriginalBotao = botaoAssinar.textContent;
	botaoAssinar.textContent = 'Assinando...';

	try {
		const dadosFormulario = new FormData();
		dadosFormulario.append('arquivo', arquivo);

		if (previewDisponivel) {
			const posicao = calcularPosicaoEscolhida();
			dadosFormulario.append('pagina', posicao.pagina);
			dadosFormulario.append('x', posicao.x);
			dadosFormulario.append('y', posicao.y);
			dadosFormulario.append('width', posicao.width);
			dadosFormulario.append('height', posicao.height);
		}

		const resposta = await fetch('/api/assinaturas', {
			method: 'POST',
			body: dadosFormulario
		});

		if (!resposta.ok) {
			const contentType = resposta.headers.get('Content-Type') || '';
			if (contentType.includes('application/json')) {
				const erro = await resposta.json();
				mostrarErro(erro.mensagem || 'Não foi possível realizar a assinatura digital.');
			} else {
				mostrarErro('Não foi possível realizar a assinatura digital.');
			}
			return;
		}

		const blob = await resposta.blob();
		const disposition = resposta.headers.get('Content-Disposition') || '';
		const correspondencia = disposition.match(/filename="?([^"]+)"?/);
		const nomeArquivoSaida = correspondencia ? correspondencia[1] : 'documento_assinado.pdf';

		const url = window.URL.createObjectURL(blob);
		const link = document.createElement('a');
		link.href = url;
		link.download = nomeArquivoSaida;
		document.body.appendChild(link);
		link.click();
		link.remove();
		window.URL.revokeObjectURL(url);

		form.reset();
		limparSelecao();
	} catch (erro) {
		mostrarErro('Não foi possível conectar ao servidor. Tente novamente.');
	} finally {
		botaoAssinar.textContent = textoOriginalBotao;
		botaoAssinar.disabled = arquivoInput.files.length === 0;
	}
});
