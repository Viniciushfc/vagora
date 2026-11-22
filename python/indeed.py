from urllib.parse import urlparse

from patchright.sync_api import TimeoutError as PlaywrightTimeout

from core import PageUnreadable
from human import Human, pause

TEXT = "#jobDescriptionText, .simple-job-description-html"
CLOSED = "text=/Esta vaga expirou|vaga não está mais disponível|This job has expired/i"

def meta(page, selector):
    tag = page.locator(selector).first
    return (tag.get_attribute("content") or "").strip() if tag.count() else ""

def scrape(page, url):
    page.goto(url, wait_until="domcontentloaded")
    pause(1.5, 3.5)
    human = Human(page)
    try:
        page.locator(TEXT).or_(page.locator(CLOSED)).first.wait_for(timeout=15_000)
    except PlaywrightTimeout:
        raise PageUnreadable(f"descrição não apareceu em 15 s (página: {urlparse(page.url).path}, título: {page.title()!r})")
    if page.locator(CLOSED).count():
        return None
    human.idle()
    description = page.locator(TEXT).first.inner_text().strip()
    human.read(description)
    return {
        "link": url,
        "title": meta(page, "#indeed-share-message") or page.title().split(" - ")[0],
        "description": description,
        "companyDetails": {"name": meta(page, 'meta[property="og:description"]'), "description": None},
    }
