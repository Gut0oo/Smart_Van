# 🔹Projeto de Software - Smart Van
*** 

## 🔹Descrição: 
O Smart Van é uma plataforma que conecta pais de alunos a motoristas de transporte escolar, facilitando a contratação, o gerenciamento e a comunicação no dia a dia.

Para os motoristas, o aplicativo funciona como uma ferramenta de gestão, permitindo cadastrar rotas, regiões e escolas atendidas, organizar as mensalidades dos alunos e sugerir melhores trajetos para o transporte das crianças.

Para os pais, a plataforma facilita a busca por um transporte escolar confiável. É possível cadastrar as crianças, seus endereços e escolas, encontrar motoristas com base na localização, avaliações e escolas atendidas, além de acompanhar mensalidades, avisar quando o filho não irá à aula e acompanhar o trajeto do transporte.

## 🔹Estrutura do Projeto:

```
Smart_Van/
├── backend/     # API Spring Boot e Dockerfile com Java 21
├── frontend/    # App Expo e Dockerfile da versão web com Nginx
├── docs/        # Contrato da API
└── compose.yaml
```

## 🔹Ferramentas: 
- Java
- PostgreSQL
- SpringBoot
- React Native

## 🔹Executar com Docker

Instale Docker com o plugin Docker Compose. No Windows, inicie o Docker Desktop em modo de containers Linux. Os builds usam Java 21 e Node.js 24 dentro dos containers.

Na raiz do projeto, copie o exemplo de configuração:

```powershell
Copy-Item .env.example .env
```

Preencha `DB_PASSWORD` no `.env` com uma senha local. `GOOGLE_MAPS_API_KEY` é opcional: sem ela, o backend usa o comportamento alternativo já existente. O arquivo `.env` é ignorado pelo Git e pelos builds.

```powershell
docker compose up --build -d --wait
```

Abra **http://localhost:8081**. Para conferir a API pelo mesmo endereço:

```powershell
Invoke-RestMethod http://localhost:8081/api/actuator/health
docker compose ps
docker compose logs --tail=50 backend
```

O status da API deve ser `UP`. O build do backend executa os testes existentes; o do frontend executa lint, TypeScript e a exportação web do Expo.

### Como os componentes se conectam

| Componente | Função |
| --- | --- |
| Frontend | Nginx entrega o app web e encaminha `/api/` para o backend. É a única porta publicada no computador. |
| Backend | Executa a API Java e suas tarefas agendadas. Aguarda o banco ficar saudável antes de iniciar. |
| PostgreSQL | Armazena os cadastros no volume nomeado `postgres-data`, separado dos containers. |

O navegador usa a URL relativa `/api`. Por exemplo, `/api/auth/login` é encaminhado como `/auth/login` para a API, preservando seu contrato. A mesma imagem web funciona no computador e no endereço da EC2. O frontend é exportado como uma aplicação de página única para que caminhos como `/contrato/1` sejam resolvidos pelo app. [Publicação web do Expo](https://docs.expo.dev/guides/publishing-websites/).

Para parar a execução, preservando os cadastros:

```powershell
docker compose down
```

Na próxima execução, use `docker compose up -d --wait`. O comando `docker compose down --volumes` também remove os dados do banco.

Para usar Expo no celular com a API do Docker, configure `WEB_BIND=0.0.0.0` no `.env`, recrie a execução com `docker compose up -d --wait` e use `EXPO_PUBLIC_API_URL=http://IP_DO_PC:8081/api` no `frontend/.env.local`. Celular e computador precisam estar na mesma rede.

## 🔹Transferência manual para EC2

O projeto produz duas imagens de código, `smartvan-backend` e `smartvan-frontend`. A imagem oficial do PostgreSQL completa o conjunto. O Compose sobe os três serviços na mesma instância.

Depois de construir as imagens e iniciar o Compose localmente, exporte o conjunto com a tag padrão:

```powershell
docker image save --output smartvan-images.tar smartvan-backend:faculdade-v1 smartvan-frontend:faculdade-v1 postgres:17-alpine
```

Se mudar `IMAGE_TAG` no `.env`, use a mesma tag no comando de exportação e no `.env` da EC2. As imagens precisam ser compatíveis com a arquitetura da instância: `amd64` corresponde a `x86_64`; `arm64` corresponde a `aarch64`. Confira a imagem com:

```powershell
docker image inspect smartvan-backend:faculdade-v1 --format '{{.Os}}/{{.Architecture}}'
```

Na EC2 Linux, prepare Docker e o plugin Compose e crie uma pasta para a aplicação. Envie para essa pasta `smartvan-images.tar`, `compose.yaml` e `.env.example`, usando seu método de transferência. Na instância, execute:

```bash
docker image load --input smartvan-images.tar
cp .env.example .env
```

Preencha a senha no `.env` da EC2 e configure:

```dotenv
WEB_BIND=0.0.0.0
WEB_PORT=80
```

Inicie as imagens carregadas, sem recompilar:

```bash
docker compose up -d --no-build --pull never --wait
curl http://localhost/api/actuator/health
```

O app ficará em `http://IP_DA_EC2`. No security group, permita a porta HTTP configurada aos IPs que participarão da demonstração. O backend e o banco usam a rede interna do Compose. [Security groups da EC2](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-security-groups.html).

As imagens exportadas contêm o código; os cadastros ficam no volume de cada ambiente. A EC2 começa com seu próprio banco. O mecanismo de identificação do projeto é acadêmico, conforme [CONTRATO_API.md](docs/CONTRATO_API.md).

A implantação na AWS é feita manualmente pelo responsável pelo trabalho.

## 🔹Criadores 

| Nome                          | RA        |
|-------------------------------|-----------|
| ***Marcus Ruiz Nishimura Baena*** | ***10426044***| 
|***Gustavo Melo Silva*** |***10438415***|
|***Luiz Alberto Silva Mota*** |***10436776***|
|***Arthur Rodrigues Soares Lourenço*** |***10434424***|

