package br.edu.utfpr.td.tsi.assinadordigital.service;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import br.edu.utfpr.td.tsi.assinadordigital.dto.DocumentoDTO;

@Service
public class DocumentoService {

    public DocumentoDTO processarEValidarPdf(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo foi selecionado para envio.");
        }

        String nomeArquivo = arquivo.getOriginalFilename();
        if (nomeArquivo == null || !nomeArquivo.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Formato inválido. O arquivo enviado deve ser um PDF.");
        }

        // Passando o InputStream do MultipartFile diretamente para o RandomAccessReadBuffer
        try (RandomAccessReadBuffer buffer = new RandomAccessReadBuffer(arquivo.getInputStream());
             PDDocument document = Loader.loadPDF(buffer)) {
            
            int quantidadePaginas = document.getNumberOfPages();

            return new DocumentoDTO(
                    nomeArquivo,
                    arquivo.getContentType(),
                    arquivo.getSize(),
                    quantidadePaginas,
                    "PDF carregado e validado com sucesso!"
            );
        } catch (IOException e) {
            throw new IllegalArgumentException("Erro ao processar a estrutura do PDF. O arquivo pode estar corrompido.", e);
        }
    }
}