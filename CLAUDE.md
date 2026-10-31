# Smart Van — instruções para agentes de IA

Responda e comente código em português (PT-BR).

Antes de qualquer tarefa, leia:
1. Este arquivo.
2. A seção do `docs/CONTRATO_API.md` da sua etapa (rotas, payloads, códigos de erro e regras).

## Escopo

- Só **back-end** (`backend/`). Não mexer em `frontend/`.
- Não criar nem alterar Dockerfile, docker-compose, Kubernetes ou CI.
- Não introduzir Flyway nem scripts SQL: o banco segue o mapeamento das entidades (`ddl-auto: update`).
  Criar ou alterar uma entidade **muda o banco** de quem rodar o projeto; informe toda mudança de entidade
  no relatório da etapa.
- Não mudar o `docs/CONTRATO_API.md` por conta própria; se precisar, explique no relatório.
- Nenhuma senha, chave ou token no código. Segredos só por variável de ambiente.

## Estrutura (manter a do projeto: por camadas)

```
backend/src/main/java/br/com/smartvan/
  config/       configurações (@Configuration): senha, segurança, relógio, agendador, mapas
  controller/   @RestController — só recebe, valida e delega
  service/      regras de negócio, agendadores (XxxJob) e listeners de evento
  repository/   Spring Data JPA (um repository por entidade, com todas as consultas dela)
  dto/          records de entrada e saída: XxxRequestDTO / XxxResponseDTO
  model/        entidades JPA
  enums/        enums
  exception/    RegraNegocioException, resposta de erro e tratador global
  security/     interceptor do usuário logado e anotação de papel
  event/        eventos internos (ex.: ContratoAtivadoEvent)
  maps/         MapsProvider, Google e plano B por proximidade
backend/src/test/java/br/com/smartvan/
  suporte/      helpers de teste (base de integração, fábrica de dados, relógio fixo, maps falso)
  <Funcionalidade>IntegracaoTest.java
```

## Convenções de código

- DTOs como `record`, com o sufixo `RequestDTO` / `ResponseDTO`. Nunca devolver entidade JPA na resposta.
- Injeção de dependência por construtor. `@Valid` nos corpos das requisições.
- `BigDecimal` para dinheiro. Data de hoje sempre com `LocalDate.now(clock)` (bean `Clock`, fuso `America/Sao_Paulo`).
- Erros de regra: `RegraNegocioException` com os códigos do contrato.
- Usuário atual: `UsuarioLogado.atual()`. Papel nas rotas: `@Papel(TipoUsuario.MOTORISTA)` etc.
- Agendadores: `@Scheduled(cron = "${smartvan.agendador.<x>.cron}", zone = "America/Sao_Paulo")` chamando um
  método público do service (os testes chamam o método direto; o agendador fica desligado nos testes).
- Nunca chamar o Google nos testes.

## Testes

- `@SpringBootTest` + `@AutoConfigureMockMvc` + H2 em memória (`src/test/resources/application.yml`).
- Um teste por regra do contrato, incluindo os caminhos de erro.
- Rodar `cd backend && ./mvnw.cmd -q test` antes de terminar (JDK 21). Não terminar com teste falhando nem
  com teste desligado (`@Disabled`).

## Commits

- **Agentes de etapa não commitam**: ao terminar, devolvem o relatório. Quem commita é o orquestrador (uma vez por
  etapa, com o usuário Git da máquina, sem linha de coautoria) ou o Marcus. Nunca fazer push.
- Padrão: `[TIPO] Descrição em português, começando com maiúscula`.
  Tipos: `[FEAT]`, `[FIX]`, `[TEST]`, `[DOC]`, `[CHORE]`.
  Exemplo: `[FEAT] Inserção de DTOs para classe Usuario`.
- Uma etapa = um commit coerente.

## Economia de uso

- Leia só o necessário: a sua seção do contrato e os arquivos que vai mudar. Procure (grep/glob) antes de abrir.
- Saída de comando curta: testes em modo quieto; se falhar, mostre só o fim.
- Relatório final de no máximo 15 linhas.
