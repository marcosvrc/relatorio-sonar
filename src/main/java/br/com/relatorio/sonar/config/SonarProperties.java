package br.com.relatorio.sonar.config;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import br.com.relatorio.sonar.sonar.Severidade;

/**
 * Configurações de acesso ao SonarQube e filtros das issues consultadas.
 *
 * @param url            endereço base do SonarQube (ex.: http://localhost:9000)
 * @param token          token de usuário do SonarQube (recomendado)
 * @param usuario        usuário, usado apenas quando não há token
 * @param senha          senha, usada apenas quando não há token
 * @param projeto        chave de um projeto para filtrar as issues (opcional)
 * @param severidades    severidades que entram no relatório
 * @param somenteAbertas quando {@code true}, considera apenas issues não resolvidas
 * @param timeout        tempo máximo de cada chamada à API
 */
@Validated
@ConfigurationProperties(prefix = "sonar")
public record SonarProperties(
        @NotNull URI url,
        String token,
        String usuario,
        String senha,
        String projeto,
        @NotEmpty @DefaultValue({"BLOCKER", "CRITICAL", "MAJOR"}) List<Severidade> severidades,
        @DefaultValue("true") boolean somenteAbertas,
        @DefaultValue("30s") Duration timeout) {

    public boolean possuiToken() {
        return StringUtils.hasText(token);
    }

    public boolean possuiUsuarioESenha() {
        return StringUtils.hasText(usuario) && senha != null;
    }

    @AssertTrue(message = "informe sonar.token (recomendado) ou sonar.usuario e sonar.senha")
    public boolean isCredenciaisInformadas() {
        return possuiToken() || possuiUsuarioESenha();
    }

    /** Evita que o token ou a senha apareçam em logs. */
    @Override
    public String toString() {
        return "SonarProperties[url=%s, projeto=%s, severidades=%s, somenteAbertas=%s, timeout=%s]"
                .formatted(url, projeto, severidades, somenteAbertas, timeout);
    }
}
