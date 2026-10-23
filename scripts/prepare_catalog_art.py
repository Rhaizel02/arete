"""Optional developer-only reproduction of the six reviewed CC BY-SA 3.0 assets.

Not used by Gradle or the Android app. Requires Pillow. No style changes or AI processing.
Source PNGs stay in ignored app/build; only optimized WebP files ship with Arete.
"""
from pathlib import Path
from hashlib import sha256
from io import BytesIO
from urllib.request import urlopen
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = [
    ("bench_press_extended", "192/Bench-press-1.png", "917e88d61aefa05cf46a98e44e41630343c0b2c821624ca0ac41c07dfedbceed"),
    ("bench_press_lowered", "192/Bench-press-2.png", "8f7da56e008f2e08e04e27c8702bfa223587029ed2bf186cee27ad9c312269c5"),
    ("incline_dumbbell_lowered", "16/Incline-press-1.png", "53b0545c1bbe7dc5d7db758d4e510f7331f4b73d1ec291d4760f1e6d421ad6f9"),
    ("incline_dumbbell_extended", "16/Incline-press-2.png", "e5a431a2475859436b32fcedefa316543c8dd2f72fb70e9980020e2ff3a21f34"),
    ("lateral_raise_lowered", "148/lateral-dumbbell-raises-large-2.png", "acdcbbc93c7242d837f771d3422e26ad627da8faf2a3b133416fe886f8cc3cce"),
    ("lateral_raise_raised", "148/lateral-dumbbell-raises-large-1.png", "7c31318be41a8ed82d6390d52eebda7ca280c18bb7727cf27f04c35c33eb3cdf"),
]

if __name__ == "__main__":
    output = ROOT / "app/src/main/res/drawable-nodpi"
    originals = ROOT / "app/build/catalog-research"
    output.mkdir(parents=True, exist_ok=True)
    originals.mkdir(parents=True, exist_ok=True)
    for name, relative, expected in ASSETS:
        url = "https://wger.de/media/exercise-images/" + relative
        with urlopen(url, timeout=30) as response:
            data = response.read()
        if sha256(data).hexdigest() != expected:
            raise ValueError(f"Source changed; review again before including: {url}")
        (originals / (name + ".png")).write_bytes(data)
        image = Image.open(BytesIO(data)).convert("RGBA")
        image.thumbnail((512, 512), Image.Resampling.LANCZOS)
        target = output / (name + ".webp")
        image.save(target, "WEBP", lossless=True, method=6, exact=True)
        print(name, image.size, target.stat().st_size, sha256(target.read_bytes()).hexdigest())
