import json
import random
import sys
import time
from urllib.parse import urlparse

from patchright.sync_api import sync_playwright

import glassdoor
import indeed
import linkedin
from core import HEADLESS, PROFILE, SCREENSHOT, EXIT_PAGE_UNREADABLE, PageUnreadable, emit, exit_code, log

SCRAPERS = [("indeed.com", indeed.scrape), ("glassdoor.", glassdoor.scrape)]

def scraper_for(url):
    host = urlparse(url).hostname or ""
    return next((scrape for domain, scrape in SCRAPERS if domain in host), linkedin.scrape)

def main():
    with sync_playwright() as pw:
        browser = pw.chromium.launch_persistent_context(
            PROFILE, channel="chrome", headless=HEADLESS, no_viewport=True, locale="pt-BR")

        if "--login" in sys.argv:
            try:
                linkedin.save_session(browser.pages[0])
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
        closed = 0
        unreadable = 0
        for i, url in enumerate(links, 1):
            if i > 1:
                wait = random.uniform(8, 25)
                emit("pause", seconds=round(wait))
                time.sleep(wait)
            emit("job", index=i, total=len(links))
            page = None
            try:
                page = browser.new_page()
                job = scraper_for(url)(page, url)
                if job is None:
                    closed += 1
                    emit("closed", index=i)
                else:
                    print(json.dumps(job, ensure_ascii=False), flush=True)
                    read += 1
                    emit("read", index=i, title=job["title"])
            except PageUnreadable as e:
                log(f"pulando vaga {i}: {str(e).splitlines()[0]}")
                unreadable += 1
                try:
                    page.screenshot(path=SCREENSHOT)
                except Exception:
                    pass
                emit("unreadable", index=i)
            except Exception as e:
                log(f"parando: {str(e).splitlines()[0]}")
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
    if unreadable and not read and not closed:
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

if __name__ == "__main__":
    main()
