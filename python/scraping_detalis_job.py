"""
Extrai os detalhes das vagas dos links do email de alerta do LinkedIn.

  echo '["https://..."]' | python scraping_detalis_job.py   # como o Vagora chama
  python scraping_detalis_job.py --login   # abre uma janela, preenche login e senha e espera a pessoa terminar o login

Variáveis de ambiente (definidas pelo Vagora):
  VAGORA_HEADLESS=1     roda sem janela

No stdout, uma linha JSON por vaga lida, enviada assim que a vaga termina de carregar.
Códigos de saída: 0 ok, 3 verificação de segurança, 4 login recusado,
5 a página da vaga não carregou como esperado, 6 o navegador foi fechado, 1 outra falha.
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

from patchright._impl._errors import TargetClosedError
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
CHECKPOINT_WAIT_MS = 5 * 60_000

EXIT_FAILURE, EXIT_CHECKPOINT, EXIT_LOGIN_FAILED, EXIT_PAGE_UNREADABLE, EXIT_BROWSER_CLOSED = 1, 3, 4, 5, 6


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
    password = page.locator('input[name="session_password"]').first
    password.wait_for(timeout=10_000)
    human.type(password, os.environ["LINKEDIN_PASSWORD"])
    page.keyboard.press("Enter")
    try:
        page.wait_for_url(lambda u: "/login" not in u and "/authwall" not in u, timeout=30_000)
    except PlaywrightTimeout:
        raise LoginFailed("o LinkedIn não aceitou o login")


def handle_checkpoint(page):
    if "/checkpoint/" not in page.url:
        return
    emit("checkpoint_wait")
    try:
        page.wait_for_url(lambda u: "/checkpoint/" not in u, timeout=CHECKPOINT_WAIT_MS)
    except PlaywrightTimeout:
        raise Checkpoint("verificação de segurança não resolvida a tempo")
    emit("checkpoint_resolved")


def settle(page, timeout_s=20):
    deadline = time.monotonic() + timeout_s
    while time.monotonic() < deadline:
        if urlparse(page.url).path.startswith(("/checkpoint/", "/authwall", "/login")):
            return
        try:
            if page.locator(JOB).or_(page.locator(CLOSED)).first.is_visible():
                return
        except Exception:
            pass
        page.wait_for_timeout(500)


def on_feed(url):
    return urlparse(url).path.startswith("/feed")


def save_session(page):
    page.goto("https://www.linkedin.com/feed/", wait_until="domcontentloaded")
    pause(1.5, 3.5)
    if on_feed(page.url):
        return
    emit("session_wait")
    try:
        login(page, Human(page))
    except Exception as e:
        log(f"login automático não passou, segue com a pessoa: {str(e).splitlines()[0]}")
    try:
        page.wait_for_url(on_feed, timeout=CHECKPOINT_WAIT_MS)
    except PlaywrightTimeout:
        raise Checkpoint("o login não terminou a tempo")


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
    settle(page)
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
            try:
                save_session(browser.pages[0])
            except Exception as e:
                log(f"login: {str(e).splitlines()[0]}")
                stop(browser, e)
            emit("session_saved")
            browser.close()
            return

        SCREENSHOT.unlink(missing_ok=True)
        links = json.load(sys.stdin)
        emit("start", total=len(links))
        read = 0
        unreadable = 0
        for i, url in enumerate(links, 1):
            if i > 1:
                wait = random.uniform(8, 25)
                emit("pause", seconds=round(wait))
                time.sleep(wait)
            emit("job", index=i, total=len(links))
            page = None
            try:
                page = browser.new_page()   # aba nova, como o clique no email
                job = scrape(page, url)
                if job is None:
                    emit("closed", index=i)
                else:
                    print(json.dumps(job, ensure_ascii=False), flush=True)
                    read += 1
                    emit("read", index=i, title=job["title"])
            except PageUnreadable as e:
                # uma vaga que não carrega não derruba o alerta: pula e guarda a tela para conferir
                log(f"pulando vaga {i}: {str(e).splitlines()[0]}")
                unreadable += 1
                try:
                    page.screenshot(path=SCREENSHOT)
                except Exception:
                    pass
                emit("unreadable", index=i)
            except Exception as e:
                # sem resultado parcial: o alerta inteiro é tentado de novo no próximo ciclo
                log(f"parando: {str(e).splitlines()[0]}")   # 1a linha: sem a URL com token
                try:
                    page.screenshot(path=SCREENSHOT)
                except Exception:
                    pass
                emit("fail", index=i, screenshot=SCREENSHOT.exists())
                stop(browser, e)
            finally:
                if page and not page.is_closed():
                    page.close()

        browser.close()
    if unreadable and not read:
        # nenhuma vaga carregou: provavelmente login ou LinkedIn fora do ar; o alerta é tentado de novo
        log("nenhuma vaga carregou neste alerta")
        emit("fail", screenshot=SCREENSHOT.exists())
        sys.exit(EXIT_PAGE_UNREADABLE)
    emit("done", count=read)


def stop(browser, error):
    try:
        browser.close()
    except Exception:
        pass
    sys.exit(exit_code(error))


def exit_code(error):
    if isinstance(error, Checkpoint):
        return EXIT_CHECKPOINT
    if isinstance(error, LoginFailed):
        return EXIT_LOGIN_FAILED
    if isinstance(error, PageUnreadable):
        return EXIT_PAGE_UNREADABLE
    if isinstance(error, TargetClosedError):
        return EXIT_BROWSER_CLOSED
    return EXIT_FAILURE


if __name__ == "__main__":
    main()
