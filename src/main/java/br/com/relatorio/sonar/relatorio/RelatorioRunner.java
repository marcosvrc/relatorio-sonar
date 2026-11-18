package br.com.relatorio.sonar.relatorio;

import java.io.UncheckedIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import br.com.relatorio.sonar.sonar.SonarApiException;

/**
 * Dispara a geração do relatório quando a aplicação sobe. Falhas esperadas
 * (SonarQube indisponível, credenciais inválidas, erro de gravação) viram uma
 * mensagem de erro curta e código de saída 1, sem stack trace.
 */
@Component
class RelatorioRunner implements ApplicationRunner, ExitCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(RelatorioRunner.class);

    private final RelatorioService relatorioService;
    private int exitCode;

    RelatorioRunner(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            relatorioService.gerar();
            exitCode = 0;
        } catch (SonarApiException | UncheckedIOException e) {
            log.error("Falha ao gerar o relatório: {}", e.getMessage());
            log.debug("Detalhes da falha", e);
            exitCode = 1;
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
