import json
import os
import sys
from pathlib import Path

from patchright._impl._errors import TargetClosedError

PROFILE = Path(os.environ.get("VAGORA_PROFILE") or Path(__file__).parent / ".profile")
SCREENSHOT = Path(os.environ.get("VAGORA_SCREENSHOT") or Path(__file__).parent / "last-failure.png")
HEADLESS = os.environ.get("VAGORA_HEADLESS") == "1"

EXIT_FAILURE, EXIT_CHECKPOINT, EXIT_LOGIN_FAILED, EXIT_PAGE_UNREADABLE, EXIT_BROWSER_CLOSED = 1, 3, 4, 5, 6

class Checkpoint(Exception):
    pass

class LoginFailed(Exception):
    pass

class PageUnreadable(Exception):
    pass

def log(msg):
    print(f"[scraper] {msg}", file=sys.stderr, flush=True)

def emit(event, **data):
    print("@@ " + json.dumps({"event": event, **data}, ensure_ascii=False), file=sys.stderr, flush=True)

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
