const form = document.getElementById('verificarForm');
const arquivoInput = document.getElementById('arquivo');
const btnVerificar = document.getElementById('btnVerificar');
const divResultado = document.getElementById('resultado');
const listaAssinaturas = document.getElementById('listaAssinaturas');

const SITUACOES = {
	VALIDA: { classe: 'sucesso', rotulo: '✅ Assinatura válida' },
	VALIDA_COM_ALTERACOES_POSTERIORES: { classe: 'aviso', rotulo: '⚠️ Válida, com acréscimos posteriores' },
	INVALIDA: { classe: 'erro', rotulo: '❌ Assinatura inválida' }
};

// Todos os textos vindos do servidor ou do arquivo são inseridos com
// textContent, para não serem interpretados como HTML.
function criarElemento(tag, classe, texto) {
	const elemento = document.createElement(tag);
	if (classe) {
		elemento.className = classe;
	}
	if (texto !== undefined && texto !== null) {
		elemento.textContent = texto;
	}
	return elemento;
}

function criarLinha(rotulo, valor) {
	const linha = criarElemento('div', 'info-item');
	linha.appendChild(criarElemento('strong', null, rotulo));
	linha.append(' ' + (valor ?? '—'));
	return linha;
}

function formatarData(iso) {
	if (!iso) {
		return null;
	}
	return new Date(iso).toLocaleString('pt-BR');
}

function mostrarResultado(classe, titulo, texto) {
	divResultado.className = classe;
	divResultado.replaceChildren(criarElemento('strong', null, titulo));
	if (texto) {
		divResultado.append(' ' + texto);
	}
	divResultado.style.display = 'block';
}

function limpar() {
	divResultado.style.display = 'none';
	divResultado.replaceChildren();
	listaAssinaturas.replaceChildren();
}

function criarCartaoAssinatura(assinatura, indice) {
	const situacao = SITUACOES[assinatura.situacao] || SITUACOES.INVALIDA;
	const cartao = criarElemento('div', 'cartao-assinatura ' + situacao.classe);

	cartao.appendChild(criarElemento('div', 'cartao-assinatura-titulo', `${indice + 1}. ${situacao.rotulo}`));
	cartao.appendChild(criarElemento('p', 'cartao-assinatura-mensagem', assinatura.mensagem));
	cartao.appendChild(criarLinha('Assinado por:', assinatura.assinante));
	cartao.appendChild(criarLinha('Certificado emitido por:', assinatura.emissorCertificado));
	cartao.appendChild(criarLinha('Data da assinatura:', formatarData(assinatura.dataAssinatura)));
	if (assinatura.motivo) {
		cartao.appendChild(criarLinha('Motivo:', assinatura.motivo));
	}

	if (assinatura.avisos && assinatura.avisos.length) {
		const lista = criarElemento('ul', 'cartao-assinatura-avisos');
		assinatura.avisos.forEach(aviso => lista.appendChild(criarElemento('li', null, aviso)));
		cartao.appendChild(lista);
	}
	return cartao;
}

arquivoInput.addEventListener('change', limpar);

form.addEventListener('submit', async (evento) => {
	evento.preventDefault();

	const arquivo = arquivoInput.files[0];
	if (!arquivo) {
		mostrarResultado('erro', '❌ Erro:', 'Por favor, selecione um arquivo PDF.');
		return;
	}

	limpar();
	btnVerificar.disabled = true;
	btnVerificar.textContent = 'Verificando...';

	try {
		const formData = new FormData();
		formData.append('arquivo', arquivo);

		const resposta = await fetch('/api/v1/documentos/verificar', {
			method: 'POST',
			body: formData
		});

		if (!resposta.ok) {
			const contentType = resposta.headers.get('Content-Type') || '';
			const mensagem = contentType.includes('application/json')
				? (await resposta.json()).mensagem
				: null;
			mostrarResultado('erro', '❌ Erro:', mensagem || 'Não foi possível verificar o arquivo.');
			return;
		}

		const dados = await resposta.json();
		let classe = 'sucesso';
		if (dados.quantidadeAssinaturas === 0) {
			classe = 'aviso';
		} else if (dados.quantidadeValidas < dados.quantidadeAssinaturas) {
			classe = 'erro';
		}

		const titulo = dados.quantidadeAssinaturas === 1
			? 'Existe 1 assinatura no documento.'
			: `Existem ${dados.quantidadeAssinaturas} assinaturas no documento.`;
		mostrarResultado(classe, dados.quantidadeAssinaturas === 0 ? dados.mensagem : titulo,
			dados.quantidadeAssinaturas === 0 ? null : dados.mensagem);

		dados.assinaturas.forEach((assinatura, indice) => {
			listaAssinaturas.appendChild(criarCartaoAssinatura(assinatura, indice));
		});
	} catch (erro) {
		mostrarResultado('erro', '❌ Erro de Conexão:', 'Não foi possível conectar ao servidor.');
	} finally {
		btnVerificar.disabled = false;
		btnVerificar.textContent = 'Verificar documento';
	}
});
