package br.com.relatorio.sonar.sonar;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Subconjunto da resposta de {@code GET /api/issues/search} usado pelo relatório. */
@JsonIgnoreProperties(ignoreUnknown = true)
record IssuesSearchResponse(Paging paging, List<Issue> issues, List<Rule> rules) {

    IssuesSearchResponse {
        issues = issues == null ? List.of() : issues;
        rules = rules == null ? List.of() : rules;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Paging(Integer total) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Issue(String key, String rule) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Rule(String key, String name) {
    }
}
