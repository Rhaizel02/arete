# Bloque 5 — Progreso 2.0 e icono aprobado

Implementado sobre `305a03e39ee510a8e293d967300b6d5f8d24da64`. Validación del 23 de octubre de 2026, APK real en `Arete_API_35` (Android 15, 1080 × 2400). Se conserva el sistema visual A+B y no se añaden dependencias.

## Semántica de períodos

- Selector compartido: **1 mes / 3 meses / 6 meses / Todo**, conservado al navegar y durante rotación dentro de la actividad.
- Inicio inclusivo a medianoche local del mismo día de hace N meses naturales, ajustado al último día válido del mes; fin inclusivo en el instante actual. No son ventanas fijas de 30/90/180 días. Fechas futuras no alimentan las estadísticas filtradas.
- Entrenamientos y rendimiento se filtran por `completedAt`; peso corporal, por `recordedAt`. Solo las sesiones finalizadas entran en el histórico confirmado.
- Se recalculan sesiones, series efectivas, duración media fiable, distribución muscular, mejores valores del período, historial de ejercicio y tendencia/historial de peso. Duración fiable conserva el criterio de 1 minuto a 6 horas.
- Sesiones/semana = sesiones del período ÷ semanas equivalentes de sus días civiles inclusivos. En Todo, desde la primera sesión hasta hoy; en los demás filtros, desde el límite seleccionado, también si hay días sin sesiones. El cómputo de días civiles evita distorsiones por horario de verano.
- Los **PR absolutos** y sus fechas se identifican expresamente como históricos. Las sugerencias conservadoras se identifican como basadas en la última sesión histórica y nunca modifican la rutina.
- Los PR se derivan antes del selector y se reutilizan al cambiar de período. Se mantienen lecturas por lote, sin consultas N+1 ni tablas/cachés persistentes nuevas.

## Gráficas y comparaciones

`StaticTrendChart` conserva su API y ahora selecciona el punto más cercano al toque. Resalta la selección y muestra fecha, valor, unidad y posición. Anterior/Posterior permiten recorrer todos los registros sin depender del Canvas; el resumen accesible describe el conjunto. Cero puntos muestra un vacío explícito; uno se centra, sin línea ni comparación inventada. Se mantienen fechas reales en el eje horizontal y rango vertical con unidades. El gráfico usa la precisión de presentación de dos decimales; e1RM sigue siendo una estimación.

Se aplica al dashboard, detalle de ejercicio y peso corporal. La tarjeta de peso del dashboard deja de capturar toda la pulsación: su enlace explícito permite interactuar con el gráfico sin navegar accidentalmente.

En el ejercicio:

- Última sesión vs. sesión inmediatamente anterior **del período**: solo si ambas tienen e1RM representativo; no se salta una sesión no comparable.
- Primer vs. último registro válido del período: requiere al menos dos estimaciones.
- PR actual vs. evento de PR previo del mismo tipo; para repeticiones, requiere además la misma carga. Sin marca previa comparable se muestra ese estado.
- Diferencia absoluta, dirección textual ↑/↓/= y porcentaje solo con baseline positivo y finito. Carga, repeticiones y e1RM conservan unidades distintas. Nunca se compara entre ejercicios.
- Epley y su límite de 1–12 reps permanecen intactos. Calentamientos e incompletas se excluyen con las reglas existentes.

## Señales de entrenamiento

Una serie guardada como completada puede mostrar **↑ Provisional**: más peso, mayor e1RM o más reps al mismo peso frente al histórico confirmado del mismo ejercicio. Se reutiliza `recordsForSession` y se exige referencia comparable; una primera carga sin antecedente no se anuncia como mejora comparativa.

El indicador está debajo de la fila, sin añadir columnas, diálogos o celebraciones. Desmarcar, convertir a calentamiento o corregir una serie lo recalcula. Las ediciones de una serie pendiente no lo activan. Tras finalizar, el histórico y los PR se recalculan desde Room y la sesión muestra **Mejora confirmada**; no se guardan banderas derivadas como fuente de verdad.

## Icono

Se incorpora íntegramente el [paquete aprobado](../icon/README.md), incluidos origen, branding y previsualizaciones. En producción:

- PNG foreground/background sin máscara en `drawable-nodpi`.
- Adaptive icon v26 y v33; v33 referencia la silueta monocromática para iconos temáticos.
- PNG de 48/72/96/144/192 px en mipmap mdpi–xxxhdpi.
- Manifest conserva `icon` y `roundIcon` apuntando a `@mipmap/ic_launcher`, según el README del paquete.
- Se eliminan el vector antiguo, fallback anydpi anterior a v26 y color verde obsoleto. No se ha generado o reinterpretado el logo.

Comprobados el launcher normal, el icono fijado en Home con iconos temáticos activados, recortes circular/redondeado y el PNG mdpi de 48 px. Para el recorte redondeado se habilitó temporalmente en el AVD una superposición del recurso Android `config_icon_mask`; se deshabilitó después. Esto no forma parte del APK.

## Pruebas

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --no-daemon
```

38 pruebas unitarias: las 34 previas y cuatro casos agrupados nuevos. Cubren límites inclusivos, cambio de mes y zona Europe/Madrid/DST, comparación positiva/negativa/neutra, referencias ausentes, baseline cero/negativo/no finito, calentamientos/incompletas, separación por ejercicio, activo vs. confirmado y recálculo de período/PR tras editar o eliminar una sesión en Room real bajo Robolectric.

Build debug y lint correctos. Lint: 0 errores, 24 avisos y una recomendación preexistente de estado primitivo. Los avisos incluyen versiones de dependencias/herramientas, dos sugerencias KTX previas y seis avisos del paquete de iconos: PNG legacy no circular usado como roundIcon y ausencia de monochrome en v26 (la variante temática está explícitamente en v33). Se preservan los recursos aprobados y no se silencian los avisos ni se actualizan dependencias fuera de alcance.

## Reproducción y validación real

El fixture es opt-in y vive únicamente en `androidTest`. **Reemplaza los datos del dispositivo de pruebas**; no debe ejecutarse en un dispositivo personal con registros reales.

```powershell
.\gradlew.bat assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.arete.app.data.DesignFixtureTest -e designFixture true -e progressFixture true -e theme DARK com.arete.app.test/androidx.test.runner.AndroidJUnitRunner
```

`progressFixture=true` distribuye las siete sesiones y pesos entre hace 220/170/100/70/40/15/3 días. La variante original del fixture A+B no cambia. `historySessions=1` permite comprobar un único registro histórico y un período sin sesiones; LIGHT/DARK selecciona el tema de prueba. No hay datos demo en producción.

Comprobado sobre la interfaz:

- 1/3/6 meses/Todo muestran respectivamente 2/4/6/7 sesiones y 32/64/96/112 series antes de finalizar la sesión activa.
- Toque selecciona otra fecha/valor; Anterior recorre la serie. Los filtros afectan también el detalle y peso, sin perder la navegación atrás.
- Una sola sesión antigua: un punto en Todo y cero en 1 mes; PR absoluto sigue visible, comparaciones sin referencia.
- Borrado de los dos pesos recientes desde la UI: gráfica con un punto y luego vacía en 1 mes; Todo conserva los registros anteriores.
- Sesión activa: desmarcar elimina la señal; editar 75 kg × 11 reps pendiente no la crea; completar muestra mayor e1RM y más reps al mismo peso; convertir a calentamiento la elimina; volver a trabajo la restaura. Finalizar confirma el histórico y el PR de 80 kg × 10 reps. Se reabrió la sesión finalizada desde Historial.
- Temas claro/oscuro, teclado numérico, scroll, diálogos de borrado, fuente al 130 % y horizontal real 2400 × 1080. Se restauraron escala y orientación al terminar.

## Capturas reales

Todas proceden de `adb shell screencap -p`, sin mockups ni retoque. Los valores diferentes reflejan filtros, fixture y operaciones reales.

| Evidencia | Capturas |
| --- | --- |
| Períodos dashboard | [Todo oscuro](progress-all-dark.png), [1 mes](progress-month-dark.png), [3 meses](progress-quarter-dark.png), [6 meses](progress-half-year-dark.png), [claro](progress-light.png) |
| Interacción | [Toque](chart-touch-dark.png), [controles accesibles](chart-accessible-dark.png), [sin datos](chart-empty-light.png), [un punto](chart-single-light.png) |
| Ejercicio | [Oscuro](exercise-dark.png), [claro](exercise-light.png), [PR](exercise-records-dark.png), [comparaciones](exercise-comparisons-dark.png), [período vacío](exercise-empty-light.png) |
| Peso | [Oscuro](body-weight-dark.png), [1 mes](body-weight-month-dark.png), [claro](body-weight-light.png), [un punto](body-weight-single-light.png), [vacío](body-weight-empty-light.png), [fuente ampliada](body-weight-large-font-light.png), [horizontal](body-weight-landscape-light.png) |
| Series | [Provisional](session-provisional-dark.png), [reps mismo peso](session-reps-provisional-dark.png), [incompleta](session-incomplete-dark.png), [calentamiento](session-warmup-dark.png), [finalizada](session-confirmed-dark.png) |
| Launcher | [Normal circular](launcher-circle-dark.png), [normal en catálogo](launcher-normal-drawer.png), [temático circular](launcher-themed-circle.png), [temático redondeado](launcher-rounded.png) |

## Límites

No se detectan P0/P1 en los flujos validados. Se mantienen los P2 conocidos: aviso del temporizador si Android mata el proceso y matching avanzado de precarga cuando cambia mucho la estructura de series. No se ha probado hardware físico ni launchers de fabricantes; las máscaras/colores temáticos dependen del launcher. La compatibilidad de recursos legacy se comprueba por empaquetado y revisión del PNG, no mediante otro AVD antiguo. No hay zoom, filtros avanzados, nuevos algoritmos de progresión, backup automático ni cambios de Room/migraciones/contratos JSON/autosave.
