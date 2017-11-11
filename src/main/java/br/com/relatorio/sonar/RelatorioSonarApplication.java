package br.com.relatorio.sonar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import br.com.relatorio.sonar.processamento.Processamento;

@SpringBootApplication
public class RelatorioSonarApplication {

	public static void main(String[] args) {
		SpringApplication.run(RelatorioSonarApplication.class, args);
		Processamento processamento = new Processamento();
		processamento.gerarArquivoSonar();
	}
}
