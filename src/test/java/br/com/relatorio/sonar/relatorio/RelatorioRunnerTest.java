package br.com.relatorio.sonar.relatorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import br.com.relatorio.sonar.sonar.SonarApiException;

class RelatorioRunnerTest {

    @Test
    void retornaZeroQuandoGeraComSucesso() {
        var runner = new RelatorioRunner(servico(null));
        runner.run(null);
        assertThat(runner.getExitCode()).isZero();
    }

    @Test
    void retornaUmQuandoSonarFalha() {
        var runner = new RelatorioRunner(servico(new SonarApiException("fora do ar")));
        runner.run(null);
        assertThat(runner.getExitCode()).isEqualTo(1);
    }

    private static RelatorioService servico(RuntimeException erro) {
        return new RelatorioService(null, null, null, null, null) {
            @Override
            public Path gerar() {
                if (erro != null) {
                    throw erro;
                }
                return Path.of("ok.xlsx");
            }
        };
    }
}
