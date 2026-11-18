package br.com.relatorio.sonar.sonar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import br.com.relatorio.sonar.config.SonarProperties;

class SonarClientTest {

    private static final String BASE = "http://sonar.local";

    private MockRestServiceServer server;

    private SonarClient criarCliente(String projeto, boolean somenteAbertas) {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        var props = new SonarProperties(URI.create(BASE), "tkn", null, null, projeto,
                List.of(Severidade.BLOCKER), somenteAbertas, Duration.ofSeconds(5));
        return new SonarClient(builder.build(), props);
    }

    @Test
    void agrupaIssuesPorRegraPercorrendoTodasAsPaginas() {
        SonarClient client = criarCliente("meu-projeto", true);

        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE + "/api/issues/search")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("severities", "CRITICAL"))
                .andExpect(queryParam("additionalFields", "rules"))
                .andExpect(queryParam("ps", "500"))
                .andExpect(queryParam("p", "1"))
                .andExpect(queryParam("resolved", "false"))
                .andExpect(queryParam("components", "meu-projeto"))
                .andRespond(withSuccess(pagina(1, 600, issues("java:S1", 499) + ",{\"key\":\"x\",\"rule\":\"java:S2\"}",
                        "{\"key\":\"java:S1\",\"name\":\"Regra Um\"},{\"key\":\"java:S2\",\"name\":\"Regra Dois\"}"),
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE + "/api/issues/search")))
                .andExpect(queryParam("p", "2"))
                .andRespond(withSuccess(pagina(2, 600, issues("java:S2", 100), "{\"key\":\"java:S2\",\"name\":\"Regra Dois\"}"),
                        MediaType.APPLICATION_JSON));

        List<OcorrenciaRegra> resultado = client.contarOcorrenciasPorRegra(Severidade.CRITICAL);

        assertThat(resultado).containsExactly(
                new OcorrenciaRegra("java:S1", "Regra Um", Severidade.CRITICAL, 499),
                new OcorrenciaRegra("java:S2", "Regra Dois", Severidade.CRITICAL, 101));
        server.verify();
    }

    @Test
    void naoEnviaFiltrosOpcionaisQuandoNaoConfigurados() {
        SonarClient client = criarCliente(null, false);
        server.expect(request -> {
                    String query = request.getURI().getQuery();
                    assertThat(query).doesNotContain("resolved").doesNotContain("components");
                })
                .andRespond(withSuccess(pagina(1, 0, "", ""), MediaType.APPLICATION_JSON));

        assertThat(client.contarOcorrenciasPorRegra(Severidade.INFO)).isEmpty();
        server.verify();
    }

    @Test
    void paraNoLimiteDeDezMilResultadosDaApi() {
        SonarClient client = criarCliente(null, true);
        server.expect(ExpectedCount.times(20), requestTo(org.hamcrest.Matchers.startsWith(BASE)))
                .andRespond(withSuccess(pagina(1, 25_000, issues("java:S1", 500), ""), MediaType.APPLICATION_JSON));

        List<OcorrenciaRegra> resultado = client.contarOcorrenciasPorRegra(Severidade.MAJOR);

        assertThat(resultado).singleElement().satisfies(o -> {
            assertThat(o.ocorrencias()).isEqualTo(10_000);
            assertThat(o.nome()).as("sem nome na resposta, usa a chave").isEqualTo("java:S1");
        });
        server.verify();
    }

    @Test
    void traduzErroDeAutenticacao() {
        SonarClient client = criarCliente(null, true);
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE))).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.contarOcorrenciasPorRegra(Severidade.BLOCKER))
                .isInstanceOf(SonarApiException.class)
                .hasMessageContaining("credenciais");
    }

    @Test
    void traduzProjetoInexistente() {
        SonarClient client = criarCliente("nao-existe", true);
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE))).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.contarOcorrenciasPorRegra(Severidade.BLOCKER))
                .isInstanceOf(SonarApiException.class)
                .hasMessageContaining("nao-existe");
    }

    private static String pagina(int p, int total, String issues, String rules) {
        return """
                {"paging":{"pageIndex":%d,"pageSize":500,"total":%d},
                 "issues":[%s],"rules":[%s],"components":[]}
                """.formatted(p, total, issues, rules);
    }

    private static String issues(String regra, int quantidade) {
        return IntStream.range(0, quantidade)
                .mapToObj(i -> "{\"key\":\"" + regra + "-" + i + "\",\"rule\":\"" + regra + "\"}")
                .collect(Collectors.joining(","));
    }
}
