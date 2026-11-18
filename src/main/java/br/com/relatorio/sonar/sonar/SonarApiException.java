package br.com.relatorio.sonar.sonar;

/** Falha ao consultar a API do SonarQube. */
public class SonarApiException extends RuntimeException {

    public SonarApiException(String message) {
        super(message);
    }

    public SonarApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
