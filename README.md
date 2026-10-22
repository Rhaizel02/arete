# Arete

Arete es una aplicación Android personal y offline-first para diseñar rutinas, planificar la semana, registrar entrenamientos reales y convertir el histórico en progreso comprensible. Los Bloques 1 a 4 cierran la V1 con recordatorios locales, actividad, días omitidos y backup/restore completo.

## Stack y arquitectura

- Kotlin, Jetpack Compose y Material 3 en una única actividad.
- Room es la fuente de verdad para rutinas, sesiones activas e historial.
- DataStore Preferences centraliza tema, temporizador, vibración, objetivo semanal, planificación y recordatorios.
- Los repositorios concentran operaciones transaccionales: activación y edición de rutinas, importación, creación de snapshots y mutaciones del entrenamiento.
- Sin backend, red, framework de inyección ni estadísticas derivadas persistidas.

El código se organiza en:

- `data/local`: entidades, relaciones, DAO, migraciones y base Room.
- `data`: repositorios, catálogo, preferencias y contrato Routine JSON.
- `ui`: navegación ligera, pantallas Compose y tema.

## Motor de entrenamiento

La Home propone el siguiente workout secuencial de la rutina activa. Si existe una sesión en curso, la acción **Continuar entrenamiento** tiene prioridad y se impide crear otra sesión activa por accidente.

Al comenzar un workout se copian sus ejercicios, prescripciones, notas, descansos, orden y grupos a snapshots de sesión. La pantalla de entrenamiento permite:

- registrar kg, repeticiones, RIR opcional (5+ a 0), fallo y series de calentamiento/trabajo;
- completar una serie con una pulsación y precargar valores útiles de la última sesión;
- consultar la última ejecución de cada ejercicio sin abrir el historial y mediante una consulta Room por lote;
- añadir, borrar y reordenar ejercicios y series;
- sustituir un ejercicio únicamente en la sesión actual; si ya hay series completadas se conserva su identidad histórica y se ofrece añadir el sustituto como ejercicio nuevo;
- representar superseries, trisets y circuitos con `groupId`;
- usar un temporizador de descanso opcional con ±15 s, salto y vibración;
- iniciar un entrenamiento libre y guardarlo después como rutina;
- finalizar, recuperar, editar o eliminar sesiones desde el historial.

Cada edición relevante se escribe inmediatamente en Room mediante consultas parciales. La sesión activa, las series completadas y el fin absoluto del temporizador sobreviven a navegación, cambio de aplicación y recreación del proceso.

## Progreso

El destino **Progreso** resume actividad reciente, objetivo semanal, racha, PR, estadísticas, sugerencias conservadoras y peso corporal. El detalle de cada ejercicio muestra sesiones, mejor peso/serie, e1RM estimado, evolución y un historial con gráfica. Todos estos datos se derivan de una consulta Room conjunta; no existen tablas de estadísticas ni cachés que puedan desincronizarse al editar o eliminar una sesión.

El e1RM usa Epley (`peso × (1 + repeticiones / 30)`) y solo admite series de trabajo completadas de 1 a 12 repeticiones. Los PR cubren mayor peso, mayor e1RM estimado y más repeticiones para un peso. La racha cuenta semanas de lunes a domingo que alcanzan el objetivo; una semana actual incompleta no rompe prematuramente la racha anterior.

## Planificación y backup

La planificación semanal asigna workouts orientativos a días, sin cambiar la secuencia real de la rutina. **Hoy no entreno** registra un día omitido independiente, reversible y sin crear sesiones falsas. La vista Actividad distingue entrenamientos, días pendientes, omitidos y no planificados.

Los recordatorios son locales, opcionales e inexactos: se programan únicamente para días planificados y se omiten si ya se entrenó, se marcó el día como omitido o existe una sesión activa. Android puede retrasarlos por ahorro de batería. Se reprograman tras cambios de hora, zona o reinicio.

Routine JSON v1 continúa dedicado a compartir rutinas. El contrato separado [Backup JSON v1](docs/backup-json-v1.md) contiene toda la base y las preferencias necesarias. El restore valida primero y reemplaza todas las tablas Room en una transacción; nunca mezcla historiales silenciosamente.

## Schema Room v2

Los schemas exportados v1 y v2 están versionados en `app/schemas`. La migración explícita `MIGRATION_1_2` conserva los datos existentes y añade el estado persistente del temporizador, la relación con el ejercicio de rutina de origen y notas editables por ejercicio de sesión.

Tablas principales:

- `exercises`, `routines`, `workouts`, `routine_exercises`, `prescription_sets`;
- `workout_sessions`, `session_exercises`, `session_sets`;
- `body_weight_entries`, con fecha editable, y `skipped_planned_workouts`, para días planificados omitidos.

Los snapshots de sesión aseguran que editar una plantilla no modifique entrenamientos en curso ni históricos.

## Routine JSON

El contrato sigue en la versión 1 y se documenta en [docs/routine-json-v1.md](docs/routine-json-v1.md). La aplicación exporta JSON legible e importa únicamente versiones compatibles conocidas. La resolución usa ID, nombre español y nombre inglés, en ese orden; si no encuentra el ejercicio, crea uno personalizado sin abortar el resto de la rutina.

## Verificación

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Las pruebas locales usan Room real en memoria bajo Robolectric. Además de la cobertura previa, validan el bloqueo de sustitución histórica, e1RM, cada tipo de PR, exclusiones, recálculo tras editar/borrar, límites semanales, rachas, sugerencias, estadísticas y CRUD/tendencia de peso. La prueba instrumental abre una base v1 real, ejecuta la migración a v2 y comprueba tanto los datos previos como las columnas nuevas.

Más detalles: [Bloque 2](docs/block-2-workout-engine.md), [Bloque 3](docs/block-3-progress.md) y [Bloque 4](docs/block-4-v1.md).

## Rediseño UI — Bloque A

Inicio, sesión con un ejercicio destacado y Progreso comparten la nueva identidad light/dark, con Space Grotesk y Hanken Grotesk locales. La navegación principal es Inicio, Rutinas, Progreso y Más; Más reúne Ejercicios, Historial, Actividad y Ajustes. La sesión conserva autosave, descansos y guía técnica, y permite reordenar sus series desde el menú de cada fila.

Referencias, seis capturas reales, excepciones autorizadas y resultados de validación: [Bloque A](docs/design/validation-block-a/README.md). El alcance posterior está delimitado en [STITCH_BACKLOG](docs/product/STITCH_BACKLOG.md).

## Rediseño UI — Bloque B

La identidad del Bloque A se extiende a rutinas, workouts, prescripciones, catálogo y fichas, ejercicios personalizados, historial, actividad, planificación, ajustes, detalle de progreso y peso corporal. Listas compactas con separadores, campos tonales y acciones inferiores compartidas mantienen los flujos originales. Home reduce su encabezado decorativo y la última ejecución de sesión muestra kg y reps explícitos por serie.

Validación funcional, compatibilidad y capturas reales: [Bloque B](docs/design/validation-block-b/README.md).
