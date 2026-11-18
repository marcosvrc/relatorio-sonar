package br.com.relatorio.sonar.config;

import java.nio.file.Path;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configurações do arquivo gerado.
 *
 * @param diretorio   diretório onde a planilha será gravada (criado se não existir)
 * @param nomeArquivo nome do arquivo .xlsx
 */
@Validated
@ConfigurationProperties(prefix = "relatorio")
public record RelatorioProperties(
        @NotNull @DefaultValue(".") Path diretorio,
        @NotBlank @DefaultValue("RelatorioSonar.xlsx") String nomeArquivo) {

    public Path caminhoArquivo() {
        return diretorio.resolve(nomeArquivo);
    }
}
