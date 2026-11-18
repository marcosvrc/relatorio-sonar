package br.com.relatorio.sonar.config;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import br.com.relatorio.sonar.sonar.SonarClient;

/** Cria o {@link RestClient} já autenticado e apontando para o SonarQube. */
@Configuration(proxyBeanMethods = false)
public class SonarClientConfig {

    @Bean
    SonarClient sonarClient(RestClient.Builder builder, SonarProperties props) {
        var requestFactory = ClientHttpRequestFactoryBuilder.jdk()
                .build(HttpClientSettings.defaults()
                        .withConnectTimeout(props.timeout())
                        .withReadTimeout(props.timeout()));

        RestClient restClient = builder
                .baseUrl(props.url().toString())
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> autenticar(headers, props))
                .build();
        return new SonarClient(restClient, props);
    }

    /**
     * Token é enviado como usuário do Basic Auth com senha vazia, formato aceito
     * por todas as versões do SonarQube e pelo SonarCloud.
     */
    private static void autenticar(HttpHeaders headers, SonarProperties props) {
        if (props.possuiToken()) {
            headers.setBasicAuth(props.token(), "", StandardCharsets.UTF_8);
        } else {
            headers.setBasicAuth(props.usuario(), props.senha(), StandardCharsets.UTF_8);
        }
    }
}
