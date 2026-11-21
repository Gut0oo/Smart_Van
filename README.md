# 🔹Projeto de Software - Smart Van
*** 

## 🔹Descrição: 
O Smart Van é uma plataforma que conecta pais de alunos a motoristas de transporte escolar, facilitando a contratação, o gerenciamento e a comunicação no dia a dia.

Para os motoristas, o aplicativo funciona como uma ferramenta de gestão: eles cadastram o veículo, o endereço de partida e os horários que atendem em cada escola, com o preço de cada horário, respondem às propostas de contratação, acompanham as mensalidades e geram a rota do dia otimizada, que abre no Google Maps.

Para os pais, a plataforma facilita a busca por um transporte escolar confiável. É possível cadastrar as crianças, seus endereços e escolas, encontrar motoristas validados que atendem a escola no horário da aula, propor o valor da mensalidade, acompanhar as mensalidades, avisar quando o filho não irá à aula e pedir o cancelamento do contrato.

## 🔹Estrutura do Projeto:

```
Smart_Van/
├── backend/                 API em Spring Boot (Java 21)
│   └── src/main/java/br/com/smartvan/
│       ├── controller/      rotas da API
│       ├── service/         regras de negócio e agendadores
│       ├── repository/      acesso ao banco (Spring Data JPA)
│       ├── dto/             objetos de entrada e saída da API
│       ├── model/           entidades
│       ├── enums/           status e tipos (contrato, mensalidade, rota, usuário)
│       ├── event/           eventos internos (ex.: contrato ativado gera a mensalidade)
│       ├── maps/            integração com o Google Maps (Routes e Geocoding)
│       ├── security/        identificação do usuário e papéis
│       ├── config/          configurações (CORS, mapas, agendador, senhas)
│       └── exception/       erros padronizados da API
├── frontend/                aplicativo em React Native com Expo (TypeScript)
│   └── src/
│       ├── app/             telas (responsável, motorista e administrador)
│       ├── components/      componentes de interface reutilizáveis
│       └── services/        conexão com a API, login e formatação
├── docs/CONTRATO_API.md     rotas, regras e códigos de erro da API
└── compose.yaml             execução completa com Docker (banco, API e app web)
```

## 🔹Ferramentas: 
- Java 21
- Spring Boot
- PostgreSQL
- React Native com Expo (TypeScript)
- Google Maps Platform (Routes API e Geocoding API)
- Docker
- AWS

## 🔹Implantação:
A API, o aplicativo web (servido pelo Nginx) e o banco PostgreSQL rodam em contêineres Docker, orquestrados pelo `compose.yaml`. As imagens da API e do aplicativo são geradas com a mesma tag de versão e executadas numa instância EC2 da AWS. Senhas e a chave do Google Maps ficam no arquivo `.env` do servidor (modelo em `.env.example`), fora do código.

## 🔹Criadores 

| Nome                          | RA        |
|-------------------------------|-----------|
| ***Marcus Ruiz Nishimura Baena*** | ***10426044***| 
|***Gustavo Melo Silva*** |***10438415***|
|***Luiz Alberto Silva Mota*** |***10436776***|
|***Arthur Rodrigues Soares Lourenço*** |***10434424***|
