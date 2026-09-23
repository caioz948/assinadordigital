# Assinador Digital de Documentos - UTFPR

Sprint #1: primeira versão funcional do assinador digital de PDFs. Permite que
um usuário envie um arquivo PDF, o sistema realize uma **assinatura digital
criptográfica real** (CMS/PKCS#7, incorporada ao PDF) com uma **representação
visual** (etiqueta) no documento, e disponibilize o PDF assinado para download.

> ⚠️ **Importante**: o certificado de desenvolvimento incluído no projeto é
> **autoassinado** e serve apenas para testes locais. Ele **não possui o
> mesmo valor jurídico de um certificado ICP-Brasil**. Para uso em produção é
> necessário configurar um certificado digital válido (ICP-Brasil ou outra
> Autoridade Certificadora reconhecida).

## 1. Tecnologias utilizadas

- Java 21
- Apache Maven
- Spring Boot 4.0.8 (Spring Framework 7)
- Apache PDFBox 3.0.8 - manipulação e assinatura de PDF (Apache License 2.0)
- Bouncy Castle (`bcprov-jdk18on` / `bcpkix-jdk18on` 1.86) - assinatura
  criptográfica CMS/PKCS#7 (Bouncy Castle License, permissiva)
- HTML/CSS/JavaScript puro (sem framework de frontend) servidos como recursos
  estáticos do próprio Spring Boot
- PDF.js (`pdfjs-dist` 6.3.289, Apache License 2.0) - renderiza a prévia da
  página no navegador para o usuário posicionar a etiqueta antes de assinar;
  vendorizado localmente em `static/vendor/pdfjs/` (sem depender de CDN em
  runtime)

## 2. Requisitos

- JDK 21
- Apache Maven 3.9+

O projeto foi validado nesta máquina com **Microsoft Build of OpenJDK 21**
(`21.0.12.1`) e **Apache Maven 3.9.16**: `mvn clean install` compila o
código, e `mvn spring-boot:run` sobe a aplicação normalmente servindo a
página do Assinador Digital em `http://localhost:8080`.

## 3. Estrutura do projeto

```
src/main/java/br/edu/utfpr/td/tsi/assinadordigital/
├── Main.java                          # ponto de entrada Spring Boot
├── controller/
│   ├── DocumentoController.java       # POST /api/v1/documentos/upload
│   └── AssinaturaController.java      # POST /api/assinaturas
├── service/
│   ├── DocumentoService.java          # recebe e valida o PDF (ponto único de validação)
│   ├── AssinaturaService.java         # orquestra a assinatura (valida via DocumentoService, chama os demais)
│   ├── PdfService.java                # valida o conteúdo/assina o PDF e monta a etiqueta visual
│   ├── CertificadoService.java        # carrega o certificado/keystore configurado
│   ├── PdfSignatureService.java       # assinatura criptográfica CMS/PKCS#7 (Bouncy Castle)
│   ├── MaterialAssinatura.java        # chave privada + cadeia de certificados
│   ├── PosicaoAssinatura.java         # posição efetiva (já resolvida) da etiqueta
│   ├── PosicaoAssinaturaRequest.java  # posição opcional vinda do formulário (override)
│   └── ResultadoAssinatura.java       # bytes do PDF assinado + nome de arquivo
├── dto/
│   ├── DocumentoDTO.java              # dados do PDF validado (resposta do upload)
│   └── ErroResponse.java              # formato padrão de resposta de erro
├── config/
│   ├── AssinaturaKeystoreProperties.java  # signature.keystore.*
│   ├── PosicaoAssinaturaProperties.java   # assinatura.posicao.* (página/x/y/largura/altura)
│   └── UploadProperties.java              # assinatura.upload.*
└── exception/
    ├── ArquivoInvalidoException.java  # 400
    ├── PdfInvalidoException.java      # 422
    ├── CertificadoException.java      # 500
    ├── AssinaturaDigitalException.java# 500
    └── GlobalExceptionHandler.java    # @RestControllerAdvice centralizado

src/main/resources/
├── application.properties
├── certs/dev-keystore.p12             # certificado de DESENVOLVIMENTO (fake, sem valor jurídico)
└── static/
    ├── index.html                     # página única: envio, validação, preview e assinatura
    ├── css/index.css
    ├── js/assinar.js                  # lógica da página (upload, preview com PDF.js, assinatura)
    └── vendor/pdfjs/                  # PDF.js vendorizado
```

Fluxos:

- **Validação**: `DocumentoController` → `DocumentoService` (arquivo vazio,
  tamanho, extensão, Content-Type) → `PdfService.validarConteudo` (assinatura
  `%PDF-`, abertura com PDFBox, ao menos 1 página) → `DocumentoDTO`.
- **Assinatura**: `AssinaturaController` → `AssinaturaService` →
  `DocumentoService` (mesma validação acima) → `PdfService` (monta a etiqueta
  visual e o campo de assinatura) → `PdfSignatureService` (gera a assinatura
  CMS/PKCS#7 usando o material fornecido por `CertificadoService`).

Como os dois fluxos validam pelo `DocumentoService`, um arquivo aceito no
envio é sempre aceito na assinatura, e vice-versa.

## 4. Como configurar o certificado de desenvolvimento

O projeto já inclui um certificado de teste em
`src/main/resources/certs/dev-keystore.p12` (PKCS#12, alias `assinador-dev`,
senha `changeit`), usado por padrão. Para gerar um novo, caso necessário:

```bash
keytool -genkeypair -v \
  -alias assinador-dev \
  -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -validity 3650 \
  -keystore dev-keystore.p12 -storetype PKCS12 -storepass changeit -keypass changeit \
  -dname "CN=Assinador Digital UTFPR (Certificado de Desenvolvimento), OU=TSI, O=UTFPR, L=Medianeira, ST=PR, C=BR"
```

Novamente: este certificado autoassinado é **apenas para desenvolvimento e
testes** - leitores de PDF o exibirão como "assinatura não confiável" (não
possui cadeia até uma Autoridade Certificadora raiz confiável), o que é o
comportamento esperado.

## 5. Variáveis de ambiente

Nenhuma senha ou dado sensível está no código-fonte; tudo é resolvido via
`application.properties` + variáveis de ambiente (com defaults apontando para
o certificado de desenvolvimento):

| Variável | Descrição | Padrão |
|---|---|---|
| `SIGNATURE_KEYSTORE_PATH` | Caminho do keystore (`classpath:...` ou `file:...`) | `classpath:certs/dev-keystore.p12` |
| `SIGNATURE_KEYSTORE_PASSWORD` | Senha do keystore | `changeit` |
| `SIGNATURE_KEYSTORE_ALIAS` | Alias do certificado | `assinador-dev` |
| `SIGNATURE_KEYSTORE_TYPE` | Tipo do keystore | `PKCS12` |
| `ASSINATURA_POSICAO_PAGINA` | Página da etiqueta (-1 = última página) | `-1` |
| `ASSINATURA_POSICAO_X` / `_Y` / `_WIDTH` / `_HEIGHT` | Posição/tamanho padrão da etiqueta em pontos (pt). `x`/`y` são a distância a partir do canto **inferior esquerdo** da página até o canto inferior esquerdo da etiqueta (convenção padrão de PDF) | `350` / `100` / `200` / `80` |
| `ASSINATURA_UPLOAD_MAX_SIZE` | Limite de upload (Spring) | `10MB` |
| `ASSINATURA_UPLOAD_MAX_SIZE_BYTES` | Limite de upload (regra de negócio) | `10485760` (10MB) |
| `SERVER_PORT` | Porta HTTP | `8080` |

## 6. Como executar

```bash
mvn clean install
mvn spring-boot:run
```

Acesse `http://localhost:8080` para abrir a página do **Assinador Digital**.

## 7. Como utilizar a API

### 7.1. Enviar e validar um PDF

```
POST /api/v1/documentos/upload
Content-Type: multipart/form-data

arquivo=<arquivo.pdf>                    (obrigatório)
```

- **Sucesso (200)**: corpo JSON com os dados do documento:
  ```json
  {
    "nomeArquivo": "documento.pdf",
    "tipoConteudo": "application/pdf",
    "tamanhoBytes": 26051,
    "quantidadePaginas": 11,
    "mensagem": "PDF carregado e validado com sucesso!"
  }
  ```
- **Erro (400/422)**: mesmo formato de erro da assinatura (ver abaixo).

### 7.2. Assinar um PDF

```
POST /api/assinaturas
Content-Type: multipart/form-data

arquivo=<arquivo.pdf>                    (obrigatório)
nomeAssinante=<opcional>                 nome exibido na assinatura
pagina=<opcional>                        página onde a etiqueta é inserida (1-based)
x=<opcional>                             distância a partir da esquerda da página (pt)
y=<opcional>                             distância a partir de baixo da página (pt)
width=<opcional>                         largura da etiqueta (pt)
height=<opcional>                        altura da etiqueta (pt)
```

`pagina`/`x`/`y`/`width`/`height` são opcionais e, juntos, permitem
sobrescrever a posição padrão configurada no servidor (`assinatura.posicao.*`)
para uma assinatura específica - é o que a página "Assinar PDF" usa ao
enviar a posição escolhida pelo usuário ao arrastar/redimensionar a etiqueta
no preview. Se `x`/`y`/`width`/`height` não vierem todos preenchidos, o
servidor ignora o override e usa a posição padrão. Coordenadas fora dos
limites da página são ajustadas automaticamente pelo servidor.

- **Sucesso (200)**: corpo é o PDF assinado (`Content-Type: application/pdf`,
  `Content-Disposition: attachment; filename="documento_assinado.pdf"`).
- **Erro (400/422/500)**: corpo JSON no formato:
  ```json
  { "sucesso": false, "mensagem": "Formato inválido. O arquivo enviado deve ser um PDF." }
  ```

| Status | Quando ocorre |
|---|---|
| 400 | arquivo ausente/vazio, extensão/Content-Type inválido, conteúdo que não é PDF, tamanho excedido |
| 422 | PDF com extensão válida mas corrompido/não processável |
| 500 | falha de certificado ou falha ao gerar a assinatura digital |

## 8. Como realizar um teste de assinatura manual

```bash
curl -X POST http://localhost:8080/api/assinaturas \
  -F "arquivo=@/caminho/para/documento.pdf" \
  -o documento_assinado.pdf
```

Ou pela interface web, em `http://localhost:8080`:

1. Selecione um PDF e clique em **"Enviar PDF"**: o servidor valida o arquivo
   e a página mostra nome, tipo, tamanho e quantidade de páginas.
2. Uma prévia da página é exibida com uma etiqueta que pode ser **arrastada e
   redimensionada** com o mouse (é possível navegar entre as páginas do
   documento).
3. Clique em **"Assinar PDF"**: o download do PDF assinado inicia
   automaticamente, com a etiqueta na posição escolhida.

Se a prévia não puder ser carregada (ex.: PDF protegido por senha), o botão
"Assinar PDF" continua disponível e a assinatura usa a posição padrão do
servidor.

## 9. Como verificar a assinatura em um leitor compatível

1. Abra o `documento_assinado.pdf` no **Adobe Acrobat Reader** (ou outro
   leitor compatível com assinaturas PDF/CMS).
2. Vá até o painel de assinaturas (em geral, um aviso/ícone já aparece no
   topo do documento indicando que ele foi assinado digitalmente).
3. O leitor mostrará os detalhes do certificado utilizado. Como o certificado
   de desenvolvimento é autoassinado, o leitor indicará que a **identidade do
   assinante não pôde ser verificada** (cadeia de confiança desconhecida) -
   isso é esperado para um certificado de teste, mas o leitor deve confirmar
   que **o documento não foi alterado após a assinatura**.
4. Para testar a detecção de alterações: edite/salve o PDF assinado em
   qualquer editor e reabra-o - o leitor deve indicar que o documento foi
   modificado após a assinatura.

## 10. Limitações atuais (Sprint #1)

- Não há autenticação/usuários: o campo `nomeAssinante` é opcional (via
  formulário) e, se omitido, o nome (CN) do certificado configurado é utilizado.
- O editor visual de posicionamento (arrastar/redimensionar a etiqueta no
  preview, com navegação entre páginas) já existe na página inicial,
  mas ainda é por página única de cada vez - não há, por exemplo, múltiplas
  etiquetas na mesma assinatura.
- O certificado de desenvolvimento não possui validade jurídica; produção
  exige um certificado real (ICP-Brasil ou equivalente).
- Não há suíte de testes automatizados no projeto atualmente.

## 11. Próximos passos possíveis

- Suporte a múltiplos assinantes por documento.
- Integração com certificados ICP-Brasil (A1/A3, cartão/token).
- Histórico/consulta de documentos assinados (persistência em banco de dados).
- Autenticação de usuários e vínculo do documento à conta do usuário.
- Página de "Verificar Assinaturas" (upload de um PDF já assinado para
  conferir quem assinou e se a assinatura ainda é válida), inspirada no
  Assinador ICPEdu da RNP (https://cdd.icpedu.rnp.br/).
- Autenticação institucional federada (CAFe) e certificado pessoal por
  usuário (em vez de um único certificado compartilhado), também nos moldes
  do ICPEdu - ficaria a critério de uma sprint futura, já que exigiria
  integração com identidade federada (SAML/OAuth) fora do escopo atual.
