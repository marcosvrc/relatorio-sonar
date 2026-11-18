package br.com.relatorio.sonar.planilha;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.relatorio.sonar.sonar.OcorrenciaRegra;
import br.com.relatorio.sonar.sonar.Severidade;

class GeradorPlanilhaTest {

    @TempDir
    Path tempDir;

    private final GeradorPlanilha gerador = new GeradorPlanilha();

    @Test
    void gravaCabecalhoLinhasETotal() throws Exception {
        Path destino = tempDir.resolve("sub/dir/relatorio.xlsx");
        LocalDate data = LocalDate.of(2026, 11, 18);

        gerador.gerar(List.of(
                new OcorrenciaRegra("java:S1", "Regra Um", Severidade.BLOCKER, 3),
                new OcorrenciaRegra("java:S2", "Regra Dois", Severidade.MAJOR, 7)), data, destino);

        assertThat(destino).exists();
        assertThat(Files.list(destino.getParent())).as("sem arquivos temporários").hasSize(1);
        try (InputStream in = Files.newInputStream(destino); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheet(GeradorPlanilha.NOME_ABA);
            assertThat(texto(sheet.getRow(0))).containsExactlyElementsOf(GeradorPlanilha.CABECALHO);

            Row primeira = sheet.getRow(1);
            assertThat(primeira.getCell(0).getStringCellValue()).isEqualTo("Regra Um");
            assertThat(primeira.getCell(1).getStringCellValue()).isEqualTo("java:S1");
            assertThat(primeira.getCell(2).getStringCellValue()).isEqualTo("BLOCKER");
            assertThat(primeira.getCell(3).getNumericCellValue()).isEqualTo(3);
            assertThat(primeira.getCell(4).getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                    .isEqualTo(data);

            Row total = sheet.getRow(3);
            assertThat(total.getCell(2).getStringCellValue()).isEqualTo("Total");
            assertThat(total.getCell(3).getNumericCellValue()).isEqualTo(10);
        }
    }

    @Test
    void geraPlanilhaApenasComCabecalhoQuandoNaoHaOcorrencias() throws Exception {
        Path destino = tempDir.resolve("vazio.xlsx");

        gerador.gerar(List.of(), LocalDate.of(2026, 1, 1), destino);

        try (InputStream in = Files.newInputStream(destino); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(3).getNumericCellValue()).isZero();
        }
    }

    private static List<String> texto(Row row) {
        return java.util.stream.StreamSupport.stream(row.spliterator(), false)
                .map(c -> c.getStringCellValue()).toList();
    }
}
