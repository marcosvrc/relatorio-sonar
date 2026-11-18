# relatorio-sonar

Gera uma planilha Excel (`.xlsx`) com as **regras do SonarQube impactadas por issues**,
mostrando quantas ocorrências cada regra tem em cada severidade.

A aplicação consulta a Web API do SonarQube (`GET /api/issues/search`), agrupa as issues
por regra e grava a planilha. É uma aplicação de linha de comando: roda, gera o arquivo e encerra.

## Planilha gerada

Aba **Ocorrências**, ordenada por severidade (mais grave primeiro) e depois pelo número de ocorrências:

| Regra | Chave da Regra | Severidade | Quantidade de Ocorrências | Data de Geração |
|---|---|---|---|---|
| String literals should not be duplicated | java:S1192 | CRITICAL | 42 | 18/11/2026 |
| … | | | | |
| | | **Total** | **…** | |

## Requisitos

- Java 21+
- Acesso a um SonarQube (ou SonarCloud) e um token de usuário com permissão de leitura nos projetos.

Não é preciso instalar o Maven: use o wrapper `./mvnw` (ou `mvnw.cmd` no Windows).

## Como executar

```bash
# 1. Gere o token em: SonarQube > My Account > Security > Generate Tokens
export SONAR_URL=https://sonar.minhaempresa.com.br
export SONAR_TOKEN=squ_xxxxxxxxxxxxxxxx

# 2. Rode direto pelo Maven...
./mvnw spring-boot:run

# ...ou gere o jar e execute
./mvnw package
java -jar target/relatorio-sonar-1.0.0-SNAPSHOT.jar --sonar.projeto=meu-projeto
```

No Windows (PowerShell), use `$env:SONAR_URL="..."` e `$env:SONAR_TOKEN="..."`.

O arquivo é gravado em `./relatorios/RelatorioSonar.xlsx` por padrão. O processo termina com
código `0` em caso de sucesso e `1` em caso de erro (útil em pipelines e agendadores).

## Configuração

Toda a configuração fica em `src/main/resources/application.yml` e pode ser sobrescrita por
variável de ambiente ou por argumento `--chave=valor`, sem recompilar.

| Propriedade | Variável de ambiente | Padrão | Descrição |
|---|---|---|---|
| `sonar.url` | `SONAR_URL` | `http://localhost:9000` | Endereço do SonarQube |
| `sonar.token` | `SONAR_TOKEN` | — | Token de usuário (forma recomendada de autenticação) |
| `sonar.usuario` / `sonar.senha` | `SONAR_USUARIO` / `SONAR_SENHA` | — | Alternativa ao token (só usada se não houver token) |
| `sonar.projeto` | `SONAR_PROJETO` | todos | Chave de um projeto para filtrar as issues |
| `sonar.severidades` | `SONAR_SEVERIDADES` | `BLOCKER,CRITICAL,MAJOR` | Severidades incluídas: `BLOCKER`, `CRITICAL`, `MAJOR`, `MINOR`, `INFO` |
| `sonar.somente-abertas` | `SONAR_SOMENTEABERTAS` | `true` | `true` considera só issues não resolvidas; `false` inclui as resolvidas |
| `sonar.timeout` | `SONAR_TIMEOUT` | `30s` | Tempo máximo de cada chamada à API |
| `relatorio.diretorio` | `RELATORIO_DIRETORIO` | `./relatorios` | Pasta de saída (criada se não existir) |
| `relatorio.nome-arquivo` | `RELATORIO_NOMEARQUIVO` | `RelatorioSonar.xlsx` | Nome do arquivo |

Exemplo:

```bash
java -jar target/relatorio-sonar-1.0.0-SNAPSHOT.jar \
  --sonar.projeto=meu-projeto \
  --sonar.severidades=BLOCKER,CRITICAL \
  --relatorio.diretorio=/tmp/relatorios
```

> **Segurança:** nunca grave o token ou a senha no `application.yml` nem no código. Use variáveis
> de ambiente ou o gerenciador de segredos do seu CI. A aplicação não imprime credenciais nos logs.

## Limitações conhecidas

- A API do SonarQube retorna no máximo **10.000 issues por consulta**. Se uma severidade
  ultrapassar esse número, a aplicação avisa no log e as contagens ficam incompletas; nesse caso
  filtre por projeto com `sonar.projeto`.
- A partir do SonarQube 10.2 (modo *Multi-Quality Rule*), as severidades clássicas convivem com as
  novas severidades de impacto. O relatório continua usando as severidades clássicas
  (parâmetro `severities`), que ainda são aceitas pela API.

## Desenvolvimento

```bash
./mvnw verify      # compila e roda os testes
```

Estrutura:

```
src/main/java/br/com/relatorio/sonar
├── RelatorioSonarApplication.java   # ponto de entrada
├── config/      # propriedades (SonarProperties, RelatorioProperties) e criação do cliente HTTP
├── sonar/       # cliente da API do SonarQube e modelos da resposta
├── planilha/    # geração do .xlsx com Apache POI
└── relatorio/   # orquestração (RelatorioService) e execução (RelatorioRunner)
```

Stack: Java 21, Spring Boot 4 (`RestClient`, `@ConfigurationProperties` com validação),
Apache POI 6 e JUnit 5. O CI no GitHub Actions roda os testes a cada push, e o Dependabot
mantém as dependências atualizadas.
