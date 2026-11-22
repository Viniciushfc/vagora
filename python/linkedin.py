import os
import re
import time
from urllib.parse import urlparse

from patchright.sync_api import TimeoutError as PlaywrightTimeout

from core import Checkpoint, LoginFailed, PageUnreadable, emit, log
from human import Human, pause

SECTION = 'xpath=//h2[{}]/ancestor::div[.//*[@data-testid="expandable-text-box"]][1]'

def section(*titles):
    return SECTION.format(" or ".join(f'normalize-space()="{t}"' for t in titles))

JOB = section("Sobre a vaga", "About the job")
COMPANY = section("Sobre a empresa", "About the company")
TEXT = '[data-testid="expandable-text-box"]'
MORE = '[data-testid="expandable-text-button"]'

CLOSED = "text=/Não aceita mais candidaturas|No longer accepting applications/i"

CHECKPOINT_WAIT_MS = 5 * 60_000

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
    parts = [p.strip() for p in re.sub(r"^\(\d+\)\s*", "", page_title).split(" | ")]
    if parts[-1] == "LinkedIn":
        parts = parts[:-1]
    if len(parts) < 2:
        return (parts[0] if parts else ""), ""
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

    title, company = parse_title(page.title())
    return {
        "link": url,
        "title": title,
        "description": description,
        "companyDetails": {"name": company, "description": company_description},
    }
