# Rediseño UI — Bloque A

Validación del 22 de octubre de 2026. Implementación nativa Jetpack Compose / Material 3, sobre el Bloque 4 existente.

## Alcance y referencias

Inicio, sesión y resumen de Progreso reciben el rediseño estructural. Rutinas, catálogo, historial, actividad, editores y ajustes conservan su estructura funcional con el tema global. La navegación principal es Inicio / Rutinas / Progreso / Más. Más abre Ejercicios, Historial, Actividad y Ajustes; las pantallas secundarias conservan la navegación atrás.

Las referencias entregadas se conservan en [stitch](../stitch/):

| Adjunto original | Copia en el repositorio |
| --- | --- |
| DESIGN.md | `stitch/DESIGN.md` |
| screen (3).png | `stitch/home-dark.png` |
| screen (5).png | `stitch/workout-dark.png` |
| screen (7).png | `stitch/progress-dark.png` |
| code.html | `stitch/reference-html/home.html` |
| code (4).html | `stitch/reference-html/workout.html` |
| code (6).html | `stitch/reference-html/progress.html` |
| screen.png, screen (1).png, screen (2).png | `stitch/secondary/` |

Los HTML son referencias visuales, no código de ejecución. El [backlog](../../product/STITCH_BACKLOG.md) delimita este bloque según el prompt aprobado; no representa requisitos recuperados de un backlog anterior.

Space Grotesk y Hanken Grotesk son recursos locales en `app/src/main/res/font/`. Proceden de los directorios `ofl/spacegrotesk` y `ofl/hankengrotesk` del repositorio oficial [google/fonts](https://github.com/google/fonts/tree/main/ofl). Sus licencias OFL están en [fonts](../fonts/). No se cargan fuentes, fotografías ni perfiles por red. Títulos/métricas usan Space Grotesk, texto/inputs Hanken Grotesk, con cifras tabulares.

## Comparación con Stitch y adaptaciones justificadas

Se revisaron las capturas reales contra las tres referencias principales. Se corrigieron pesos tipográficos de las fuentes variables, contraste del indicador activo de navegación, superficies heredadas del tema, alineación de las tarjetas semanales y color azul de las gráficas.

| Pantalla | Identidad conservada | Diferencias deliberadas |
| --- | --- | --- |
| Inicio | Jerarquía de próxima sesión, CTA azul, dos tarjetas semanales, PR y planificación posterior | Sin nombre/avatar ficticio ni fotografías. Racha en semanas cumpliendo, nunca días consecutivos. PR real con fecha y sin incremento inventado. Entrenamiento libre secundario cuando puede iniciarse. |
| Sesión | Un ejercicio destacado, última ejecución, tabla alineada, completado azul, descanso y navegación inferior | Progreso por series de trabajo, no por ejercicios. Menú por serie para objetivo, calentamiento, orden y borrado. RIR/fallo conserva su selector. Descanso con ±15 s y salto reales, sin pausa/reinicio decorativos. |
| Progreso | Métricas en tarjetas, fuerza, distribución muscular, PR y peso | Entrenamientos, series efectivas, sesiones/semana y duración fiable, no tonelaje ni comparaciones entre ejercicios. Distribución por recuentos reales de series efectivas. Selector de ejercicio funcional, sin pestañas/filtros temporales simulados. |

El azul de acción es `#2563EB`. En oscuro se usa azul claro para textos sobre superficies oscuras, manteniendo azul saturado con blanco para la acción principal. Las gráficas son estáticas, incluyen rango/unidad, fechas, último valor y una descripción accesible. Cero registros produce un estado informativo; un único registro se representa como un punto, sin simular una tendencia.

Se priorizan controles Android de al menos 48 dp, scroll y adaptación de texto sobre reproducir la densidad exacta del HTML. La guía es una hoja inferior desplazable con instrucciones guardadas y enlace HTTP(S) mediante Android. No se han añadido botones de notificación, perfiles o controles sin comportamiento.

## Persistencia y excepción autorizada

No cambian entidades, DAO, schema, migraciones, JSON de rutinas/backup ni algoritmos de progreso. La guía añade una lectura por ID que también encuentra ejercicios archivados. La selección de ejercicio es estado de presentación por ID, conservada durante navegación y rotación; completar una serie no provoca avance automático. Si se borra el ejercicio seleccionado, se escoge su vecino.

El repositorio original no exponía reordenación de series. El usuario autorizó expresamente ampliar esa parte durante la implementación: `SessionRepository.moveSet` reescribe únicamente posiciones en una transacción, usando las operaciones DAO existentes y sin alterar identidad, valores, calentamiento, completado o timestamps. Es la única ampliación del motor. La lectura directa de la preferencia guardada al completar una serie evita depender de que Ajustes esté observando el temporizador.

## Verificación automatizada

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --no-daemon
.\gradlew.bat assembleDebugAndroidTest connectedDebugAndroidTest --no-daemon
```

- 32 pruebas unitarias: persistencia/archivados y guía, sesiones, reordenación de series, cálculos y repositorios de progreso, JSON, backup, planificación, recordatorios y presentación de sesión.
- Presentación: progreso excluyendo calentamientos, cero series, primer ejercicio pendiente, selección estable tras completado/reordenación y vecino tras borrado.
- Reordenación: mantiene valores y completado, posiciones contiguas, límites y ID inexistente.
- Instrumentación API 35: migración y notificaciones; el tercer caso es el fixture, que retorna sin modificar datos salvo activación explícita.
- `lintDebug`: 0 errores, 18 avisos de versiones de herramientas/dependencias y 1 recomendación de estado primitivo preexistente en el editor de peso. No se actualizan dependencias como parte del rediseño.

## Comprobación manual en AVD

AVD `Arete_API_35`, Android 15/API 35, 1080 × 2400, orientación vertical y horizontal. Datos de prueba exclusivos del emulador.

- Sesión vacía: 0 / 0, 0 %, acción de añadir; sesión poblada: selección, anterior/siguiente, resumen de grupo y acceso directo.
- Entrada decimal, teclado numérico y avance de foco; CTA inferior accesible con teclado. Valor decimal y completado sobreviven al cierre/reapertura del proceso; notas guardadas verificadas al navegar.
- Selector RIR y fallo; calentamiento marcado como C y excluido del total; completar/desmarcar sin avance automático.
- Bloqueo de sustitución con series completadas y alternativa que abre «Añadir al entrenamiento».
- Guía técnica, instrucciones y enlace externo; lookup de archivados cubierto además por Room en memoria.
- Temporizador al completar una serie, ajuste/salto y preferencia desactivada; notas con autosave.
- Rotación manteniendo el ejercicio seleccionado, textos largos, fuente al 130 %, scroll y contraste en claro/oscuro.
- Fuerza sin sesiones completadas y con una sola sesión: estado vacío y un único punto con fecha/unidad, respectivamente.
- Menú de orden de series: una serie completada baja a la segunda fila y vuelve a la primera sin perder su estado.
- Más, sus cuatro destinos, navegación atrás y accesos conservados a importación/exportación y backup/restore.

## Capturas reales

Capturadas con `adb shell screencap`, no renderizados ni mockups. Los valores proceden de Room en el AVD, no de constantes en pantallas de producción.

| Pantalla | Oscuro | Claro |
| --- | --- | --- |
| Inicio | [home-dark.png](home-dark.png) | [home-light.png](home-light.png) |
| Sesión | [workout-dark.png](workout-dark.png) | [workout-light.png](workout-light.png) |
| Progreso | [progress-dark.png](progress-dark.png) | [progress-light.png](progress-light.png) |

### Reproducir datos y capturas

Usar únicamente un emulador/dispositivo de pruebas: el fixture reemplaza su base de datos y preferencias. Está en `androidTest`, no en el APK de producción. No se ejecuta automáticamente al abrir la app, no añade un modo demo y no modifica contratos de importación.

```powershell
$adb = 'C:\Users\bobby\Android\AreteSdk\platform-tools\adb.exe'
& $adb install -r app/build/outputs/apk/debug/app-debug.apk
& $adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adb shell am force-stop com.arete.app
& $adb shell am instrument -w -e class com.arete.app.data.DesignFixtureTest -e designFixture true -e theme DARK com.arete.app.test/androidx.test.runner.AndroidJUnitRunner
& $adb shell am start -W -n com.arete.app/.MainActivity
```

Cambiar `DARK` por `LIGHT` para el otro tema. `-e historySessions 0` y `-e historySessions 1` permiten revisar cero y un único punto de fuerza; por defecto se generan siete sesiones completadas y una activa. Tras navegar y esperar a que cargue el destino:

```powershell
& $adb shell screencap -p /sdcard/design.png
& $adb pull /sdcard/design.png docs/design/validation-block-a/home-dark.png
```

## Fuera de este bloque

Se mantiene para el Bloque B el rediseño estructural de las pantallas restantes y lo expresamente excluido: filtros avanzados y gráficas interactivas. No se han añadido servicios, perfiles, métricas simuladas o acciones externas adicionales.
