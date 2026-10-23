# Validación del Bloque 8 — Catálogo 2.0

24 de octubre de 2026. Base: `24c4f927bdb8097ae194576ee9cd8ab4869b4c2f`.

[Cobertura de los 45 IDs, fuentes y decisiones gráficas](../../block-8-catalog.md). [Créditos individuales, licencias, transformaciones y SHA-256](../../licenses/exercise-art.md).

## Pruebas y compatibilidad

```powershell
.\gradlew.bat clean testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --no-daemon
```

Se mantienen las regresiones anteriores de sesiones/snapshots, progreso, Routine JSON, Backup JSON y backup automático. Se actualizan los tests editoriales para exigir **45/45 IDs estables**, ES/EN con todas sus secciones, fuentes prácticas trazables, **18 recursos decodificables**, descripciones y créditos, resolución local y fallbacks. Una prueba pequeña adicional comprueba todas las secciones de todos los idiomas; no se añade una suite Compose extensa.

Resultado de la ejecución limpia final: **61 pruebas, 0 fallos, 0 errores; assembleDebug y assembleDebugAndroidTest correctos; lintDebug con 0 errores, 30 avisos y una recomendación**. Los avisos siguen siendo versiones de herramientas/dependencias, iconos aprobados y sugerencias KTX; la recomendación de estado boxed es anterior. No se actualizan dependencias ni se suprimen avisos fuera de alcance. Se reinstala el APK limpio y se vuelve a ejecutar `catalogFixture check`: **OK (1 test)**, conservando el fixture anterior.

La comparación contra el commit base confirma que las diez guías previas conservan exactamente `text` y `sourceIds`. Sin cambios en `exercises_catalog.json`, schema/entidades/DAO Room, contratos JSON, repositorios, motor o manifest Android. No se añaden permisos ni dependencias. La lectura por ID de ejercicios archivados y la preservación del contenido personalizado siguen cubiertas por pruebas.

## Tamaño comparable del APK

Misma máquina, toolchain y variante debug sin minificar. Se reconstruye el commit base en un directorio aislado extraído con `git archive`, sin resultados anteriores, y se compila el estado final con `clean`. No se compara un APK limpio con otro incremental.

| Medida | Bytes |
| --- | ---: |
| Bloque 7 reconstruido desde `24c4f92` | 21387125 |
| Bloque 8 final limpio | **21895536** |
| Incremento comparable | **508411** |
| Doce WebP nuevos | 474466 |
| Dieciocho WebP totales | 657018 |
| Manifest editorial final, sin comprimir / comprimido ZIP | 67252 / 19636 |

Final: **20,881 MiB**, incremento **0,485 MiB (+2,38 %)**. El ZIP contiene exactamente dieciocho WebP, sin duplicados por densidad. No es una medición de AAB/release ni del tamaño instalado. El APK incremental conservado del cierre del Bloque 7 medía 22677580 bytes y una primera compilación incremental de este bloque 22160198: se registran para explicar la diferencia con la documentación histórica, pero no se usan para atribuir una reducción ficticia al contenido nuevo.

## AVD y datos aislados

AVD desechable `Arete_API_35`, Android 15/API 35, 1080×2400. Se instala la actualización con `adb install -r` sobre el fixture aislado del Bloque 7, **sin resembrar ni borrar sus datos**: 45 ejercicios del catálogo, un personalizado (`qa-custom-band`) y una sesión libre con banca/jalón, dos ejercicios y seis series pendientes.

El fixture opt-in de `androidTest` comprueba ahora 45 guías y nueve ejercicios ilustrados. Los modos `check`, `light` y `dark` pasan en el dispositivo; los dos últimos únicamente cambian el tema y ejecutan las mismas comprobaciones. Se comprueban tanto el contenido personalizado exacto como los recuentos de la sesión después de consultar fichas y cambiar de destino. No se utiliza el modo destructivo `seed` en este bloque ni se incorpora un modo demo de producción.

```powershell
adb shell am instrument -w -e class com.arete.app.data.CatalogFixtureTest -e catalogFixture check com.arete.app.test/androidx.test.runner.AndroidJUnitRunner
adb shell cmd connectivity airplane-mode enable
```

## Flujos comprobados

- Muestra de los seis grupos: flexiones (pecho), jalón con brazos rectos (espalda), rumano/prensa (piernas), face pull (hombros), martillo (brazos), plancha (core). Se revisan guías nuevas y anteriores, con/sin ilustración, y cambio ES/EN.
- Modo avión durante toda la consulta de catálogo, búsqueda, filtros y fichas. Instrucciones e imágenes permanecen locales; los sitios externos de bibliografía requieren conexión y no se presentan como disponibles offline.
- Preparación, pasos e indicaciones aparecen primero; imágenes y referencias se despliegan por separado. Las imágenes abiertas incluyen atribución corta y licencia; los créditos completos conservan originales, cambios y enlaces legales. Se revisan CC BY-SA 3.0 y 4.0.
- Búsqueda inglesa y teclado; `hammer` + Bíceps + Mancuernas reduce el catálogo a martillo. Salir a Inicio/sesión y abrir Añadir ejercicio muestra **46 ejercicios, búsqueda vacía y ambos filtros sin selección**. Una búsqueda `face` en el selector permite previsualizar sin añadir. No se heredan criterios entre destinos.
- Abrir guía técnica de banca desde la sesión muestra texto primero. Cerrar la guía/selector y regresar conserva dos ejercicios, seis series pendientes y progreso 0/6. No se completa, sustituye ni elimina ninguna serie para consultar contenido.
- Claro/oscuro; fuente al **140 %**, horizontal/vertical, scroll de pasos e imágenes. La ficha y el filtro `leg press` sobreviven a la rotación. Los filtros permanecen en el scroll de lista del Bloque 7, con acceso al resultado en horizontal. Se restablecen fuente y orientación al finalizar.
- La ficha personalizada conserva literalmente «Mi instrucción personal: ancla la banda, fija el tronco y tira con control.», su edición y fallback sin guía editorial asociada. No se asigna una ilustración por similitud de nombre.

## Capturas reales

PNG obtenidos con `adb shell screencap -p`, sin mockups ni retoques. El icono de avión refleja el estado real del AVD.

- Catálogo [oscuro](catalog-dark.png), [claro](catalog-light.png), [filtros combinados](catalog-combined-filters-dark.png).
- Pecho sin imagen: [flexiones](chest-pushup-dark.png). Espalda sin imagen: [jalón con brazos rectos](back-straight-pulldown-dark.png). Hombros sin imagen: [face pull](shoulders-face-pull-dark.png).
- Piernas: [rumano texto primero](legs-rdl-text-dark.png), [dos poses](legs-rdl-art-dark.png), [fuentes/créditos](legs-rdl-credits-dark.png), [prensa inclinada en claro](legs-press-art-light.png).
- Brazos: [martillo con imágenes y licencia 3.0](arms-hammer-art-dark.png). Core: [plancha en inglés](core-plank-english-dark.png).
- Sesión: [guía textual contextual](session-guide-text-dark.png), [selector sin filtros heredados](picker-isolated-dark.png), [previsualización](picker-preview-dark.png), [sesión conservada](session-preserved-dark.png).
- Accesibilidad: [catálogo horizontal al 140 %](catalog-landscape-large-light.png), [cabecera de ficha](guide-landscape-large-light.png), [pasos mediante scroll](guide-landscape-steps-large-light.png), [ficha vertical ampliada](guide-large-light.png), [filtro conservado tras rotar](catalog-restored-large-light.png).
- [Personalizado conservado](custom-preserved-light.png).

## Límites y prioridades

- **P0/P1:** no detectados en las pruebas y flujos de este bloque; no equivale a una auditoría exhaustiva del producto.
- **P2 de contenido:** 36 ejercicios aún sin imagen. Entre los siete prioritarios, dominadas, jalón al pecho, remo con barra y militar de pie conservan fallback por ambigüedad/variante incorrecta de los candidatos. La investigación y los descartes están documentados; no se fuerza cobertura artificial.
- Continúan los P2 anteriores de temporizador y precarga, fuera del alcance de este bloque. No se añade hardening ni funcionalidad de entrenamiento.
- No se valida técnica individual ni se promete prevenir lesiones. Las referencias prácticas no son evidencia clínica; ACSM general no avala cada dibujo.
- La app general sigue en español; ES/EN corresponde al contenido editorial, no traduce datos personales ni toda la interfaz.
- Sin prueba con TalkBack ni dispositivo físico. Se revisan semántica de imágenes, objetivos táctiles, scroll, ambos temas, fuente ampliada y rotación en API 35. No se simulan enlaces externos navegables en modo avión.
