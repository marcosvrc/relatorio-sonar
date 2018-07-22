# relatorio-sonar

Geração de Relatório do Sonar em Excel (Apache POI e Chamada Rest a Api do Sonar)


------------



A seguir segue algumas configurações necessárias para a correta utilização deste gerador de relatório:

1. Definir quais severidades irão ser processadas no relatório. No sonar atualmente temos as seguintes severidades:

	a.  BLOCKER
	b.  CRITICAL 
	c.  MAJOR 
	d.  MINOR 
	e.  INFO
	
	Para definir a severidade, basta configurar Constantes.LISTA_SEVERIDADES = { "BLOCKER", "CRITICAL", "MAJOR" }, colocando 	uma ou mais severidades.

2. Definir o caminho da geração do arquivo xls. O default é **C:/java/**. Para configurar o caminho, basta alterar Constantes.CAMINHO_GERACAO_ARQUIVO.

3. Definir o nome do arquivo. O default é **RelatorioSonar.xls**. Para configurar o caminho, basta alterar Constantes.NOME_ARQUIVO.

4. Definir a url para conectar o sonar. O default é **http://localhost:9000**. Para configurar o caminho, basta alterar Constantes.URL_SONAR.

5. Definir o usuário ADM do sonar. O default é "**admin**". Para configurar o caminho, basta alterar Constantes.USUARIO_ADM_SONAR.

6. Definir a senha ADM do sonar.  O default é "**admin**". Para configurar o caminho, basta alterar Constantes.SENHA_ADM_SONAR.


------------


Para rodar o programa, bastar ir em RelatorioSonarApplication.main e rodar como Java Application.