import * as pdfjsLib from '/vendor/pdfjs/pdf.min.mjs';

pdfjsLib.GlobalWorkerOptions.workerSrc = '/vendor/pdfjs/pdf.worker.min.mjs';

const LARGURA_MAXIMA_CANVAS = 560;
const MARGEM_PADRAO_PT = 24;
const LARGURA_ETIQUETA_MAXIMA_PT = 200;
const ALTURA_ETIQUETA_MAXIMA_PT = 80;
const LARGURA_ETIQUETA_MINIMA_PX = 60;
const ALTURA_ETIQUETA_MINIMA_PX = 30;

const containerEl = document.getElementById('container');
const form = document.getElementById('uploadForm');
const arquivoInput = document.getElementById('arquivo');
const btnCarregarAssinar = document.getElementById('btnCarregarAssinar');
const btnVerificarAssinatura = document.getElementById('btnVerificarAssinatura');
const divResultado = document.getElementById('resultado');

const areaAssinatura = document.getElementById('areaAssinatura');
const btnAssinar = document.getElementById('btnAssinar');
const areaPreview = document.getElementById('areaPreview');
const previewContainer = document.getElementById('previewContainer');
const canvas = document.getElementById('previewCanvas');
const etiqueta = document.getElementById('etiquetaArrastavel');
const alcaRedimensionar = document.getElementById('alcaRedimensionar');
const indicadorPagina = document.getElementById('indicadorPagina');
const botaoPaginaAnterior = document.getElementById('botaoPaginaAnterior');
const botaoProximaPagina = document.getElementById('botaoProximaPagina');

let arquivoValidado = null;
let pdfDocument = null;
let paginaAtual = 1;
let escalaRenderizacao = 1;
let larguraPaginaPt = 0;
let alturaPaginaPt = 0;
let previewDisponivel = false;

function criarLinha(rotulo, valor) {
	const linha = document.createElement('div');
	linha.className = 'info-item';
	const forte = document.createElement('strong');
	forte.textContent = rotulo;
	linha.appendChild(forte);
	if (valor !== null && valor !== undefined) {
		linha.append(' ' + valor);
	}
	return linha;
}

function mostrarResultado(classe, ...linhas) {
	divResultado.className = classe;
	divResultado.replaceChildren(...linhas);
	divResultado.style.display = 'block';
}

function esconderResultado() {
	divResultado.style.display = 'none';
	divResultado.replaceChildren();
}

function mostrarErro(mensagem) {
	mostrarResultado('erro', criarLinha('❌ Erro:', mensagem));
}

async function lerMensagemErro(resposta, mensagemPadrao) {
	const contentType = resposta.headers.get('Content-Type') || '';
	if (contentType.includes('application/json')) {
		const erro = await resposta.json();
		return erro.mensagem || mensagemPadrao;
	}
	return mensagemPadrao;
}

function limparAssinatura() {
	arquivoValidado = null;
	areaAssinatura.hidden = true;
	esconderPreview();
}

arquivoInput.addEventListener('change', () => {
	esconderResultado();
	limparAssinatura();
});

// Ação 1: Botão Carregar / Preparar para Assinar
btnCarregarAssinar.addEventListener('click', async () => {
	const arquivo = arquivoInput.files[0];
	if (!arquivo) {
		mostrarErro('Por favor, selecione um arquivo PDF.');
		return;
	}

	limparAssinatura();
	esconderResultado();
	btnCarregarAssinar.disabled = true;
	btnCarregarAssinar.textContent = 'Processando...';

	try {
		const formData = new FormData();
		formData.append('arquivo', arquivo);

		const resposta = await fetch('/api/v1/documentos/upload', {
			method: 'POST',
			body: formData
		});

		if (!resposta.ok) {
			mostrarErro(await lerMensagemErro(resposta, 'Não foi possível processar o arquivo.'));
			return;
		}

		const dados = await resposta.json();
		mostrarResultado('sucesso',
			criarLinha('✅ ' + dados.mensagem),
			document.createElement('br'),
			criarLinha('Nome:', dados.nomeArquivo),
			criarLinha('Tipo:', dados.tipoConteudo),
			criarLinha('Tamanho:', (dados.tamanhoBytes / 1024).toFixed(2) + ' KB'),
			criarLinha('Páginas:', dados.quantidadePaginas)
		);

		arquivoValidado = arquivo;
		areaAssinatura.hidden = false;
		await carregarPreview(arquivo);
	} catch (erro) {
		mostrarResultado('erro', criarLinha('❌ Erro de Conexão:', 'Não foi possível conectar ao servidor.'));
	} finally {
		btnCarregarAssinar.disabled = false;
		btnCarregarAssinar.textContent = 'Assinar PDF';
	}
});

// Ação 2: Botão Verificar Assinatura Digital
btnVerificarAssinatura.addEventListener('click', async () => {
	const arquivo = arquivoInput.files[0];
	if (!arquivo) {
		mostrarErro('Por favor, selecione um arquivo PDF para verificar.');
		return;
	}

	limparAssinatura();
	esconderResultado();
	btnVerificarAssinatura.disabled = true;
	btnVerificarAssinatura.textContent = 'Verificando...';

	try {
		const formData = new FormData();
		formData.append('arquivo', arquivo);

		const resposta = await fetch('/api/v1/documentos/verificar', {
			method: 'POST',
			body: formData
		});

		if (!resposta.ok) {
			mostrarErro(await lerMensagemErro(resposta, 'Não foi possível verificar as assinaturas do arquivo.'));
			return;d
		}

		const assinaturas = await resposta.json();

		if (!assinaturas || assinaturas.length === 0) {
			mostrarResultado('erro', 
				criarLinha('⚠️ Atenção:', 'Este documento NÃO possui nenhuma assinatura digital.'),
				document.createElement('br'),
				criarLinha('Detalhe:', 'O arquivo enviado não contém blocos de assinatura válidos.')
			);
			return;
		}

		// Monta o relatório com todas as assinaturas encontradas no PDF
		const elementosResultado = [criarLinha('🔍 Relatório de Verificação de Assinaturas:')];
		elementosResultado.push(document.createElement('br'));

		let todasValidas = true;

		assinaturas.forEach((sig, index) => {
			const statusTexto = sig.valido ? 'VÁLIDA (Íntegra)' : 'INVÁLIDA (Modificado ou Corrompido)';
			if (!sig.valido) todasValidas = false;

			elementosResultado.push(criarLinha(`Assinatura #${index + 1}:`, ''));
			elementosResultado.push(criarLinha(' • Signatário:', sig.nomeSignatario));
			elementosResultado.push(criarLinha(' • Data:', sig.dataAssinatura));
			elementosResultado.push(criarLinha(' • Razão:', sig.razao || 'Não informada'));
			elementosResultado.push(criarLinha(' • Status:', statusTexto));
			elementosResultado.push(document.createElement('br'));
		});

		if (todasValidas) {
			mostrarResultado('sucesso', ...elementosResultado);
		} else {
			mostrarResultado('erro', 
				criarLinha('❌ Alerta de Integridade:', 'O documento foi modificado após ser assinado ou contém assinaturas corrompidas!'),
				document.createElement('br'),
				...elementosResultado
			);
		}

	} catch (erro) {
		mostrarResultado('erro', criarLinha('❌ Erro de Conexão:', 'Não foi possível conectar ao servidor para verificação.'));
	} finally {
		btnVerificarAssinatura.disabled = false;
		btnVerificarAssinatura.textContent = 'Verificar Assinatura';
	}
});

function esconderPreview() {
	areaPreview.hidden = true;
	containerEl.classList.remove('container--com-preview');
	previewDisponivel = false;
	pdfDocument = null;
}

async function carregarPreview(arquivo) {
	try {
		const bytes = await arquivo.arrayBuffer();
		pdfDocument = await pdfjsLib.getDocument({ data: bytes }).promise;
		paginaAtual = pdfDocument.numPages;

		areaPreview.hidden = false;
		containerEl.classList.add('container--com-preview');
		previewDisponivel = true;

		await renderizarPagina(paginaAtual);
	} catch (erro) {
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

btnAssinar.addEventListener('click', async () => {
	if (!arquivoValidado) {
		return;
	}

	btnAssinar.disabled = true;
	const textoOriginalBotao = btnAssinar.textContent;
	btnAssinar.textContent = 'Assinando...';

	try {
		const dadosFormulario = new FormData();
		dadosFormulario.append('arquivo', arquivoValidado);

		if (previewDisponivel) {
			const posicao = calcularPosicaoEscolhida();
			dadosFormulario.append('pagina', posicao.pagina);
			dadosFormulario.append('x', posicao.x);
			dadosFormulario.append('y', posicao.y);
			dadosFormulario.append('width', posicao.width);
			dadosFormulario.append('height', posicao.height);
		}

		const resposta = await fetch('/api/v1/assinaturas', {
			method: 'POST',
			body: dadosFormulario
		});

		if (!resposta.ok) {
			mostrarErro(await lerMensagemErro(resposta, 'Não foi possível realizar a assinatura digital.'));
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
		limparAssinatura();
		mostrarResultado('sucesso', criarLinha('✅ PDF assinado com sucesso!', 'Download de ' + nomeArquivoSaida + ' iniciado.'));
	} catch (erro) {
		mostrarResultado('erro', criarLinha('❌ Erro de Conexão:', 'Não foi possível conectar ao servidor.'));
	} finally {
		btnAssinar.textContent = textoOriginalBotao;
		btnAssinar.disabled = false;
	}
});