package br.com.relatorio.sonar.relatorio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.relatorio.sonar.config.RelatorioProperties;
import br.com.relatorio.sonar.config.SonarProperties;
import br.com.relatorio.sonar.planilha.GeradorPlanilha;
import br.com.relatorio.sonar.sonar.OcorrenciaRegra;
import br.com.relatorio.sonar.sonar.Severidade;
import br.com.relatorio.sonar.sonar.SonarClient;

/** Orquestra a consulta ao SonarQube e a geração da planilha. */
@Service
public class RelatorioService {

    private static final Logger log = LoggerFactory.getLogger(RelatorioService.class);

    /** Severidade (mais grave primeiro), depois mais ocorrências, depois nome. */
    static final Comparator<OcorrenciaRegra> ORDENACAO = Comparator
            .comparing(OcorrenciaRegra::severidade)
            .thenComparing(Comparator.comparingLong(OcorrenciaRegra::ocorrencias).reversed())
            .thenComparing(OcorrenciaRegra::nome, String.CASE_INSENSITIVE_ORDER);

    private final SonarClient sonarClient;
    private final GeradorPlanilha geradorPlanilha;
    private final SonarProperties sonarProperties;
    private final RelatorioProperties relatorioProperties;
    private final Clock clock;

    public RelatorioService(SonarClient sonarClient, GeradorPlanilha geradorPlanilha,
            SonarProperties sonarProperties, RelatorioProperties relatorioProperties, Clock clock) {
        this.sonarClient = sonarClient;
        this.geradorPlanilha = geradorPlanilha;
        this.sonarProperties = sonarProperties;
        this.relatorioProperties = relatorioProperties;
        this.clock = clock;
    }

    /** @return caminho do arquivo gerado */
    public Path gerar() {
        log.info("Gerando relatório a partir de {}", sonarProperties);
        List<OcorrenciaRegra> ocorrencias = new ArrayList<>();
        for (Severidade severidade : sonarProperties.severidades().stream().distinct().toList()) {
            List<OcorrenciaRegra> daSeveridade = sonarClient.contarOcorrenciasPorRegra(severidade);
            log.info("Severidade {}: {} regras impactadas", severidade, daSeveridade.size());
            ocorrencias.addAll(daSeveridade);
        }
        ocorrencias.sort(ORDENACAO);

        Path destino = relatorioProperties.caminhoArquivo().toAbsolutePath();
        try {
            geradorPlanilha.gerar(ocorrencias, LocalDate.now(clock), destino);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gravar a planilha em " + destino, e);
        }
        log.info("Relatório gerado em {}", destino);
        return destino;
    }
}
