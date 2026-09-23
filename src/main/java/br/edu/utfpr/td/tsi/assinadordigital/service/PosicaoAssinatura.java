package br.edu.utfpr.td.tsi.assinadordigital.service;

/**
 * Posição efetiva (já resolvida) da representação visual da assinatura para
 * uma assinatura específica: tanto o padrão configurado em
 * {@link br.edu.utfpr.td.tsi.assinadordigital.config.PosicaoAssinaturaProperties}
 * quanto uma posição escolhida manualmente pelo usuário (arrastando a
 * etiqueta no preview) resultam neste mesmo formato.
 *
 * pagina menor ou igual a 0 (ou maior que o total de páginas do documento)
 * representa a última página, mesma convenção usada na configuração padrão.
 */
public record PosicaoAssinatura(int pagina, float x, float y, float width, float height) {
}
