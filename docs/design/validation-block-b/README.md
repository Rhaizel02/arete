# Rediseño UI — Bloque B

Validación del 22 de octubre de 2026, desde `3c229ed63408f1b1c499738e1cbfe521708a19c0`. Se continúa el sistema del [Bloque A](../validation-block-a/README.md), usando su tema, Space Grotesk / Hanken Grotesk locales, cifras tabulares, colores y radios. [DESIGN y referencias Stitch](../stitch/DESIGN.md) siguen siendo referencias; no se incorpora otro sistema visual.

## Pantallas y componentes

- Rutinas: lista, estado activo, creación, cambio de nombre, activación, eliminación e importación/exportación.
- Rutina/workout: secuencia numerada, resúmenes compactos de series, grupos y descansos, menús de orden/edición/borrado y estados vacíos accionables.
- Prescripciones: dos filas por serie, selectores de trabajo/calentamiento y exactas/rango, orden sobre el borrador, descanso/incremento/grupo/notas y guardado visible sobre el teclado.
- Catálogo y selección: búsqueda, filas separadas y acceso al formulario personalizado. La ficha reutiliza la hoja técnica con clasificación, instrucciones y enlaces HTTP(S) existentes; permite editar los personalizados.
- Historial, Actividad y planificación: filas densas, estado identificado por texto e icono, selección semanal y deshacer días omitidos.
- Más/Ajustes: accesos planos, preferencias agrupadas, switches con etiqueta pulsable, objetivo semanal, recordatorios y datos con explicación del restore.
- Progreso por ejercicio/peso: métricas y gráficas del Bloque A con fechas, unidades, último valor y descripción accesible; registros y operaciones de peso conservadas.
- Diálogos, campos, selectores y confirmaciones con los mismos radios, contraste y jerarquía. Los selectores de archivos, fecha y hora continúan siendo controles nativos/Material de Android.

Se reutilizan `TonalCard`, `MetricTile`, `SectionHeading` y `StaticTrendChart`. Se incorporan `ScreenHeader`, `EditorialRow`, `StatusLabel`, `AreteField`, `PrimaryAction`, `SecondaryAction` y `AddAction`. Las acciones de añadir/guardar están en barras inferiores, no en una pila de grandes tarjetas ni en botones decorativos. Los campos usan superficies tonales y radio de 10 dp; acciones/tarjetas, el radio de 16 dp existente. No se añaden dependencias ni recursos remotos.

`EditorPresentation` contiene solo transformaciones del borrador y su resumen: agrupa únicamente series consecutivas equivalentes y mantiene el orden y la distinción de calentamientos. El guardado sigue usando `savePrescription` sin cambios.

## P2 resueltos

1. Home elimina el bloque de saludo/lema y compacta la marca en la cabecera. La sesión y la actividad útil aparecen antes sin cambiar sus datos ni prioridades.
2. La última sesión muestra cada serie como `75 kg × 10 reps`, con fecha y fallo cuando corresponde. Se elimina el sufijo ambiguo global `kg/reps`.
3. Listas planas con separadores, indicadores de estado no pulsables, campos tonales, botones secundarios de radio consistente y acciones azules inferiores reducen el aspecto Material genérico sin sustituir el tema del Bloque A.

## Compatibilidad

No hay cambios en `data/`, `AreteViewModel`, entidades, DAO, schemas, migraciones, JSON de rutinas/backup, recordatorios ni algoritmos de progreso. Se conservan autosave, completado, recuperación, bloqueo de sustitución y orden de series de sesión del Bloque A. Los cambios de sesión son de presentación y reutilización de la hoja técnica.

Importación/exportación y backup/restore siguen usando sus launchers, validaciones y repositorios originales. Las confirmaciones no omiten la validación previa a restaurar. No hay filtros avanzados, nuevos servicios, perfiles, métricas ficticias ni funcionalidades adicionales del backlog.

## Validación automatizada

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --no-daemon
```

- 34 pruebas unitarias superadas: las 32 del Bloque A y dos pruebas pequeñas de presentación del editor (orden/límites del borrador y resumen de series consecutivas con calentamientos).
- APK debug compilado correctamente.
- Lint: 0 errores, 18 avisos de versiones de herramientas/dependencias y una recomendación preexistente de estado primitivo en el editor de peso. No se actualizan dependencias fuera de alcance.
- Sin suite UI nueva. La comprobación de los flujos se realiza sobre el APK real en el AVD.

## AVD y datos de prueba

AVD `Arete_API_35`, Android 15/API 35, 1080 × 2400. Se reutiliza el fixture opt-in del Bloque A (`DesignFixtureTest`) y se crean y editan datos adicionales a través de la interfaz: otra rutina, un workout y un ejercicio personalizado. Todo sucede en el emulador; no se inyectan datos ni un modo demo en código de producción.

El fixture se reproduce con los comandos de [validación A](../validation-block-a/README.md#reproducir-datos-y-capturas). **Reemplaza los datos del dispositivo de pruebas**, por lo que no debe ejecutarse sobre un dispositivo con registros personales. Después de la carga, las capturas de este bloque reflejan también ediciones realizadas por la UI.

Las imágenes se obtienen con `adb shell screencap -p`; no son HTML, previews Compose ni imágenes generadas. Las diferencias de contenido entre capturas responden a operaciones de validación, no a constantes dentro de las pantallas.

## Validación manual realizada

| Flujo | Comprobación sobre el APK |
| --- | --- |
| Rutinas | Crear, activar, cambiar nombre a Fuerza B, volver a activar Torso/Pierna y confirmar eliminación de la copia importada de prueba. |
| Workouts | Crear Pierna B, comprobar su vacío accionable y moverlo en la secuencia. Mover Press inclinado antes de Press banca y devolverlo a su posición. |
| Prescripción | Cambiar el mínimo a 6, marcar calentamiento, bajar esa serie, guardar y reabrir: valor, tipo y orden persisten. Guardado accesible con teclado numérico abierto. |
| Catálogo/picker | Búsqueda sin resultados y búsqueda Remo; crear Remo unilateral B con clasificación e instrucciones, editar su nombre inglés y consultar la ficha. Añadirlo desde el selector a Pierna B y guardar sus tres series de plantilla. |
| Planificación/actividad | Asignar Pierna B al lunes; omitir hoy desde Inicio y deshacerlo desde Actividad. Estado y planificación conservados después del backup/restore. |
| Historial/progreso | Abrir una sesión finalizada, consultar el último rendimiento con unidades y navegar al detalle de fuerza desde Progreso. |
| Peso | Crear 78,3 kg, editar a 78,4 kg, confirmar borrado y abrir/cancelar el selector de fecha. |
| Routine JSON | Exportar a Downloads con Android, importar el archivo generado y comprobar los dos workouts y las prescripciones. `schemaVersion = 1`. |
| Backup JSON | Exportar, cambiar objetivo de 3 a 4 y restaurar mediante confirmación: vuelve a 3. Archivo `backupSchemaVersion = 1`, con dos rutinas, ocho sesiones y siete registros de peso. |
| Presentación | Claro/oscuro, scroll, teclado, menús, confirmaciones, título largo con fuente al 130 % y orientación horizontal real (`ROTATION_90`, 2400 × 1080). |

Las exportaciones de prueba permanecen solo en Downloads del emulador; no se versionan como contratos nuevos ni como datos de producción. Se revisaron las capturas contra el Bloque A/Stitch: superficies y tipografía comunes, botones azules de radio consistente, densidad útil y ausencia de perfiles/acciones ficticios. También se igualó la altura de las métricas del detalle de progreso cuando una tiene una segunda línea.

## Capturas representativas

| Pantalla | Oscuro | Claro |
| --- | --- | --- |
| Rutinas | [Captura](routines-dark.png) | [Captura](routines-light.png) |
| Secuencia de workouts | [Captura](routine-editor-dark.png) | [Captura](routine-editor-light.png) |
| Editor de workout | [Captura](workout-editor-dark.png) | [Captura](workout-editor-light.png) |
| Prescripciones | [Captura](prescription-dark.png) | [Captura](prescription-light.png) |
| Catálogo | [Captura](catalog-dark.png) | [Captura](catalog-light.png) |
| Planificación | [Captura](planning-dark.png) | [Captura](planning-light.png) |
| Más | [Captura](more-dark.png) | [Captura](more-light.png) |
| Ajustes | [Captura](settings-dark.png) | [Captura](settings-light.png) |
| Progreso por ejercicio | [Captura](exercise-progress-dark.png) | [Captura](exercise-progress-light.png) |
| Peso corporal | [Captura](body-weight-dark.png) | [Captura](body-weight-light.png) |

Otras comprobaciones: [Home compacto](home-dark.png), [unidades de última sesión](session-units-dark.png), [Historial](history-dark.png), [Actividad y día omitido](activity-dark.png), [ficha técnica](exercise-detail-dark.png), [formulario personalizado](custom-exercise-dark.png), [ficha personalizada](custom-detail-dark.png), [backup/restore](backup-confirm-dark.png), [confirmación de borrado](delete-dialog-dark.png), [fecha](weight-date-light.png), [teclado](prescription-keyboard-dark.png), [fuente ampliada](prescription-large-font-light.png), [horizontal](prescription-landscape-light.png), [búsqueda](catalog-search-light.png), [vacío de búsqueda](catalog-empty-light.png), [workout vacío](workout-empty-dark.png) y [selección de personalizado](workout-custom-light.png).

## Fuera de alcance y límites

Los selectores del sistema pueden conservar la apariencia y el idioma configurados en Android. No se han personalizado ni reemplazado por un gestor de archivos propio. No se ha validado hardware físico ni otras versiones de Android en este bloque. Las gráficas continúan siendo estáticas; los filtros avanzados permanecen fuera de alcance según el prompt del Bloque B.
