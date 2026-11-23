import html
import json
import re
import time
from urllib.parse import parse_qs, urlparse

from core import PageUnreadable
from human import Human, pause

POSTING = 'script[type="application/ld+json"]'
PANEL_TITLE = 'h1[id^="jd-job-title-"]'
PANEL_DESCRIPTION = '[class*="JobDetails_jobDescription"]'
PANEL_COMPANY = 'header[data-test="job-details-header"] h4'
CLOSED = "text=/Essa vaga expirou|Vaga expirada|não está mais disponível|This job has expired/i"
REMOTE = {"TELECOMMUTE": "Remoto"}
PERIOD = {"HOUR": "hora", "DAY": "dia", "WEEK": "semana", "MONTH": "mês", "YEAR": "ano"}
CONTRACT = {"FULL_TIME": "Tempo integral", "PART_TIME": "Meio período", "CONTRACTOR": "PJ / contrato",
            "TEMPORARY": "Temporária", "INTERN": "Estágio"}

def html_to_text(markup):
    markup = re.sub(r"(?i)<br\s*/?>|</(p|li|h\d|div)>", "\n", markup)
    markup = re.sub(r"(?i)<li[^>]*>", "- ", markup)
    return re.sub(r"\n{3,}", "\n\n", html.unescape(re.sub(r"<[^>]+>", "", markup))).strip()

def job_posting(page):
    for script in page.locator(POSTING).all():
        try:
            data = json.loads(script.text_content() or "")
        except json.JSONDecodeError:
            continue
        if isinstance(data, dict) and data.get("@type") == "JobPosting":
            return data
    return None

def panel_job(page, listing):
    title = page.locator(PANEL_TITLE)
    if not title.count():
        return None
    if title.first.get_attribute("id") != f"jd-job-title-{listing}":
        return False
    description = page.locator(PANEL_DESCRIPTION)
    if not description.count():
        return None
    company = page.locator(PANEL_COMPANY)
    return {
        "title": title.first.inner_text().strip(),
        "description": html_to_text(description.first.inner_html()),
        "company": company.first.inner_text().strip() if company.count() else None,
    }

def header(posting):
    parts = []
    if posting.get("jobLocationType") in REMOTE:
        parts.append("Modalidade: " + REMOTE[posting["jobLocationType"]])
    contracts = [CONTRACT.get(kind, kind) for kind in posting.get("employmentType") or []]
    if contracts:
        parts.append("Contratação: " + ", ".join(contracts))
    salary = (posting.get("baseSalary") or {}).get("value") or {}
    if salary.get("minValue"):
        parts.append(f"Salário: {salary['minValue']} a {salary.get('maxValue', salary['minValue'])} {posting.get('salaryCurrency', '')} por {PERIOD.get(salary.get('unitText'), salary.get('unitText', ''))}".strip())
    return ". ".join(parts) + ("." if parts else "")

def scrape(page, url):
    page.goto(url, wait_until="domcontentloaded")
    pause(1.5, 3.5)
    human = Human(page)
    listing = parse_qs(urlparse(url).query).get("jl", [None])[0]
    posting = panel = None
    deadline = time.time() + 15
    while time.time() < deadline and posting is None and not panel:
        if page.locator(CLOSED).count():
            return None
        if page.title().startswith("Security"):
            raise PageUnreadable("o Glassdoor mostrou a tela de segurança e bloqueou a leitura da vaga")
        posting = job_posting(page)
        if posting is None:
            panel = panel_job(page, listing)
            if panel is False:
                return None
            if panel is None:
                page.wait_for_timeout(1000)
    if posting is None and not panel:
        raise PageUnreadable(f"dados da vaga não apareceram em 15 s (página: {urlparse(page.url).path}, título: {page.title()!r})")
    human.idle()
    if panel:
        human.read(panel["description"])
        return {"link": url, "title": panel["title"], "description": panel["description"],
                "companyDetails": {"name": panel["company"], "description": None}}
    description = html_to_text(posting.get("description") or "")
    human.read(description)
    summary = header(posting)
    return {
        "link": url,
        "title": posting.get("title") or page.title().split(" | ")[0],
        "description": (summary + "\n\n" + description) if summary else description,
        "companyDetails": {"name": (posting.get("hiringOrganization") or {}).get("name"), "description": None},
    }
