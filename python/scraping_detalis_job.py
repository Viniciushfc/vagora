"""
Extrai os detalhes das vagas dos links do email de alerta do LinkedIn.

  echo '["https://..."]' | python scraping_detalis_job.py   # como o Vagora chama
  python scraping_detalis_job.py --login   # opcional: login manual numa janela, se o automático não passar

Variáveis de ambiente (definidas pelo Vagora):
  VAGORA_HEADLESS=1     roda sem janela
  VAGORA_INTERACTIVE=1  a janela está na tela: espera a pessoa resolver verificações do LinkedIn

Códigos de saída: 0 ok (JSON no stdout), 3 verificação de segurança, 4 login recusado, 1 outra falha.
"""

import json
import os
import random
import re
import sys
import time
from pathlib import Path

from patchright.sync_api import sync_playwright

from human import Human, pause

PROFILE = Path(__file__).parent / ".profile"
SECTION = 'xpath=//h2[normalize-space()="{}"]/ancestor::div[.//*[@data-testid="expandable-text-box"]][1]'
JOB = SECTION.format("Sobre a vaga")
COMPANY = SECTION.format("Sobre a empresa")
TEXT = '[data-testid="expandable-text-box"]'
MORE = '[data-testid="expandable-text-button"]'

# aviso do LinkedIn em vagas encerradas (site em português ou inglês)
CLOSED = "text=/Não aceita mais candidaturas|No longer accepting applications/i"

HEADLESS = os.environ.get("VAGORA_HEADLESS") == "1"
INTERACTIVE = os.environ.get("VAGORA_INTERACTIVE") == "1"
CHECKPOINT_WAIT_MS = 5 * 60_000

EXIT_FAILURE, EXIT_CHECKPOINT, EXIT_LOGIN_FAILED = 1, 3, 4


class Checkpoint(Exception):
    """O LinkedIn pediu captcha, código por email ou outra verificação."""


class LoginFailed(Exception):
    """O LinkedIn recusou o login (senha errada ou conta bloqueada)."""


def log(msg):
    print(f"[scraper] {msg}", file=sys.stderr, flush=True)


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
    log("verificação de segurança: resolva na janela do navegador (até 5 minutos)")
    try:
        page.wait_for_url(lambda u: "/checkpoint/" not in u, timeout=CHECKPOINT_WAIT_MS)
    except Exception:
        raise Checkpoint("verificação de segurança não resolvida a tempo")
    log("verificação resolvida")


def scrape(page, url):
    page.goto(url, wait_until="domcontentloaded")
    pause(1.5, 3.5)
    human = Human(page)

    if "/login" in page.url or "/authwall" in page.url:
        log("sessão expirada: fazendo login")
        login(page, human)
    handle_checkpoint(page)

    # espera a descrição ou o aviso de vaga encerrada, o que aparecer primeiro
    page.locator(JOB).or_(page.locator(CLOSED)).first.wait_for(timeout=15_000)
    if page.locator(CLOSED).count():
        log("vaga encerrada: não aceita mais candidaturas, pulando")
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
    title, company, _ = page.title().rsplit(" | ", 2)
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
            PROFILE, channel="chrome", headless=HEADLESS, no_viewport=True)

        if "--login" in sys.argv:
            browser.pages[0].goto("https://www.linkedin.com/login")
            input("Faça login no browser e pressione ENTER...")
            browser.close()
            return

        results = []
        for i, url in enumerate(json.load(sys.stdin), 1):
            if i > 1:
                time.sleep(random.uniform(8, 25))
            log(f"vaga {i}")
            page = browser.new_page()   # aba nova, como o clique no email
            try:
                job = scrape(page, url)
                if job is not None:
                    results.append(job)
            except Exception as e:
                # sem resultado parcial: o alerta inteiro é tentado de novo no próximo ciclo
                log(f"parando: {str(e).splitlines()[0]}")   # 1a linha: sem a URL com token
                browser.close()
                sys.exit(exit_code(e))
            finally:
                if not page.is_closed():
                    page.close()

        browser.close()
    json.dump(results, sys.stdout, ensure_ascii=False)


def exit_code(error):
    if isinstance(error, Checkpoint):
        return EXIT_CHECKPOINT
    if isinstance(error, LoginFailed):
        return EXIT_LOGIN_FAILED
    return EXIT_FAILURE


if __name__ == "__main__":
    main()
