package br.com.relatorio.sonar.planilha;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import br.com.relatorio.sonar.sonar.OcorrenciaRegra;

/** Gera a planilha .xlsx com as regras impactadas. */
@Component
public class GeradorPlanilha {

    static final String NOME_ABA = "Ocorrências";
    static final List<String> CABECALHO =
            List.of("Regra", "Chave da Regra", "Severidade", "Quantidade de Ocorrências", "Data de Geração");

    /**
     * Grava a planilha no caminho informado, criando os diretórios necessários.
     * O arquivo é escrito em um temporário e movido no final, para não deixar
     * um .xlsx corrompido caso algo falhe no meio.
     */
    public void gerar(List<OcorrenciaRegra> ocorrencias, LocalDate dataGeracao, Path destino) throws IOException {
        Path diretorio = destino.toAbsolutePath().getParent();
        Files.createDirectories(diretorio);
        Path temporario = Files.createTempFile(diretorio, ".relatorio-sonar-", ".xlsx");
        try {
            try (Workbook workbook = criarWorkbook(ocorrencias, dataGeracao);
                 OutputStream out = Files.newOutputStream(temporario)) {
                workbook.write(out);
            }
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    Workbook criarWorkbook(List<OcorrenciaRegra> ocorrencias, LocalDate dataGeracao) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(NOME_ABA);
        CellStyle estiloCabecalho = estiloCabecalho(workbook);
        CellStyle estiloData = workbook.createCellStyle();
        estiloData.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));

        Row cabecalho = sheet.createRow(0);
        for (int i = 0; i < CABECALHO.size(); i++) {
            Cell cell = cabecalho.createCell(i);
            cell.setCellValue(CABECALHO.get(i));
            cell.setCellStyle(estiloCabecalho);
        }

        int linha = 1;
        for (OcorrenciaRegra ocorrencia : ocorrencias) {
            Row row = sheet.createRow(linha++);
            row.createCell(0).setCellValue(ocorrencia.nome());
            row.createCell(1).setCellValue(ocorrencia.chave());
            row.createCell(2).setCellValue(ocorrencia.severidade().name());
            row.createCell(3).setCellValue(ocorrencia.ocorrencias());
            Cell data = row.createCell(4);
            data.setCellValue(dataGeracao);
            data.setCellStyle(estiloData);
        }

        Row total = sheet.createRow(linha);
        Cell rotulo = total.createCell(2);
        rotulo.setCellValue("Total");
        rotulo.setCellStyle(estiloCabecalho);
        Cell soma = total.createCell(3);
        soma.setCellValue(ocorrencias.stream().mapToLong(OcorrenciaRegra::ocorrencias).sum());
        soma.setCellStyle(estiloCabecalho);

        sheet.createFreezePane(0, 1);
        if (!ocorrencias.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, ocorrencias.size(), 0, CABECALHO.size() - 1));
        }
        for (int i = 0; i < CABECALHO.size(); i++) {
            sheet.autoSizeColumn(i);
        }
        return workbook;
    }

    private static CellStyle estiloCabecalho(Workbook workbook) {
        Font fonte = workbook.createFont();
        fonte.setBold(true);
        CellStyle estilo = workbook.createCellStyle();
        estilo.setFont(fonte);
        estilo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setBorderBottom(BorderStyle.THIN);
        return estilo;
    }
}
