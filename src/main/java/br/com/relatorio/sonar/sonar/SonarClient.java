package br.com.relatorio.sonar.sonar;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import br.com.relatorio.sonar.config.SonarProperties;

/**
 * Cliente da Web API do SonarQube ({@code /api/issues/search}).
 */
public class SonarClient {

    private static final Logger log = LoggerFactory.getLogger(SonarClient.class);

    /** Maior página aceita pela API. */
    static final int TAMANHO_PAGINA = 500;
    /** A API não retorna além do 10.000º resultado de uma mesma consulta. */
    static final int LIMITE_RESULTADOS = 10_000;

    private final RestClient restClient;
    private final SonarProperties props;

    public SonarClient(RestClient restClient, SonarProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    /**
     * Conta as issues de uma severidade agrupando por regra.
     *
     * @return ocorrências por regra, na ordem em que as regras foram encontradas
     */
    public List<OcorrenciaRegra> contarOcorrenciasPorRegra(Severidade severidade) {
        Map<String, Long> contagem = new LinkedHashMap<>();
        Map<String, String> nomes = new HashMap<>();

        int pagina = 1;
        int total;
        do {
            IssuesSearchResponse resposta = buscarPagina(severidade, pagina);
            resposta.issues().forEach(issue -> contagem.merge(issue.rule(), 1L, Long::sum));
            resposta.rules().forEach(rule -> nomes.putIfAbsent(rule.key(), rule.name()));
            total = resposta.paging() == null || resposta.paging().total() == null
                    ? resposta.issues().size()
                    : resposta.paging().total();
            if (resposta.issues().isEmpty()) {
                break;
            }
            pagina++;
        } while ((long) (pagina - 1) * TAMANHO_PAGINA < Math.min(total, LIMITE_RESULTADOS));

        if (total > LIMITE_RESULTADOS) {
            log.warn("Severidade {} possui {} issues, mas a API do SonarQube retorna no máximo {}. "
                    + "As contagens estão incompletas; filtre por projeto (sonar.projeto) para obter números exatos.",
                    severidade, total, LIMITE_RESULTADOS);
        }

        List<OcorrenciaRegra> resultado = new ArrayList<>(contagem.size());
        contagem.forEach((regra, qtd) ->
                resultado.add(new OcorrenciaRegra(regra, nomes.getOrDefault(regra, regra), severidade, qtd)));
        return resultado;
    }

    private IssuesSearchResponse buscarPagina(Severidade severidade, int pagina) {
        log.debug("Consultando issues {} - página {}", severidade, pagina);
        try {
            IssuesSearchResponse resposta = restClient.get()
                    .uri(uri -> montarUri(uri, severidade, pagina))
                    .retrieve()
                    .body(IssuesSearchResponse.class);
            if (resposta == null) {
                throw new SonarApiException("Resposta vazia do SonarQube em /api/issues/search");
            }
            return resposta;
        } catch (RestClientResponseException e) {
            throw new SonarApiException(mensagemErroHttp(e), e);
        } catch (RestClientException e) {
            Throwable causa = e.getMostSpecificCause();
            String detalhe = causa.getMessage() != null ? causa.getMessage() : causa.getClass().getSimpleName();
            throw new SonarApiException("Não foi possível acessar o SonarQube em " + props.url() + ": " + detalhe, e);
        }
    }

    private URI montarUri(UriBuilder uri, Severidade severidade, int pagina) {
        uri.path("/api/issues/search")
                .queryParam("severities", severidade.name())
                .queryParam("additionalFields", "rules")
                .queryParam("ps", TAMANHO_PAGINA)
                .queryParam("p", pagina);
        if (props.somenteAbertas()) {
            uri.queryParam("resolved", false);
        }
        if (StringUtils.hasText(props.projeto())) {
            uri.queryParam("components", props.projeto());
        }
        return uri.build();
    }

    private String mensagemErroHttp(RestClientResponseException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == HttpStatus.UNAUTHORIZED) {
            return "SonarQube recusou as credenciais (401). Verifique sonar.token ou sonar.usuario/sonar.senha.";
        }
        if (status == HttpStatus.FORBIDDEN) {
            return "Usuário sem permissão para consultar issues no SonarQube (403).";
        }
        if (status == HttpStatus.NOT_FOUND && StringUtils.hasText(props.projeto())) {
            return "Projeto '" + props.projeto() + "' não encontrado no SonarQube (404).";
        }
        return "Erro HTTP " + e.getStatusCode().value() + " ao consultar o SonarQube: " + e.getResponseBodyAsString();
    }
}
