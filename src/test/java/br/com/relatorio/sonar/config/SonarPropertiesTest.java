package br.com.relatorio.sonar.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import br.com.relatorio.sonar.sonar.Severidade;

class SonarPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static SonarProperties bind(Map<String, String> valores) {
        return new Binder(new MapConfigurationPropertySource(valores))
                .bindOrCreate("sonar", SonarProperties.class);
    }

    @Test
    void aplicaValoresPadrao() {
        SonarProperties props = bind(Map.of("sonar.url", "http://localhost:9000", "sonar.token", "abc"));

        assertThat(props.severidades()).containsExactly(Severidade.BLOCKER, Severidade.CRITICAL, Severidade.MAJOR);
        assertThat(props.somenteAbertas()).isTrue();
        assertThat(validator.validate(props)).isEmpty();
    }

    @Test
    void aceitaSeveridadesSeparadasPorVirgula() {
        SonarProperties props = bind(Map.of("sonar.url", "http://x", "sonar.token", "abc",
                "sonar.severidades", "minor,INFO"));

        assertThat(props.severidades()).containsExactly(Severidade.MINOR, Severidade.INFO);
    }

    @Test
    void exigeCredenciais() {
        SonarProperties props = bind(Map.of("sonar.url", "http://x"));

        assertThat(validator.validate(props))
                .extracting(v -> v.getMessage())
                .containsExactly("informe sonar.token (recomendado) ou sonar.usuario e sonar.senha");
    }

    @Test
    void aceitaUsuarioESenhaQuandoNaoHaToken() {
        SonarProperties props = bind(Map.of("sonar.url", "http://x", "sonar.usuario", "admin", "sonar.senha", "s"));

        assertThat(validator.validate(props)).isEmpty();
    }

    @Test
    void naoExpoeSegredosNoToString() {
        SonarProperties props = bind(Map.of("sonar.url", "http://x", "sonar.token", "segredo",
                "sonar.usuario", "u", "sonar.senha", "senha-secreta"));

        assertThat(props.toString()).doesNotContain("segredo").doesNotContain("senha-secreta");
    }
}
