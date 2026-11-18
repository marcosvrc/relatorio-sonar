package br.com.relatorio.sonar.sonar;

/**
 * Quantidade de issues de uma regra em uma severidade.
 *
 * @param chave        chave da regra (ex.: java:S1192)
 * @param nome         nome legível da regra
 * @param severidade   severidade consultada
 * @param ocorrencias  número de issues encontradas
 */
public record OcorrenciaRegra(String chave, String nome, Severidade severidade, long ocorrencias) {
}
