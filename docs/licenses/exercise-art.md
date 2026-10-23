# Créditos y licencia de las ilustraciones de ejercicios

Las licencias se indican por archivo. Los recursos del Bloque 7 no cambian de licencia; las nuevas incorporaciones se detallan más abajo.

## Recursos del Bloque 7 (seis archivos)

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

## Recursos del Bloque 8 (doce archivos)

Revisión: 24-10-2026. Diez originales proceden directamente de **Everkinetic/data**, revisión fija `446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6`. La [licencia raíz de esa revisión](https://github.com/everkinetic/data/blob/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/LICENSE.md) declara **CC BY-SA 4.0** para el repositorio, sin excepciones para los archivos seleccionados de `src/images-web/`. El [README](https://github.com/everkinetic/data/blob/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/README.md) acredita el proyecto original de Greg Priday. Atribución distribuida: «Everkinetic · proyecto original de Greg Priday». Las adaptaciones de estos diez archivos se distribuyen bajo [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/), [texto legal](https://creativecommons.org/licenses/by-sa/4.0/legalcode).

Los dos originales de curl martillo proceden de **wger**, registros individuales [47](https://wger.de/api/v2/exerciseimage/47/) y [48](https://wger.de/api/v2/exerciseimage/48/), autor Everkinetic y licencia 1 (**CC BY-SA 3.0**). Atribución: «Everkinetic · vía wger». Esos dos derivados conservan CC BY-SA 3.0; no se aplica automáticamente la licencia del otro repositorio.

Todos los cambios son escalado proporcional (máximo 512 px, LANCZOS) y conversión WebP sin pérdida. No hay recorte, recoloración, eliminación de fondo ni transformación generativa. Se conservan estilo y proporciones; sin aval del autor o distribuidor. No se empaquetan PNG duplicados. Cada ficha y el manifest conservan URL individual, autor, licencia, cambios y descripción accesible bilingüe.

| WebP local | Original individual | Original → WebP (px) | Licencia | Bytes |
| --- | --- | --- | --- | ---: |
| back_squat_standing.webp | [Barbell squat 1](https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/barbell-squat-1.png) | 798×960 → 426×512 | 4.0 | 53564 |
| back_squat_lowered.webp | [Barbell squat 2](https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/barbell-squat-2.png) | 798×960 → 426×512 | 4.0 | 40856 |
| leg_press_extended.webp | [Leg press 1](https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/leg-press-1.png) | 960×630 → 512×336 | 4.0 | 45576 |
| leg_press_bent.webp | [Leg press 2](https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/leg-press-2.png) | 960×630 → 512×336 | 4.0 | 46196 |
| romanian_deadlift_standing.webp | [Romanian dead lift 1](https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/romanian-dead-lift-1.png) | 659×960 → 351×512 | 4.0 | 44892 |
| romanian_deadlift_lowered.webp | [Romanian dead lift 2](https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/romanian-dead-lift-2.png) | 659×960 → 351×512 | 4.0 | 29956 |
| lying_leg_curl_extended.webp | [Lying leg curl machine 1](https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/lying-leg-curl-machine-1.png) | 960×547 → 512×292 | 4.0 | 39504 |
| lying_leg_curl_bent.webp | [Lying leg curl machine 2](https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/lying-leg-curl-machine-2.png) | 960×547 → 512×292 | 4.0 | 32868 |
| seated_leg_curl_extended.webp | [Seated leg curl 2](https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/seated-leg-curl-2.png) | 640×960 → 341×512 | 4.0 | 46344 |
| seated_leg_curl_bent.webp | [Seated leg curl 1](https://raw.githubusercontent.com/everkinetic/data/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/src/images-web/seated-leg-curl-1.png) | 640×960 → 341×512 | 4.0 | 48232 |
| hammer_curl_lowered.webp | [Bicep hammer curl 1](https://wger.de/media/exercise-images/86/Bicep-hammer-curl-1.png) | 800×1779 → 230×512 | 3.0 | 24896 |
| hammer_curl_raised.webp | [Bicep hammer curl 2](https://wger.de/media/exercise-images/86/Bicep-hammer-curl-2.png) | 800×1779 → 230×512 | 3.0 | 21582 |

SHA-256 de cada WebP nuevo:

```text
back_squat_standing 254e5dbf2f117310a935e715e0333231b6b690bd07408f5df6651958a089a5d6
back_squat_lowered 37a7c17bfe3b47dbd82aaae96eaafbfcda571f0c3b20df6bd502470321a362b3
leg_press_extended 497d515c878f28769882457ce4db9f809a13ab0d14ed22ea7bc24e193ebf6091
leg_press_bent cc15d6138f7ab51034e750598df4af12b70ce05b21d5719e0592f90b62abcdd6
romanian_deadlift_standing e825234c5ab7182a1cbf15bd2fb58f057cacf98ad1d96f6772dc15e48e54d3f9
romanian_deadlift_lowered 9508d61ea7d263e86f2f30537442aa6cf7f7d743dcf4cefbad20e10c698c3278
lying_leg_curl_extended c90c4bd8511d3497f952da02baa6eda0a57fdf4f5c5feec015cc0c8579f6a818
lying_leg_curl_bent eeaf2c7af4f20ace57c976350a8c38f031865f69de6e8bffb667a9f3299f47e7
seated_leg_curl_extended a3ad9bc3bac78899109f7e011e1252a8a70816cc867337ac2c726a8e856ad891
seated_leg_curl_bent 323477423c808b7c81499f8e56fb01fdc3d194dab07548d1a70cca56aec19025
hammer_curl_lowered a7627f6dc3eb74b578b8b366e179603c27793d856d735d135fa27baaf7743a8f
hammer_curl_raised f20cde9d0bffab4860c8286a51ebb130b77885a1feff29fa3cc5f2e061ea5510
```

Incremento de recursos: **474466 bytes**, total de los dieciocho WebP: **657018 bytes**. El script de reproducción ahora contempla los dieciocho archivos y fija también los hashes de los PNG originales. [Cobertura, matching y descartes del Bloque 8](../block-8-catalog.md). La imagen de prensa se identifica como ejemplo de prensa inclinada; no representa todas las máquinas posibles. Los recursos rechazados no se distribuyen.
