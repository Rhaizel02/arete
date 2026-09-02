# Arete

Arete es una aplicación Android personal y offline-first para diseñar rutinas y registrar entrenamientos reales. Los Bloques 1 y 2 incluyen catálogo local, ejercicios personalizados, rutinas, prescripciones, ejecución de sesiones, entrenamiento libre e historial editable.

## Stack y arquitectura

- Kotlin, Jetpack Compose y Material 3 en una única actividad.
- Room es la fuente de verdad para rutinas, sesiones activas e historial.
- DataStore Preferences guarda el tema y el ajuste global del temporizador.
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
- sustituir un ejercicio únicamente en la sesión actual;
- representar superseries, trisets y circuitos con `groupId`;
- usar un temporizador de descanso opcional con ±15 s, salto y vibración;
- iniciar un entrenamiento libre y guardarlo después como rutina;
- finalizar, recuperar, editar o eliminar sesiones desde el historial.

Cada edición relevante se escribe inmediatamente en Room mediante consultas parciales. La sesión activa, las series completadas y el fin absoluto del temporizador sobreviven a navegación, cambio de aplicación y recreación del proceso.

## Schema Room v2

Los schemas exportados v1 y v2 están versionados en `app/schemas`. La migración explícita `MIGRATION_1_2` conserva los datos existentes y añade el estado persistente del temporizador, la relación con el ejercicio de rutina de origen y notas editables por ejercicio de sesión.

Tablas principales:

- `exercises`, `routines`, `workouts`, `routine_exercises`, `prescription_sets`;
- `workout_sessions`, `session_exercises`, `session_sets`;
- `body_weight_entries` y `skipped_planned_workouts`, reservadas para bloques posteriores.

Los snapshots de sesión aseguran que editar una plantilla no modifique entrenamientos en curso ni históricos.

## Routine JSON

El contrato sigue en la versión 1 y se documenta en [docs/routine-json-v1.md](docs/routine-json-v1.md). La aplicación exporta JSON legible e importa únicamente versiones compatibles conocidas. La resolución usa ID, nombre español y nombre inglés, en ese orden; si no encuentra el ejercicio, crea uno personalizado sin abortar el resto de la rutina.

## Verificación

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Las pruebas locales usan Room real en memoria bajo Robolectric. Además de la cobertura del Bloque 1, validan creación de snapshots, sesión activa única, autosave y recuperación, última sesión y precarga, finalización, borrado, sustitución temporal, mutaciones dinámicas, entrenamiento libre, temporizador y `groupId`. La prueba instrumental abre una base v1 real, ejecuta la migración a v2 y comprueba tanto los datos previos como las columnas nuevas.

Más detalles de implementación y validación: [docs/block-2-workout-engine.md](docs/block-2-workout-engine.md).
