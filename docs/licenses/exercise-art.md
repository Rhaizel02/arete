# Créditos y licencia de las ilustraciones de ejercicios

**Autor: Everkinetic. Distribuidor consultado: wger. Licencia: [Creative Commons Attribution-ShareAlike 3.0 Unported](https://creativecommons.org/licenses/by-sa/3.0/), [texto legal](https://creativecommons.org/licenses/by-sa/3.0/legalcode).**

Se conservan título, procedencia y licencia por cada archivo. Las adaptaciones distribuidas en Arete continúan bajo **CC BY-SA 3.0**, sin restricciones adicionales ni aval del autor o wger. El código de Arete y su contenido editorial original no se presentan como propiedad de wger/Everkinetic. Los derechos de terceros no se sustituyen por la licencia del código.

Verificación de metadatos el 23-10-2026: [API de licencias wger](https://wger.de/api/v2/license/), ID 1 = CC-BY-SA 3. Los registros [68](https://wger.de/api/v2/exerciseimage/68/), [70](https://wger.de/api/v2/exerciseimage/70/), [105](https://wger.de/api/v2/exerciseimage/105/), [106](https://wger.de/api/v2/exerciseimage/106/), [177](https://wger.de/api/v2/exerciseimage/177/), [178](https://wger.de/api/v2/exerciseimage/178/) identifican `license_author: Everkinetic`, licencia 1. Se comprobaron las variantes con exerciseinfo 73 (banca con barra), 537 (inclinado con mancuernas) y 348 (elevaciones con mancuernas), además de revisar visualmente las seis posiciones.

| WebP local (en `app/src/main/res/drawable-nodpi/`) | Título / original | Cambios | Bytes |
| --- | --- | --- | ---: |
| bench_press_extended.webp | [Bench press 1](https://wger.de/media/exercise-images/192/Bench-press-1.png) | 800 × 831 → 493 × 512, WebP sin pérdida | 34000 |
| bench_press_lowered.webp | [Bench press 2](https://wger.de/media/exercise-images/192/Bench-press-2.png) | 800 × 831 → 493 × 512, WebP sin pérdida | 32470 |
| incline_dumbbell_lowered.webp | [Incline press 1](https://wger.de/media/exercise-images/16/Incline-press-1.png) | 298 × 450, conversión WebP sin pérdida | 16378 |
| incline_dumbbell_extended.webp | [Incline press 2](https://wger.de/media/exercise-images/16/Incline-press-2.png) | 298 × 450, conversión WebP sin pérdida | 17502 |
| lateral_raise_lowered.webp | [Lateral dumbbell raises 2](https://wger.de/media/exercise-images/148/lateral-dumbbell-raises-large-2.png) | 863 × 960 → 460 × 512, WebP sin pérdida | 36188 |
| lateral_raise_raised.webp | [Lateral dumbbell raises 1](https://wger.de/media/exercise-images/148/lateral-dumbbell-raises-large-1.png) | 863 × 960 → 460 × 512, WebP sin pérdida | 46014 |

Total **182552 bytes**. La conversión conserva proporciones y estilo; sin recorte, recoloración, eliminación de fondo ni transformación generativa. No se incluyen originales duplicados ni variantes por densidad. La ficha muestra créditos y enlaces originales/licencia, también accesibles desde el entrenamiento.

`scripts/prepare_catalog_art.py` reproduce las seis descargas de desarrollo y la conversión convencional con Pillow. Verifica SHA-256 de los originales antes de escribir recursos. **No forma parte de la app ni se ejecuta con Gradle.** Los PNG descargados quedan en `app/build/catalog-research/` (ignorado); una diferencia de hash requiere revisión explícita del archivo y licencia, no se adopta automáticamente.

SHA-256 de los WebP incorporados:

```text
bench_press_extended     5867187faa7b53eaa55585dad0a471fbf8e39e0075719ab431d18f830dc7d04a
bench_press_lowered      13ac06b4265f650ee9d88158c76a5f482b32f21cd449bfd3d8c3b352915f46c0
incline_dumbbell_lowered 4a77a8ebd34c11acff14ddd482a5dd6ea1726afc7fd95ce7a1268430011935e6
incline_dumbbell_extended dbee8df4691e378e8664d1891c5b6d48b6b40bebde75b9b2785fcb2debb793dc
lateral_raise_lowered    e7cc74319444d8c26891f18bf5d6d384f211b2b2bd7203cd1837e9968b12e5a0
lateral_raise_raised     016faa0437d3d8cfde0aad34498f79e2327e28e95dbc8b8515411fa90809e49c
```

RepDB se evaluó pero no se incorporó. Su permiso de uso in-app no se interpreta como autorización implícita para publicar sus assets en el código fuente público. [Decisión y matching](../block-7-catalog.md).
