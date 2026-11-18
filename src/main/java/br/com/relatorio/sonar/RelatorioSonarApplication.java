package br.com.relatorio.sonar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada da aplicação. Gera o relatório e encerra o processo,
 * com código de saída 0 em caso de sucesso e 1 em caso de falha.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class RelatorioSonarApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(RelatorioSonarApplication.class, args)));
    }
}
