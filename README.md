<p align="center">
  <img src="src/main/resources/META-INF/resources/brand/mark.svg" alt="" width="96">
</p>

<h1 align="center">vagora</h1>

<p align="center">
  <b>A vaga certa, agora.</b><br>
  Lê os alertas de vaga que chegam no seu email, abre cada vaga e diz quais combinam com o seu currículo, com o motivo de cada nota.
</p>

<p align="center">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-0D52B4">
  <img alt="Quarkus" src="https://img.shields.io/badge/Quarkus-3.40-1E9BE8">
  <img alt="Python" src="https://img.shields.io/badge/Python-3.9%2B-1E9BE8">
  <img alt="Licença MIT" src="https://img.shields.io/badge/licen%C3%A7a-MIT-3BE0C9">
</p>

---

## Objetivo

Quem procura emprego recebe dezenas de alertas de vaga por dia e perde horas abrindo uma por uma para descobrir que a
maioria não combina com o próprio perfil. O Vagora faz essa triagem por você:

1. **Lê os alertas de vaga** que as plataformas mandam para o seu email.
2. **Abre cada vaga** e lê a descrição completa.
3. **Compara com o seu currículo** usando o Gemini (IA do Google) e dá uma **nota de 0 a 100**, com os motivos.
4. **Mostra só o que importa**: as vagas compatíveis aparecem primeiro e, quando a vaga combina, você recebe um
   **currículo adaptado** para os sistemas de triagem (ATS) daquela empresa.

Tudo roda **no seu computador**: seus dados, senhas e currículo não saem da sua máquina (exceto o texto enviado ao
Gemini para a análise).

> **Plataformas suportadas:** hoje, **LinkedIn**. O Vagora foi desenhado para receber outras plataformas de vagas
> (como Indeed, Gupy e Catho) — veja o [roadmap](#roadmap).

## Como funciona

```
 Alerta de vaga         Vagora lê o email        Navegador abre         Gemini compara       Resultado
 no seu email    ──▶    (um alerta por vez,  ──▶  cada vaga e lê   ──▶   vaga x currículo ──▶  na tela, com nota,
                         15 a 25 min de pausa)     a descrição           e dá a nota          motivos e currículo
```

- **Só alertas de vaga:** o Vagora busca apenas os emails da plataforma e só marca como lido o que já analisou. Seus
  outros emails ficam intocados.
- **Ritmo humano:** um alerta por vez, com 15 a 25 minutos entre eles, e pausas de 8 a 25 segundos entre as vagas,
  para não chamar a atenção da plataforma.
- **Sem retrabalho:** vagas já analisadas e vagas encerradas ("não aceita mais candidaturas") são puladas.
- **Corte de 70:** a vaga é *compatível* quando a nota chega a 70 **e** os requisitos obrigatórios são atendidos.

## Começando

### O que você precisa

| Item | Para quê | Onde conseguir |
|---|---|---|
| **Java 21+** | rodar o Vagora | [adoptium.net](https://adoptium.net) |
| **Python 3.9+** | o leitor de vagas | [python.org](https://www.python.org/downloads) |
| **Google Chrome** | abrir as vagas | [google.com/chrome](https://www.google.com/chrome) |
| **Chave da API do Gemini** | a análise das vagas (cota gratuita) | [aistudio.google.com/apikey](https://aistudio.google.com/apikey) |
| **Senha de app do email** | ler os alertas (não é a senha normal) | Gmail: [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords) |
| *Opcional:* `xvfb` (Linux) | navegador totalmente invisível | `sudo apt install xvfb` |

### Instalar e iniciar

```bash
git clone https://github.com/Viniciushfc/vagora.git
cd vagora
./run.sh
```

O `run.sh` faz tudo sozinho: confere Java, Python e Chrome (e diz como resolver se faltar algo), cria o ambiente do
leitor de vagas, compila e **abre o navegador** em <http://localhost:8080>.

```bash
./run.sh --no-browser   # inicia sem abrir o navegador
./run.sh --clean        # apaga o build e compila do zero
./run.sh --help
```

### Configurar (uma vez, pela própria tela)

O Vagora abre num passo a passo:

1. **Perfil e currículo** — seu nome, a chave do Gemini e o currículo em PDF (até 5 MB, com texto selecionável).
2. **Email** — o email que recebe os alertas e a **senha de app**. O Vagora testa o acesso e **descobre sozinho a
   pasta** onde os alertas chegam (útil se você usa filtros e etiquetas).
3. **LinkedIn** — a conta usada para abrir as vagas. Leia os riscos antes de conectar (abaixo).

Pronto: a partir daí o Vagora trabalha sozinho enquanto estiver aberto.

## Usando

| Tela | O que tem |
|---|---|
| **Vagas** | Todas as vagas analisadas numa régua de 0 a 100 com o corte de 70, busca, filtro de compatíveis, ordenação, motivos de cada nota e o currículo adaptado para copiar. |
| **Contas** | Email (com a pasta lida) e LinkedIn conectados. Uma conta por plataforma: para trocar, desconecte e conecte de novo. |
| **LinkedIn** | Acompanhamento do leitor de vagas em tempo real: vaga sendo lida, linha do tempo dos passos e, se algo der errado, a explicação, o que fazer e **uma captura da tela** que o navegador estava vendo. |
| **Perfil** | Ficha mostrando o que está pronto e o que falta, pausar o monitoramento, mostrar o navegador durante a análise e editar seus dados. |

O **monitor** no canto da tela mostra o que o Vagora está fazendo agora: verificando o email, analisando um alerta,
aguardando o intervalo de segurança ou pausado.

### Quando o LinkedIn pede verificação

Por padrão o navegador do leitor roda **escondido**. Se o LinkedIn pedir um código ou captcha, o alerta fica guardado
no email e a tela avisa. Ligue **"Mostrar o navegador durante a análise"** no Perfil: na próxima análise a janela abre e
espera até 5 minutos você resolver. Depois disso a sessão fica salva e você pode desligar de novo.

## Segurança e riscos

- **Riscos para a conta do LinkedIn:** o contrato do LinkedIn proíbe ferramentas automatizadas. A conta pode receber
  verificações de segurança, avisos de novo acesso ou, em casos repetidos, restrições. O Vagora reduz o risco com
  ritmo humano e pausas, mas **não o elimina**. Se puder, use uma conta secundária só para isso.
- **Suas credenciais ficam só no seu computador**, no arquivo `vagora.db` (já no `.gitignore`). Não compartilhe esse
  arquivo.
- **A API só aceita conexões da própria máquina** (`127.0.0.1`) e não tem login, porque cada instalação é de uma pessoa.
- **O que vai para fora:** o texto da vaga e do seu currículo é enviado ao Gemini para a análise, usando a sua chave.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Aplicação | **Java 21**, **Quarkus 3** (REST, Scheduler, Hibernate ORM com Panache) |
| Banco | **SQLite** em arquivo local (`vagora.db`) |
| Email | **IMAP** com Jakarta Mail (Angus Mail) |
| Leitor de vagas | **Python** com **Patchright** (Playwright) controlando o **Google Chrome**; tela virtual com **Xvfb** |
| Análise | **Google Gemini** (SDK `google-genai`) |
| Currículo | **Apache PDFBox** para extrair o texto do PDF |
| Interface | **HTML, CSS e JavaScript** puros, servidos pelo próprio Quarkus |
| Testes | JUnit 5, Mockito, REST Assured e H2 em memória |

### Arquitetura

O projeto segue **Clean Architecture / Hexagonal**: o domínio não conhece Quarkus, banco, email, navegador nem Gemini.

```
src/main/java/com/viniciushfc
├── domain                      regras de negócio, sem framework
│   ├── entities                Profile, LinkedAccount, EmailReceived, JobMatch
│   ├── usecases                contracts/ (interfaces) e impl/ (um caso de uso por ação)
│   ├── gateways                portas para o mundo externo (email, leitor de vagas, IA, PDF)
│   ├── repositories            portas de persistência
│   ├── exceptions              categorias seladas de erro + ErrorCode
│   └── vo                      objetos de valor (ProcessingStatus, ScraperActivity, ...)
└── infra
    ├── adapter/in              REST (/profile) e o job agendado que verifica o email
    └── adapter/out             SQLite (Panache), IMAP, scraper (Python), Gemini, PDFBox

python/scraping_detalis_job.py  leitor de vagas do LinkedIn
src/main/resources/META-INF/resources   interface web
```

**Adicionar uma plataforma nova** (ex.: Indeed) segue esse desenho: um novo valor em `ExternalProvider`, um leitor de
vagas para a plataforma e o reconhecimento dos alertas dela no email. O resto do fluxo (análise, notas, interface) é
reaproveitado.

### API

Todas as rotas são do perfil da instalação, sem id:

| Método | Rota | Para quê |
|---|---|---|
| `GET` `POST` `PATCH` `DELETE` | `/profile` | perfil (criação e edição em `multipart/form-data`) |
| `GET` | `/profile/curriculum` | currículo em PDF |
| `POST` / `DELETE` | `/profile/accounts` · `/profile/accounts/{provider}` | conectar / desconectar conta (`EMAIL`, `LINKEDIN`) |
| `GET` / `PUT` | `/profile/email/folders` · `/profile/email/folder` | pastas do email com contagem de alertas / trocar a pasta |
| `GET` | `/profile/job-matches` | vagas analisadas |
| `GET` | `/profile/status` | monitoramento (última verificação, próximo alerta, último erro) |
| `GET` | `/profile/scraper` · `/profile/scraper/screenshot` | leitor de vagas em tempo real e captura da última falha |

Erros têm sempre o mesmo formato:

```json
{ "success": false, "type_error": "NOT_FOUND", "message_error": "Nenhum perfil configurado.",
  "status": 404, "path": "/profile", "timestamp": "2026-11-15T18:00:00-03:00" }
```

## Desenvolvimento

```bash
./mvnw quarkus:dev   # modo de desenvolvimento com recarga automática
./mvnw test          # testes (usam H2 em memória; não tocam no seu vagora.db)
```

## Roadmap

- [x] LinkedIn: leitura dos alertas, abertura das vagas e análise
- [x] Interface web com acompanhamento do leitor e currículo adaptado
- [ ] Suporte a Windows no `run.sh` (`run.ps1`)
- [ ] **Novas plataformas de vagas:** Indeed, Gupy, Catho e outras
- [ ] Notificação quando surgir uma vaga compatível

## Licença

[MIT](LICENSE) © 2026 Vinicius Henry Ferrari Campos
