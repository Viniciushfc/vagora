"""Mouse, teclado e rolagem com aparência humana."""

import math
import random
import time


def pause(a, b):
    time.sleep(random.uniform(a, b))


def clamp(v, lo, hi):
    return max(lo, min(hi, v))


class Human:
    def __init__(self, page):
        self.page = page
        w, h = self.viewport()
        self.x, self.y = w * random.uniform(0.3, 0.7), h * random.uniform(0.3, 0.7)
        page.mouse.move(self.x, self.y)

    def viewport(self):
        return self.page.evaluate("() => [innerWidth, innerHeight]")

    def move_to(self, x, y):
        """Curva de Bézier, acelera e desacelera, tremor que some perto do alvo."""
        w, h = self.viewport()
        x, y = clamp(x, 2, w - 2), clamp(y, 2, h - 2)
        dx, dy = x - self.x, y - self.y
        dist = math.hypot(dx, dy)
        if dist < 2:
            return

        bend = dist * random.uniform(-0.3, 0.3)
        nx, ny = -dy / dist * bend, dx / dist * bend   # desvio para um lado da reta
        p0, p3 = (self.x, self.y), (x, y)
        p1 = (self.x + dx * 0.3 + nx, self.y + dy * 0.3 + ny)
        p2 = (self.x + dx * 0.7 + nx * 0.5, self.y + dy * 0.7 + ny * 0.5)

        duration = 0.12 + 0.11 * math.log2(1 + dist / 15)   # lei de Fitts
        steps = max(8, int(duration * 60))
        for i in range(1, steps + 1):
            s = i / steps
            t = 10 * s**3 - 15 * s**4 + 6 * s**5             # minimum-jerk
            u = 1 - t
            px = u**3 * p0[0] + 3 * u**2 * t * p1[0] + 3 * u * t**2 * p2[0] + t**3 * p3[0]
            py = u**3 * p0[1] + 3 * u**2 * t * p1[1] + 3 * u * t**2 * p2[1] + t**3 * p3[1]
            tremor = (1 - s) * 1.2
            self.page.mouse.move(px + random.gauss(0, tremor), py + random.gauss(0, tremor))
            time.sleep(duration / steps * random.uniform(0.6, 1.4))
        self.x, self.y = x, y

    def click(self, locator):
        self.scroll_into_view(locator)
        box = locator.bounding_box()
        self.move_to(box["x"] + box["width"] * random.uniform(0.3, 0.7),
                     box["y"] + box["height"] * random.uniform(0.3, 0.7))
        pause(0.08, 0.35)
        self.page.mouse.down()
        pause(0.05, 0.15)
        self.page.mouse.up()

    def type(self, locator, text):
        self.click(locator)
        for ch in text:
            self.page.keyboard.type(ch)
            pause(0.05, 0.25)

    def idle(self):
        for _ in range(random.randint(1, 3)):
            self.move_to(self.x + random.gauss(0, 120), self.y + random.gauss(0, 80))
            pause(0.2, 1.0)

    def scroll(self, px):
        for _ in range(max(1, round(abs(px) / 100))):
            self.page.mouse.wheel(0, 100 if px > 0 else -100)
            pause(0.05, 0.4)

    def scroll_into_view(self, locator):
        _, h = self.viewport()
        for _ in range(20):
            box = locator.bounding_box()
            diff = box["y"] - h * 0.4
            if abs(diff) < h * 0.2:
                return
            self.scroll(clamp(diff, -400, 400))

    def read(self, text):
        """Tempo proporcional ao texto, passando o olho, com rolagem e mouse."""
        end = time.monotonic() + clamp(len((text or "").split()) / 20, 2, 30)
        while time.monotonic() < end:
            random.choice((lambda: self.scroll(random.randint(100, 300)), self.idle))()
            pause(0.8, 2.5)
