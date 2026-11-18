package br.com.relatorio.sonar.relatorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.relatorio.sonar.config.RelatorioProperties;
import br.com.relatorio.sonar.config.SonarProperties;
import br.com.relatorio.sonar.planilha.GeradorPlanilha;
import br.com.relatorio.sonar.sonar.OcorrenciaRegra;
import br.com.relatorio.sonar.sonar.Severidade;
import br.com.relatorio.sonar.sonar.SonarClient;

class RelatorioServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void consultaCadaSeveridadeUmaVezOrdenaEGeraArquivo() {
        var sonarProps = new SonarProperties(URI.create("http://sonar"), "t", null, null, null,
                List.of(Severidade.MAJOR, Severidade.BLOCKER, Severidade.MAJOR), true, Duration.ofSeconds(1));
        var relatorioProps = new RelatorioProperties(tempDir, "r.xlsx");
        var consultadas = new ArrayList<Severidade>();
        var dados = Map.of(
                Severidade.MAJOR, List.of(
                        new OcorrenciaRegra("a", "Alfa", Severidade.MAJOR, 2),
                        new OcorrenciaRegra("b", "Beta", Severidade.MAJOR, 9)),
                Severidade.BLOCKER, List.of(new OcorrenciaRegra("c", "Gama", Severidade.BLOCKER, 1)));
        SonarClient sonar = new SonarClient(null, sonarProps) {
            @Override
            public List<OcorrenciaRegra> contarOcorrenciasPorRegra(Severidade severidade) {
                consultadas.add(severidade);
                return dados.get(severidade);
            }
        };
        var gerado = new ArrayList<OcorrenciaRegra>();
        var datas = new ArrayList<LocalDate>();
        GeradorPlanilha gerador = new GeradorPlanilha() {
            @Override
            public void gerar(List<OcorrenciaRegra> ocorrencias, LocalDate data, Path destino) {
                gerado.addAll(ocorrencias);
                datas.add(data);
            }
        };
        Clock clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC);

        Path destino = new RelatorioService(sonar, gerador, sonarProps, relatorioProps, clock).gerar();

        assertThat(consultadas).containsExactly(Severidade.MAJOR, Severidade.BLOCKER);
        assertThat(gerado).extracting(OcorrenciaRegra::chave).containsExactly("c", "b", "a");
        assertThat(datas).containsExactly(LocalDate.of(2026, 11, 18));
        assertThat(destino).isEqualTo(tempDir.resolve("r.xlsx").toAbsolutePath());
    }
}
