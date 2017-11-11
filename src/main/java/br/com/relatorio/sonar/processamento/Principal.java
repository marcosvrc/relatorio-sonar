package br.com.relatorio.sonar.processamento;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.rule.Rule;

import br.com.relatorio.sonar.entidades.RuleProcessada;
import br.com.relatorio.sonar.utils.Util;

public class Principal {

	private static SonarClient client;
	private static IssueClient issueClient;
	private static HSSFSheet sheet;
	private static HSSFWorkbook workbook;

	// Não Mexer
	private static final String LABEL_SEVERITIES = "severities";

	// Configurações
	// Carregar com os possíveis valores. Pode colocar um ou mais de um
	// ("BLOCKER | CRITICAL | MAJOR | MINOR | INFO")
	private static String[] listaSeveridades = { "BLOCKER", "CRITICAL", "MAJOR" };
	private static final String CAMINHO_ARQUIVO_GERADO = "C:/java/";
	private static final String NOME_ARQUIVO = "RelatórioSonasr.xls";
	private static final String URL_SONAR = "http://localhost:9000";

	public static void main(String args[]) {

		System.out.println("Setando Configurações Iniciais");
		configuracaoInicial();
		String filename = CAMINHO_ARQUIVO_GERADO + NOME_ARQUIVO;

		try {
			List<RuleProcessada> listaRuleUnificada = new ArrayList<RuleProcessada>();
			workbook = new HSSFWorkbook();
			sheet = workbook.createSheet("Ocorrências");
			for (int i = 0; i < listaSeveridades.length; i++) {

				System.out.println(
						"############# Severidade analisada: " + listaSeveridades[i] + " ###########################");
				IssueQuery queryInformacoesPagina = IssueQuery.create();
				queryInformacoesPagina.urlParams().put(LABEL_SEVERITIES, listaSeveridades[i]);
				System.out.println("Obtendo informações iniciais");
				Issues informacoesIniciais = getIssues(queryInformacoesPagina);

				System.out.println("Obtendo dados do Sonar");
				System.out.println("Aguarde...");
				List<Issues> listIssues = obterDadosSonar(informacoesIniciais, listaSeveridades[i]);

				System.out.println("Processando dados do sonar");
				List<RuleProcessada> listaRuleProcessada = processarListaRule(listIssues, informacoesIniciais,
						listaSeveridades[i]);

				listaRuleUnificada.addAll(listaRuleProcessada);

			}
			System.out.println("Criando Planilha");
			createExcel(listaRuleUnificada, sheet);

			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
			fileOut.close();
			System.out.println("Arquivo Gerado com Sucesso!");

		} catch (Exception ex) {
			System.out.println(ex);

		}

	}

	private static HSSFWorkbook createExcel(List<RuleProcessada> listRuleProcessada, HSSFSheet sheet) {

		Date dataGeracao = new Date();

		HSSFRow rowhead = sheet.createRow((short) 0);
		rowhead.createCell(0).setCellValue("Rule");
		rowhead.createCell(1).setCellValue("Quantidade Ocorrências");
		rowhead.createCell(2).setCellValue("Data de Geração Relatório");
		rowhead.createCell(3).setCellValue("Severidade");

		int contador = 1;
		int total = 0;
		for (RuleProcessada ruleProcessada : listRuleProcessada) {

			HSSFRow row = sheet.createRow((short) contador++);
			row.createCell(0).setCellValue(ruleProcessada.getDescricaoRule());
			row.createCell(1).setCellValue(ruleProcessada.getQuantidadeOcorrencias());
			total += ruleProcessada.getQuantidadeOcorrencias();
			row.createCell(2).setCellValue(Util.formatarData(dataGeracao));
			row.createCell(3).setCellValue(ruleProcessada.getSeveridade());
		}

		return workbook;

	}

	private static Issues getIssues(IssueQuery query) {
		Issues issues = issueClient.find(query);
		return issues;
	}

	private static String getNameRule(List<Issues> listIssues, String keyRule) {
		String name = "";
		for (Issues issueList : listIssues) {
			for (Rule rule : issueList.rules())
				if (rule.key().equalsIgnoreCase(keyRule)) {
					name = rule.name();
					break;
				}

		}
		return name;
	}

	private static List<RuleProcessada> processarListaRule(List<Issues> listIssues, Issues issues, Object severidade) {

		HashMap<String, Integer> mapRuleProcessada = new HashMap<String, Integer>();

		List<RuleProcessada> listaRuleProcessada = new ArrayList<RuleProcessada>();

		mapRuleProcessada = getRules(listIssues, mapRuleProcessada);

		Integer contador = 1;
		for (Issues issueList : listIssues) {
			for (Issue issue : issueList.list()) {

				if (mapRuleProcessada.containsKey(issue.ruleKey())) {
					contador = mapRuleProcessada.get(issue.ruleKey());
					contador++;
					mapRuleProcessada.put(issue.ruleKey(), contador);
				}
			}

		}
		for (String key : mapRuleProcessada.keySet()) {
			RuleProcessada ruleProcessada = new RuleProcessada();
			ruleProcessada.setQuantidadeOcorrencias(mapRuleProcessada.get(key));
			ruleProcessada.setDescricaoRule(getNameRule(listIssues, key));
			ruleProcessada.setKeyRule(key);
			ruleProcessada.setSeveridade(severidade.toString());
			listaRuleProcessada.add(ruleProcessada);
		}
		return listaRuleProcessada;
	}

	private static List<Issues> obterDadosSonar(Issues informacoesIniciais, String severidade) {

		List<Issues> listIssues = new ArrayList<Issues>();

		IssueQuery queryIssues = IssueQuery.create();
		queryIssues.urlParams().put(LABEL_SEVERITIES, severidade);

		for (int i = 1; i <= informacoesIniciais.paging().pages(); i++) {
			queryIssues.urlParams().put("p", i);
			Issues issues = getIssues(queryIssues);
			listIssues.add(issues);
		}
		return listIssues;
	}

	private static void configuracaoInicial() {

		String login = "admin";
		String password = "admin";

		client = SonarClient.create(URL_SONAR);
		client.builder().login(login);
		client.builder().password(password);

		issueClient = client.issueClient();
	}

	private static HashMap<String, Integer> getRules(List<Issues> listIssues,
			HashMap<String, Integer> mapRuleProcessada) {

		for (Issues issueList : listIssues) {
			for (Issue issue : issueList.list()) {
				if (!mapRuleProcessada.containsKey(issue.ruleKey())) {
					mapRuleProcessada.put(issue.ruleKey(), 0);
				}

			}
		}
		return mapRuleProcessada;

	}

}
