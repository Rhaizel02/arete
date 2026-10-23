# Validación del Bloque 7 — Catálogo 2.0

23 de octubre de 2026. Base: `ad8c7d54a991f61db130ed38310ee3b1017d5eb4`.

[Formato, fuentes, matching y cobertura de los 45 ejercicios](../../block-7-catalog.md). [Créditos, licencia y hashes de las seis imágenes](../../licenses/exercise-art.md).

## Pruebas y build

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --no-daemon
```

Se conservan las 52 pruebas anteriores y se añaden ocho pruebas selectivas (60 en total): integridad del manifest y IDs del catálogo, filtros AND ES/EN/acentos/personalizados, datos personalizados intactos y fallbacks, idioma alternativo, imágenes locales decodificables y créditos, referencias/IDs inválidos, enlaces HTTP(S) y lectura real por ID incluyendo archivados mediante el repositorio existente. No se añade una suite UI extensiva.

Resultado final: **60 pruebas superadas, 0 fallos; assembleDebug correcto; lintDebug con 0 errores, 30 avisos y una recomendación**. Los avisos corresponden a versiones de herramientas/dependencias, iconos ya aprobados y sugerencias KTX; la recomendación de estado boxed existente no se aborda fuera de alcance. Sin supresiones ni actualizaciones de dependencias. También se compiló `assembleDebugAndroidTest` para el fixture opt-in.

Las pruebas de regresión existentes siguen cubriendo sesiones/snapshots, progreso, Routine JSON, Backup JSON y copias automáticas. Se comprueba además que no cambia el diff de schema/entidades/DAO, `exercises_catalog.json`, motor ni servicios JSON. El manifest y las imágenes no son tablas ni se añaden a los contratos de backup.

## Tamaño real del APK

Misma variante debug sin minificar, misma máquina/toolchain:

| Medida | Bytes |
| --- | ---: |
| APK del Bloque 6 presente antes de empezar | 21638252 |
| APK final del Bloque 7 | 22677580 |
| Incremento total (código, recursos y empaquetado) | **1039328** |
| Seis imágenes WebP, únicas | **182552** |
| Manifest editorial sin comprimir / comprimido en ZIP | 16776 / 5586 |

El incremento total es **0,991 MiB**, inferior al objetivo orientativo de 5 MB. El APK final ocupa 21,627 MiB. No se atribuye todo el incremento a las imágenes: incluye nuevas clases, serialización y UI de la variante debug. Se inspeccionó el ZIP para comprobar exactamente seis WebP `drawable-nodpi-v4` y ausencia de duplicados por densidad. No es una medición de un AAB/release minificado ni del tamaño instalado.

## Emulador y fixture

AVD desechable `Arete_API_35`, Android 15/API 35, 1080 × 2400. Se utilizó un fixture **opt-in de androidTest**, no un modo demo ni un endpoint de producción: 45 ejercicios reales del catálogo más un personalizado de prueba, una sesión libre activa con banca y jalón, seis series pendientes. La configuración de backup se desactiva antes de sembrar el fixture; no se borran copias externas. No se utilizan datos personales.

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# ATENCIÓN: seed reemplaza los datos fitness del dispositivo. Solo AVD desechable.
adb shell am instrument -w -e class com.arete.app.data.CatalogFixtureTest -e catalogFixture seed com.arete.app.test/androidx.test.runner.AndroidJUnitRunner
adb shell cmd connectivity airplane-mode enable
```

Los modos `catalogFixture light` y `dark` solo cambian tema y comprueban los recuentos del fixture, resolución editorial y conservación del personalizado. `check` realiza las mismas comprobaciones sin cambiar tema ni resembrar. Las comprobaciones esperan la sesión original de dos ejercicios, por lo que se elimina explícitamente cualquier añadido de prueba antes de ejecutarlas otra vez.

## Flujos reales comprobados

- Búsqueda por nombre inglés (`press`, `bench`, `home`, `lateral`) y filtros combinados de músculo/equipo; por ejemplo `press` + Pecho + Mancuernas deja únicamente el inclinado. Limpiar filtros devuelve los 46 ejercicios del fixture.
- Equipo personalizado `Banda casera` y músculo Espalda junto con `home`: aparece solo el ejercicio personalizado. Su ficha conserva exactamente la instrucción original y ofrece edición; no recibe contenido editorial por parecido del nombre.
- Estado vacío con `bench` + Bíceps y acción Mostrar todos operativa.
- Ficha básica de aperturas sin imagen, ficha ampliada sin imagen para jalón y las tres secuencias incorporadas (banca, inclinado y elevaciones). No se muestran ilustraciones de variantes distintas para completar cobertura.
- Cambio ES/EN, scroll de instrucciones, fuentes diferenciadas (ACSM general / técnica práctica), créditos Everkinetic/wger, originales y licencia visibles en la ficha.
- Desde sesión: abrir/cerrar guía de banca con imágenes y jalón sin imagen; regresar conserva selección, series y progreso. Las imágenes no aparecen permanentemente en los campos de registro.
- Selector de sesión: Hombros + Mancuernas + `lateral`, abrir su ficha sin añadir, cerrar y añadir una vez. La sesión pasa de 6 a 9 series pendientes; eliminar ese ejercicio con confirmación vuelve al estado original de 6. No se completa, sustituye ni altera ninguna serie anterior para consultar contenido.
- Modo avión real activado antes de consultar catálogo, filtrar y abrir fichas. JSON e imágenes locales siguen disponibles. Los enlaces bibliográficos son externos: no se afirma que sus páginas se puedan leer sin red.
- Teclado de búsqueda, scroll, navegación atrás, claro/oscuro y fuente al 140 %. En horizontal se detectó poco espacio útil con filtros fijos: se integraron en el scroll, manteniendo la búsqueda visible. Se revisó la recuperación tras rotación de filtros y ficha.

## Capturas reales

PNG de `adb shell screencap -p`, sin mockups, retoque ni imágenes generadas. El icono de avión documenta el estado sin conexión.

- Catálogo: [oscuro](catalog-dark.png), [claro](catalog-light.png), [teclado](search-keyboard-light.png).
- Filtros: [combinados oscuro](filters-combined-dark.png), [claro](filters-combined-light.png), [vacío y recuperación](filters-empty-dark.png), [personalizado](custom-filters-dark.png).
- Fichas: [inclinado oscuro](guide-incline-dark.png), [claro](guide-incline-light.png), [inglés](guide-english-dark.png), [fuentes/créditos](guide-credits-dark.png), [básica sin imagen](guide-basic-dark.png), [personalizado](guide-custom-dark.png).
- Sesión: [tabla sin imágenes permanentes](session-dark.png), [guía contextual ilustrada](session-guide-dark.png), [guía sin imagen](session-guide-no-image-dark.png), [filtros del selector](picker-filters-dark.png), [previsualización de elevaciones](picker-guide-dark.png).
- Accesibilidad: [ficha horizontal, fuente 140 %](guide-landscape-large-light.png), [filtros/lista horizontal](filters-landscape-large-light.png), [fuente ampliada en vertical](filters-large-light.png).

## Límites reales

- Cobertura técnica ampliada: **10/45**, ES y EN. Multimedia: **3/10 priorizados, 3/45 del catálogo**, seis posiciones estáticas. Los otros siete prioritarios y los 35 básicos permanecen sin imágenes; no se presenta el bloque como cobertura multimedia completa.
- RepDB no incorporado por la ambigüedad de redistribución de assets en código fuente público; requiere aclaración expresa antes de usarse aquí. No se usan premium-samples ni material de pago.
- No se valida técnica individual ni se promete prevenir lesiones. ACSM 2026 no se cita como aval de cada ilustración/cue.
- La app principal sigue en español; el selector ES/EN cambia el contenido técnico ampliado, no toda la interfaz ni los textos personales. Fallback a ES y después instrucciones existentes, sin traducciones inventadas de personalizados.
- No se ha ejecutado un estudio con TalkBack ni en dispositivos físicos; sí se han revisado descripciones accesibles, objetivos táctiles, scroll, fuente ampliada y ambos temas en API 35.
- P2 anteriores de temporizador y precarga sin cambios. No se añade hardening, sincronización, APIs, cuentas o nuevas funciones de entrenamiento.
