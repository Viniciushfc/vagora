"""
Extrai os detalhes das vagas dos links do email de alerta do LinkedIn.

  python scraping_detalis_job.py --login            # login manual (1a vez)
  echo '["https://..."]' | python scraping_detalis_job.py
"""

import json
import os
import random
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
    page.wait_for_url("**/jobs/view/**", timeout=30_000)


def scrape(page, url):
    page.goto(url, wait_until="domcontentloaded")
    pause(1.5, 3.5)
    human = Human(page)

    if "/login" in page.url or "/authwall" in page.url:
        log("sessão expirada: fazendo login")
        login(page, human)
    if "/checkpoint/" in page.url:
        raise RuntimeError("desafio de segurança, rode --login")

    page.locator(JOB).first.wait_for(timeout=15_000)
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
    return {
        "title": title,
        "description": description,
        "companyDetails": {"name": company, "descrition": company_description},
    }


def main():
    with sync_playwright() as pw:
        browser = pw.chromium.launch_persistent_context(
            PROFILE, channel="chrome", headless=False, no_viewport=True)

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
                results.append(scrape(page, url))
            except Exception as e:
                log(f"parando: {str(e).splitlines()[0]}")   # 1a linha: sem a URL com token
                break
            finally:
                page.close()

        browser.close()
    json.dump(results, sys.stdout, ensure_ascii=False)


if __name__ == "__main__":
    main()
