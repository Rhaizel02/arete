"""Optional developer-only reproduction of reviewed CC BY-SA 3.0/4.0 assets.

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
    ("back_squat_standing", "https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/barbell-squat-1.png", "669f2bd5bb79756823498f6f32286b07396fa8edb21eed95f6d50749d2beb4ea"),
    ("back_squat_lowered", "https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/barbell-squat-2.png", "0bb9de91a1955530d1b565fba246a1d05efc760d138cbe738ef47db24df3716f"),
    ("leg_press_extended", "https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/leg-press-1.png", "efa6c677ab45d6307df7c5620fd45a344b0edca3da1bba775123b51dc161910d"),
    ("leg_press_bent", "https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/leg-press-2.png", "eeb96924bf18fc341ee81356a8755df2a14cb00565e13cb9e2e294e38a7d6e32"),
    ("romanian_deadlift_standing", "https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/romanian-dead-lift-1.png", "86ef8f13930701a2afc107e6de7202ffe1ac569d7d25fdb4fdcc7f13862af16b"),
    ("romanian_deadlift_lowered", "https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/romanian-dead-lift-2.png", "f512afff96b15fd252884c2f823abde36c79e1d7bdf1af4e4bffc9166ff7651b"),
    ("lying_leg_curl_extended", "https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/lying-leg-curl-machine-1.png", "43bc68c6d74e0b3075a842626eb16391eb63bde185319d9fa4c50860edf1793d"),
    ("lying_leg_curl_bent", "https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/lying-leg-curl-machine-2.png", "8b44113681a75e48b273d13fd7b8fd6bd9e893d195547545dc1c1fcd8cb4e7b3"),
    ("seated_leg_curl_bent", "https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/seated-leg-curl-1.png", "b43ef327ac9061185262980c00bf909954f98625fa4ee8ef850fd722a03e301b"),
    ("seated_leg_curl_extended", "https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/seated-leg-curl-2.png", "44758a5cc2e075238b1caf6a1c60afa300799f8f7497a9534726c923b7082a5c"),
    ("hammer_curl_lowered", "86/Bicep-hammer-curl-1.png", "f9809143f121283705612669b9d98cd25432be0c9018fe1f79ed924bafec7801"),
    ("hammer_curl_raised", "86/Bicep-hammer-curl-2.png", "c5591af9a40419516da525aa108997807881e9c0ea4293b4b1f432c7b9862452"),
]

if __name__ == "__main__":
    output = ROOT / "app/src/main/res/drawable-nodpi"
    originals = ROOT / "app/build/catalog-research"
    output.mkdir(parents=True, exist_ok=True)
    originals.mkdir(parents=True, exist_ok=True)
    for name, relative, expected in ASSETS:
        url = relative if relative.startswith("https://") else "https://wger.de/media/exercise-images/" + relative
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
