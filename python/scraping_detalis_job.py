"""
Extrai os detalhes das vagas dos links do email de alerta do LinkedIn.

  echo '["https://..."]' | python scraping_detalis_job.py   # como o Vagora chama
  python scraping_detalis_job.py --login   # opcional: login manual numa janela, se o automático não passar

Variáveis de ambiente (definidas pelo Vagora):
  VAGORA_HEADLESS=1     roda sem janela
  VAGORA_INTERACTIVE=1  a janela está na tela: espera a pessoa resolver verificações do LinkedIn

Códigos de saída: 0 ok (JSON no stdout), 3 verificação de segurança, 4 login recusado,
5 a página da vaga não carregou como esperado, 1 outra falha.
No stderr, linhas "@@ {json}" são eventos de progresso para o Vagora; as demais são log técnico.
Na falha, a tela do navegador é salva em last-failure.png.
"""

import json
import os
import random
import re
import sys
import time
from pathlib import Path
from urllib.parse import urlparse

from patchright.sync_api import TimeoutError as PlaywrightTimeout, sync_playwright

from human import Human, pause

PROFILE = Path(__file__).parent / ".profile"
SCREENSHOT = Path(__file__).parent / "last-failure.png"
SECTION = 'xpath=//h2[{}]/ancestor::div[.//*[@data-testid="expandable-text-box"]][1]'


def section(*titles):
    # o LinkedIn pode vir em português ou inglês
    return SECTION.format(" or ".join(f'normalize-space()="{t}"' for t in titles))


JOB = section("Sobre a vaga", "About the job")
COMPANY = section("Sobre a empresa", "About the company")
TEXT = '[data-testid="expandable-text-box"]'
MORE = '[data-testid="expandable-text-button"]'

# aviso do LinkedIn em vagas encerradas (site em português ou inglês)
CLOSED = "text=/Não aceita mais candidaturas|No longer accepting applications/i"

HEADLESS = os.environ.get("VAGORA_HEADLESS") == "1"
INTERACTIVE = os.environ.get("VAGORA_INTERACTIVE") == "1"
CHECKPOINT_WAIT_MS = 5 * 60_000

EXIT_FAILURE, EXIT_CHECKPOINT, EXIT_LOGIN_FAILED, EXIT_PAGE_UNREADABLE = 1, 3, 4, 5


class Checkpoint(Exception):
    """O LinkedIn pediu captcha, código por email ou outra verificação."""


class LoginFailed(Exception):
    """O LinkedIn recusou o login (senha errada ou conta bloqueada)."""


class PageUnreadable(Exception):
    """A página abriu, mas a descrição da vaga não apareceu (lentidão, layout novo ou tela de login)."""


def log(msg):
    print(f"[scraper] {msg}", file=sys.stderr, flush=True)


def emit(event, **data):
    # progresso estruturado: o Vagora traduz para a página de acompanhamento
    print("@@ " + json.dumps({"event": event, **data}, ensure_ascii=False), file=sys.stderr, flush=True)


def text(page, section):
    box = page.locator(section).locator(TEXT).first
    return box.inner_text().strip() if box.count() else None


def login(page, human):
    email = page.locator('input[name="session_key"]').first
    if email.count() and email.is_visible() and not email.input_value():
        human.type(email, os.environ["LINKEDIN_LOGIN"])
    password = page.locator('input[type="password"]').first
    password.wait_for(timeout=10_000)
    human.type(password, os.environ["LINKEDIN_PASSWORD"])
    page.keyboard.press("Enter")
    try:
        page.wait_for_url(lambda u: "/jobs/view/" in u or "/checkpoint/" in u, timeout=30_000)
    except Exception:
        raise LoginFailed("o LinkedIn não aceitou o login")


def handle_checkpoint(page):
    if "/checkpoint/" not in page.url:
        return
    if not INTERACTIVE:
        raise Checkpoint("verificação de segurança com o navegador escondido")
    emit("checkpoint_wait")
    try:
        page.wait_for_url(lambda u: "/checkpoint/" not in u, timeout=CHECKPOINT_WAIT_MS)
    except Exception:
        raise Checkpoint("verificação de segurança não resolvida a tempo")
    emit("checkpoint_resolved")


def parse_title(page_title):
    """'(3) Vaga | Empresa | LinkedIn' -> ('Vaga', 'Empresa'); tolera títulos fora desse formato."""
    parts = [p.strip() for p in re.sub(r"^\(\d+\)\s*", "", page_title).split(" | ")]
    if parts[-1] == "LinkedIn":
        parts = parts[:-1]
    if len(parts) < 2:
        return (parts[0] if parts else ""), ""
    # a empresa é a última parte; o título da vaga pode conter " | "
    return " | ".join(parts[:-1]), parts[-1]


def scrape(page, url):
    page.goto(url, wait_until="domcontentloaded")
    pause(1.5, 3.5)
    human = Human(page)

    if "/login" in page.url or "/authwall" in page.url:
        emit("login")
        login(page, human)
    handle_checkpoint(page)

    # espera a descrição ou o aviso de vaga encerrada, o que aparecer primeiro
    try:
        page.locator(JOB).or_(page.locator(CLOSED)).first.wait_for(timeout=15_000)
    except PlaywrightTimeout:
        raise PageUnreadable(f"descrição não apareceu em 15 s (página: {urlparse(page.url).path}, título: {page.title()!r})")
    if page.locator(CLOSED).count():
        return None
    human.idle()

    more = page.locator(JOB).locator(MORE).first
    if more.count():
        human.click(more)
    description = text(page, JOB)
    human.read(description)

    if page.locator(COMPANY).count():
        human.scroll_into_view(page.locator(COMPANY).first)
    company_description = text(page, COMPANY)

    # título da aba: "Título da vaga | Empresa | LinkedIn"
    title, company = parse_title(page.title())
    job_id = re.search(r"/jobs/view/(\d+)", url).group(1)
    return {
        "link": f"https://www.linkedin.com/jobs/view/{job_id}/",
        "title": title,
        "description": description,
        "companyDetails": {"name": company, "description": company_description},
    }


def main():
    with sync_playwright() as pw:
        browser = pw.chromium.launch_persistent_context(
            PROFILE, channel="chrome", headless=HEADLESS, no_viewport=True, locale="pt-BR")

        if "--login" in sys.argv:
            browser.pages[0].goto("https://www.linkedin.com/login")
            input("Faça login no browser e pressione ENTER...")
            browser.close()
            return

        SCREENSHOT.unlink(missing_ok=True)
        links = json.load(sys.stdin)
        emit("start", total=len(links))
        results = []
        for i, url in enumerate(links, 1):
            if i > 1:
                wait = random.uniform(8, 25)
                emit("pause", seconds=round(wait))
                time.sleep(wait)
            emit("job", index=i, total=len(links))
            page = browser.new_page()   # aba nova, como o clique no email
            try:
                job = scrape(page, url)
                if job is None:
                    emit("closed", index=i)
                else:
                    emit("read", index=i, title=job["title"])
                    results.append(job)
            except Exception as e:
                # sem resultado parcial: o alerta inteiro é tentado de novo no próximo ciclo
                log(f"parando: {str(e).splitlines()[0]}")   # 1a linha: sem a URL com token
                try:
                    page.screenshot(path=SCREENSHOT)
                except Exception:
                    pass
                emit("fail", index=i, screenshot=SCREENSHOT.exists())
                browser.close()
                sys.exit(exit_code(e))
            finally:
                if not page.is_closed():
                    page.close()

        browser.close()
    emit("done", count=len(results))
    json.dump(results, sys.stdout, ensure_ascii=False)


def exit_code(error):
    if isinstance(error, Checkpoint):
        return EXIT_CHECKPOINT
    if isinstance(error, LoginFailed):
        return EXIT_LOGIN_FAILED
    if isinstance(error, PageUnreadable):
        return EXIT_PAGE_UNREADABLE
    return EXIT_FAILURE


if __name__ == "__main__":
    main()
