<p align="center">
  <img src="src/main/resources/META-INF/resources/brand/mark.svg" alt="Logo do Vagora" width="96">
</p>

<h1 align="center">vagora</h1>

<p align="center">
  <b>A vaga certa, agora.</b><br>
  Triagem automática dos seus alertas de vaga: o Vagora lê o email, abre cada vaga, compara com o seu currículo
  e mostra só o que vale a sua candidatura, com a nota, os motivos e um currículo adaptado.
</p>

<p align="center">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-1E4D3B">
  <img alt="Quarkus 3" src="https://img.shields.io/badge/Quarkus-3.40-1E4D3B">
  <img alt="Python 3.9+" src="https://img.shields.io/badge/Python-3.9%2B-3F7D5E">
  <img alt="IA: Claude, Gemini, Groq e IA local" src="https://img.shields.io/badge/IA-Claude%20%C2%B7%20Gemini%20%C2%B7%20Groq%20%C2%B7%20IA%20local-3F7D5E">
  <img alt="Licença MIT" src="https://img.shields.io/badge/licen%C3%A7a-MIT-C6E85A">
</p>

---

## Sumário

- [O problema](#o-problema)
- [O que o Vagora faz](#o-que-o-vagora-faz)
- [Como funciona](#como-funciona)
- [O que você encontra na tela](#o-que-você-encontra-na-tela)
- [Configurações](#configurações)
- [Começando](#começando)
- [Segurança e riscos](#segurança-e-riscos)
- [Tecnologias e arquitetura](#tecnologias-e-arquitetura)
- [Desenvolvimento](#desenvolvimento)
- [Roadmap](#roadmap)

## O problema

Quem procura emprego ativa alertas no LinkedIn e no Indeed e passa a receber **dezenas de vagas por dia** no email.
O alerta traz só o título e a empresa. Para saber se a vaga serve, é preciso abrir uma por uma e ler a descrição
inteira atrás dos requisitos obrigatórios, da senioridade e do modelo de trabalho. Na maioria das vezes, a vaga não
combina.

O resultado é conhecido:

- **Horas perdidas** abrindo vagas que não servem, todos os dias.
- **Vagas boas perdidas** no meio do volume, ou vistas tarde demais.
- **Candidaturas genéricas**, porque não sobra tempo para adaptar o currículo a cada vaga, e os sistemas de
  triagem das empresas (ATS) descartam o currículo que não usa as palavras da vaga.

## O que o Vagora faz

O Vagora faz essa triagem por você, sozinho e no seu computador:

1. **Lê os alertas de vaga** que chegam no seu email, sem mexer nos outros emails.
2. **Abre cada vaga** num navegador nesta máquina e lê a descrição completa.
3. **Compara com o seu currículo** usando IA e dá uma **nota de 0 a 100**, com os motivos: o que você atende e
   o que falta.
4. **Separa o que importa:** a vaga é *compatível* quando a nota chega a 70 **e** os requisitos obrigatórios são
   atendidos.
5. **Adapta o currículo** para cada vaga compatível, no formato que os sistemas de triagem (ATS) leem, sem inventar
   nada que não esteja no original. Você copia o texto ou baixa em PDF.

Você abre o Vagora e encontra a lista pronta: as melhores vagas no topo, com o motivo de cada nota e o currículo
para enviar.

## Como funciona

```
 Alerta de vaga        Vagora lê o email        Navegador abre         IA dá a nota          Resultado na tela
 no seu email   ──▶    (última semana,     ──▶  só as vagas      ──▶   vaga × currículo ──▶  nota, motivos e
                        mais novos primeiro)     novas                 (com reserva)         currículo adaptado
```

- **Só alertas de vaga:** o Vagora busca apenas os emails das plataformas e só marca como lido o que já analisou.
- **Ritmo humano:** espera de 15 a 25 minutos entre um acesso à plataforma e outro (dá para ajustar) e faz pausas
  de 8 a 25 segundos entre as vagas, com rolagem e movimentos de mouse parecidos com os de uma pessoa.
- **Recentes primeiro:** só alertas da última semana, dos mais novos para os mais antigos.
- **Sem retrabalho:** vagas já analisadas não são abertas de novo. Vagas encerradas ou que não carregaram são
  puladas sem derrubar o alerta.
- **Fila de análise:** as vagas lidas esperam numa fila e a IA analisa em lotes a cada minuto, sem esperar o leitor terminar o alerta (a partir de 5 vagas lidas, para não gastar a cota da IA em lotes pequenos). Uma falha da IA
  não perde nenhuma vaga, e você pode priorizar uma vaga da fila.
- **IA com reserva:** em cada etapa você escolhe a IA principal e as reservas. Se a principal falhar ou estiver
  fora do ar, a próxima assume sozinha.

**Plataformas suportadas:** LinkedIn, Indeed e Glassdoor, pelos alertas que chegam no email, e Gupy, pelas buscas
que você cria no Vagora. O desenho aceita novas plataformas (veja o [roadmap](#roadmap)).

## O que você encontra na tela

| Tela | O que tem |
|---|---|
| **Vagas › Analisadas** | Uma régua com todas as notas de 0 a 100 e o corte de 70. Ao lado, a lista de vagas e o detalhe da vaga escolhida: nota, motivos, link para a vaga e o currículo adaptado para copiar ou baixar em PDF. Depois de se candidatar, **marque a vaga como enviada**: ela ganha a data do envio e aparece no filtro **Enviadas**. Tem busca, filtros (todas, compatíveis, enviadas) e ordenação por nota ou data. |
| **Vagas › Na fila** | As vagas já lidas que esperam a análise da IA, com a opção de priorizar uma delas. |
| **Vagas › Gupy** | **Buscas** na Gupy, desligadas até você criar uma: **cargo**, **estado**, **cidade**, **modalidade** (remoto, híbrido, presencial), **tipo de vaga** (CLT, PJ, temporária, estágio, banco de talentos) e de quantas em quantas **horas** buscar (padrão 4). Cada busca traz até 30 vagas novas por vez, publicadas nos últimos 7 dias e com inscrições abertas, que entram direto na fila da IA, sem abrir navegador e sem conta. A página mostra a atividade da Gupy: o que cada busca encontrou, as vagas na fila, as que a IA está analisando e as já analisadas com a nota. |
| **Atividade** | O leitor do LinkedIn, do Indeed e do Glassdoor em tempo real: a vaga sendo lida, a linha do tempo dos passos e, se algo der errado, a explicação, o que fazer e **uma captura da tela** que o navegador estava vendo. |
| **Configurações** | Tudo o que dá para ajustar, separado em abas (abaixo), com a lista do que ainda falta configurar. |

O **monitor** no topo mostra o que o Vagora está fazendo agora (verificando o email, analisando um alerta,
aguardando o intervalo de segurança ou pausado) e tem o botão para pausar.

## Configurações

Cada aba tem o próprio botão de salvar e mostra se há alterações não salvas.

| Aba | O que você configura |
|---|---|
| **Currículo** | O PDF que a IA compara com cada vaga (até 5 MB, com texto selecionável). |
| **IA** | As chaves do **Gemini** e do **Groq**, o **Claude** (pela chave da API da Anthropic ou pelo login da sua assinatura no Claude Code, que o Vagora instala sozinho) e uma **IA local**: o endereço e o modelo de qualquer servidor no formato da OpenAI que você roda ou confia (Ollama, LM Studio, llama.cpp, vLLM). Cada uma tem o campo **Modelo**: vazio usa o padrão do Vagora, e ao clicar nele aparecem os modelos que a sua conta tem (consultados com a sua chave, ou no seu servidor, na IA local). Basta uma IA, mas com mais de uma você ganha reserva. |
| **Fluxo da IA** | Um fluxograma das etapas. Em cada etapa que usa IA (**nota da vaga** e **currículo adaptado**) você marca quais IAs entram e em que ordem: a primeira é a principal e as outras são reservas, tentadas em sequência se a anterior falhar. Exemplo: uma IA com cota grande na nota, que roda para toda vaga, e a que escreve melhor no currículo. |
| **Contas** | O **email** que recebe os alertas (com senha de app) e a **pasta** onde os alertas de cada rede chegam, descoberta automaticamente. A conta do **LinkedIn** usada para abrir as vagas e os leitores do **Indeed** e do **Glassdoor**, cada um com a própria pasta do email. |
| **Automação** | Ligar ou pausar a verificação do email, gerar o currículo adaptado automaticamente ou só quando você pedir (economiza cota da IA) e mostrar o navegador durante a análise (para resolver verificações do LinkedIn). Também o **intervalo entre os acessos**, com mínimo e máximo em minutos e aviso de risco quando fica abaixo do padrão. |
| **Perfil** | O seu nome e a exclusão do perfil com todos os dados. |

## Começando

### Baixar e abrir (o jeito mais fácil)

Baixe o ZIP do seu sistema na página de [**versões do Vagora**](https://github.com/Viniciushfc/vagora/releases/latest).
Ele já traz o Java e o leitor de vagas: não precisa instalar nada além do **Google Chrome**. Se você for usar o
Claude com a sua assinatura, o Vagora instala o **Claude Code** sozinho quando você abre as Configurações; com a
chave da API do Claude, ou com Gemini, Groq ou IA local, ele não é necessário.

| Sistema | Arquivo | Como abrir |
|---|---|---|
| **Windows** | `Vagora-windows-x64.zip` | extraia e dê dois cliques em `Vagora.exe` |
| **macOS** (Apple Silicon) | `Vagora-macos-arm64.zip` | extraia, clique com o botão direito em `Vagora.app` e escolha **Abrir**. Deixe o `Vagora.app` junto da pasta `leitor` |
| **Linux** | `Vagora-linux-x64.zip` | extraia e abra `bin/Vagora` |

O Vagora abre na própria janela, e **fechar a janela encerra o Vagora**. Tudo o que ele guarda fica na pasta
`dados`, ao lado do app: perfil, vagas, notas e a sessão do LinkedIn. Para fazer backup ou trocar de computador,
copie a pasta inteira do Vagora. Para remover, apague a pasta.

> Extraia num lugar onde dá para gravar, como **Documentos** ou a **Área de Trabalho**. Dentro de
> `Arquivos de Programas` o Windows não deixa o Vagora criar a pasta `dados`.

**Primeira vez abrindo:** o app ainda não tem assinatura digital paga, então o sistema avisa.

| Sistema | O que aparece | O que fazer |
|---|---|---|
| Windows | "O Windows protegeu o computador" | **Mais informações › Executar assim mesmo** |
| macOS | "não pode ser aberto porque o desenvolvedor não pode ser verificado" | botão direito em `Vagora.app` › **Abrir**. Se disser que está danificado, rode `xattr -cr Vagora.app` no Terminal, dentro da pasta |

> A pasta `dados` guarda a senha de app do email e o login do LinkedIn. Não compartilhe a pasta do Vagora.

### Rodar pelo código-fonte

Para quem quer acompanhar a versão mais nova ou mexer no código.

#### O que você precisa

| Item | Para quê | Onde conseguir |
|---|---|---|
| **Java 21+ (JDK)** | rodar o Vagora | o script de início instala, ou [adoptium.net](https://adoptium.net) |
| **Python 3.9+** | o leitor de vagas | o script de início instala, ou [python.org](https://www.python.org/downloads) |
| **Google Chrome** | abrir as vagas | o script de início instala, ou [google.com/chrome](https://www.google.com/chrome) |
| **Ao menos uma IA** | a análise das vagas | Gemini: [aistudio.google.com/apikey](https://aistudio.google.com/apikey) · Groq: [console.groq.com/keys](https://console.groq.com/keys) · Claude: chave em [console.anthropic.com/settings/keys](https://console.anthropic.com/settings/keys) ou login da assinatura pela tela · IA local: o endereço do seu servidor |
| *Opcional:* **Claude Code** | usar o Claude com a sua assinatura | o script de início instala, ou [claude.com/claude-code](https://claude.com/claude-code) |
| **Senha de app do email** | ler os alertas (não é a senha normal) | Gmail: [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords) |
| *Opcional:* `xvfb` (Linux) | navegador totalmente invisível | o `run.sh` instala |

#### Instalar e iniciar

**1. Baixe o Vagora.** Com o Git:

```bash
git clone https://github.com/Viniciushfc/vagora.git
cd vagora
```

Sem o Git, baixe o ZIP em **Code › Download ZIP** no GitHub e extraia numa pasta.

**2. Inicie.**

| Sistema | Como iniciar |
|---|---|
| **Linux** e **macOS** | `./run.sh` no terminal, dentro da pasta |
| **Windows** | dois cliques no `run.cmd`, ou `run.cmd` no terminal, dentro da pasta |

**3. Deixe o script preparar tudo.** Na primeira vez ele confere Java 21 (JDK), Python 3.9+ e Google Chrome
(e o Xvfb no Linux) e o Claude Code. O que faltar ele **oferece instalar** e pergunta antes de rodar cada comando:

| Sistema | Instala com |
|---|---|
| Linux | apt, dnf, pacman ou zypper (pede a senha do `sudo`) |
| macOS | Homebrew |
| Windows | `winget`, que já vem no Windows 10 e 11 |

Depois ele cria o ambiente do leitor de vagas e compila o Vagora. A primeira vez demora alguns minutos; nas
seguintes ele só refaz o que mudou e sobe em segundos.

**4. O Vagora abre na própria janela.** Veja abaixo.

#### A janela do Vagora

O Vagora abre no **modo app** do Chrome ou do Edge: uma janela só dele, sem abas nem barra de endereço, com
ícone próprio na barra de tarefas. Parece um programa instalado, mas é o Vagora rodando na sua máquina em
<http://localhost:7777>. Nada sai do seu computador, a não ser os acessos ao email, às vagas e às IAs.

- **Fechou a janela?** O Vagora continua rodando no terminal. Abra <http://localhost:7777> em qualquer navegador,
  ou rode o script de novo: ele percebe que o Vagora já está aberto e mostra o endereço.
- **Para parar o Vagora** de verdade, use **Ctrl+C** no terminal onde ele está rodando.
- **Fixar na barra de tarefas:** com a janela aberta, clique com o botão direito no ícone e escolha *Fixar*.
- **Sem Chrome nem Edge** para a janela, ele abre no navegador padrão.
- **Porta ocupada:** se outro programa estiver na 7777, o Vagora usa a próxima livre (7778, 7779...) e o
  endereço certo aparece no terminal.

A janela do Vagora e o navegador que lê as vagas são coisas diferentes: o leitor usa um perfil próprio do Chrome
(em `python/.profile`), separado do seu, e por padrão roda escondido.

#### Opções do script

| Linux e macOS | Windows | O que faz |
|---|---|---|
| `./run.sh` | `run.cmd` | prepara, compila se preciso e abre a janela |
| `./run.sh --no-browser` | `run.cmd --no-browser` | inicia sem abrir a janela |
| `./run.sh --clean` | `run.cmd --clean` | apaga o build e compila do zero |
| `./run.sh --help` | `run.cmd --help` | mostra as opções |

### Primeiro acesso

O Vagora abre num passo a passo:

1. **Perfil e currículo:** seu nome, uma chave de IA e o currículo em PDF. O Claude pode ser conectado depois, em
   Configurações.
2. **Email:** o email que recebe os alertas e a **senha de app**. O Vagora testa o acesso e **descobre sozinho a
   pasta** onde os alertas chegam (útil se você usa filtros e etiquetas).
3. **LinkedIn:** a conta usada para abrir as vagas. Leia os riscos antes de conectar (abaixo).

Pronto. A partir daí o Vagora trabalha sozinho enquanto estiver aberto.

### Quando o LinkedIn pede verificação

Por padrão o navegador do leitor roda **escondido**. Se o LinkedIn pedir um código ou captcha, o alerta fica
guardado no email e a tela avisa. Ligue **"Mostrar o navegador durante a análise"** em Configurações › Automação:
na próxima análise a janela abre e espera até 5 minutos você resolver. Depois disso a sessão fica salva e você
pode desligar de novo.

## Segurança e riscos

- **Riscos para a conta do LinkedIn:** o contrato do LinkedIn proíbe ferramentas automatizadas. A conta pode
  receber verificações de segurança, avisos de novo acesso ou, em casos repetidos, restrições. O Vagora reduz o
  risco com ritmo humano e pausas, mas **não o elimina**. Intervalos menores que o padrão aumentam o risco, e a
  tela avisa. Se puder, use uma conta secundária só para isso.
- **Suas credenciais ficam só no seu computador**, no arquivo `vagora.db` (já no `.gitignore`). Não compartilhe
  esse arquivo.
- **A API só aceita conexões da própria máquina** (`127.0.0.1`) e não tem login, porque cada instalação é de uma
  pessoa.
- **O que vai para fora:** o texto da vaga e do seu currículo é enviado à IA escolhida (Claude, Gemini, Groq ou o endereço da sua IA local)
  para a análise, usando a sua chave ou a sua conta. Nada mais sai da sua máquina.

## Tecnologias e arquitetura

| Camada | Tecnologia |
|---|---|
| Aplicação | **Java 21**, **Quarkus 3** (REST, Scheduler, Hibernate ORM com Panache) |
| Banco | **SQLite** em arquivo local (`vagora.db`) |
| Email | **IMAP** com Jakarta Mail (Angus Mail) |
| Leitor de vagas | **Python** com **Patchright** (Playwright) controlando o **Google Chrome**; tela virtual com **Xvfb** |
| Análise | **Claude** (SDK `anthropic-java` com chave da API, ou o Claude Code com a sua assinatura), **Google Gemini** (SDK `google-genai`), **Groq** e **IA local** (qualquer servidor no formato da OpenAI), com reserva entre elas |
| Currículo | **Apache PDFBox** para ler o PDF e gerar o currículo adaptado em PDF |
| Interface | **HTML, CSS e JavaScript** puros, servidos pelo próprio Quarkus |
| Testes | JUnit 5, Mockito, REST Assured e H2 em memória |

O projeto segue **Clean Architecture / Hexagonal**: o domínio não conhece Quarkus, banco, email, navegador nem IA.

```
src/main/java/com/viniciushfc
├── domain                      regras de negócio, sem framework
│   ├── entities                Profile, LinkedAccount, EmailReceived, PendingJob, JobMatch
│   ├── usecases                contracts/ (interfaces) e impl/ (um caso de uso por ação)
│   ├── gateways                portas para o mundo externo (email, leitor de vagas, IA, PDF, Claude)
│   ├── repositories            portas de persistência
│   ├── prompts                 instruções enviadas à IA
│   ├── exceptions              categorias seladas de erro + ErrorCode
│   └── vo                      objetos de valor (ProcessingStatus, ScraperActivity, ...)
└── infra
    ├── adapter/in              REST (/profile) e os jobs agendados (email e fila de análise)
    └── adapter/out             SQLite (Panache), IMAP, leitor (Python), Claude, Gemini, Groq, PDFBox

python
├── hub.py                      entrada do leitor: abre o navegador, percorre os links e escolhe o leitor de cada site
├── core.py                     caminhos, erros, códigos de saída e eventos para o Vagora
├── linkedin.py                 login, verificação de segurança e leitura da vaga do LinkedIn
├── indeed.py                   leitura da vaga do Indeed
├── glassdoor.py                leitura da vaga do Glassdoor (bloco JobPosting)
└── human.py                    cliques, digitação e pausas com ritmo humano
src/main/resources/META-INF/resources   interface web
```

**Adicionar uma plataforma nova por email** segue esse desenho: um novo valor em `AlertSource` com o
reconhecimento dos alertas dela, um arquivo em `python/` com a função `scrape(page, url)` e uma linha em `SCRAPERS`,
no `hub.py`. O resto do fluxo (fila, análise, notas, interface) é reaproveitado.

### API

Todas as rotas são do perfil da instalação, sem id:

| Método | Rota | Para quê |
|---|---|---|
| `GET` `POST` `PATCH` `DELETE` | `/profile` | perfil, ajustes e fluxo da IA (criação e edição em `multipart/form-data`) |
| `GET` | `/profile/curriculum` | currículo em PDF |
| `POST` / `DELETE` | `/profile/accounts` · `/profile/accounts/{provider}` | conectar / desconectar conta (`EMAIL`, `LINKEDIN`, `CLAUDE`) |
| `POST` | `/profile/accounts/LINKEDIN/session` | abrir o navegador para entrar no LinkedIn |
| `GET` `POST` | `/profile/accounts/CLAUDE/session` · `/profile/accounts/CLAUDE/code` | login no Claude |
| `POST` | `/profile/accounts/CLAUDE/install` | instala o Claude Code neste computador |
| `GET` / `PUT` | `/profile/email/folders` · `/profile/email/folder` | pastas do email com contagem de alertas / trocar a pasta |
| `GET` | `/profile/job-matches` | vagas analisadas |
| `GET` / `POST` | `/profile/job-matches/{id}/curriculum` | currículo adaptado em PDF / gerar o currículo adaptado |
| `GET` | `/profile/llm-models` | modelo padrão de cada IA |
| `GET` | `/profile/llm-models/{provider}` | modelos disponíveis de uma IA (`GEMINI`, `GROQ`, `CLAUDE`, `LOCAL`) |
| `PUT` | `/profile/job-matches/{id}/applied` | marcar ou desmarcar a vaga como enviada (`{"applied": true}`) |
| `GET` / `POST` | `/profile/pending-jobs` · `/profile/pending-jobs/{id}/priority` | fila de análise / priorizar uma vaga |
| `GET` | `/profile/status` | monitoramento (última verificação, próximo alerta, último erro) |
| `GET` / `POST` | `/gupy/searches` | buscas da Gupy / criar uma busca |
| `PUT` / `DELETE` | `/gupy/searches/{id}` | editar / excluir uma busca |
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
./run.sh --clean     # build do zero e o app como o usuário roda
./scripts/portable.sh   # gera o ZIP para download do sistema atual em target/
```

**Publicar uma versão:** crie e envie uma tag com o número da versão (`git tag v1.2.0 && git push origin v1.2.0`).
O GitHub Actions gera os ZIPs de Windows, macOS e Linux, cada um no próprio sistema, com esse número no app, e anexa
todos na página da versão. Uma tag já enviada não pode ser reaproveitada: para refazer, use o próximo número.

## Roadmap

- [x] LinkedIn: leitura dos alertas, abertura das vagas e análise
- [x] Indeed: leitura dos alertas, abertura das vagas e análise
- [~] Glassdoor: leitura dos alertas pronta; a abertura das vagas depende de sessão logada no Glassdoor
- [x] Gupy: buscas com filtros de cargo, local, modalidade e tipo de vaga, direto na fila da IA
- [x] Currículo adaptado para ATS, em texto e PDF
- [x] Claude, Gemini, Groq e IA local (qualquer servidor no formato da OpenAI), com fluxo e reservas configuráveis por etapa
- [x] Intervalo entre acessos configurável
- [x] Linux, macOS e Windows, com instalação do que falta
- [x] ZIP para download com o Java embutido e os dados junto do app
- [ ] **Novas plataformas de vagas:** Catho e outras
- [x] Marcar a vaga como enviada e filtrar as candidaturas enviadas
- [ ] Notificação quando surgir uma vaga compatível

## Licença

[MIT](LICENSE) © 2026 Vinicius Henry Ferrari Campos
