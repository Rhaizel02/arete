# Arete

Arete es una aplicación Android personal y offline-first para diseñar rutinas de gimnasio. Este repositorio contiene el **Bloque 1**: catálogo local, ejercicios personalizados, rutinas y prescripciones. La ejecución y el historial funcional de entrenamientos quedan deliberadamente fuera de este bloque.

## Stack y arquitectura

- Kotlin, Jetpack Compose y Material 3 en una única actividad.
- Room como fuente de verdad; los flujos de Room alimentan directamente el `ViewModel` y la UI.
- DataStore Preferences para la elección de tema `SYSTEM`, `LIGHT` o `DARK`.
- Un repositorio de aplicación concentra las operaciones que necesitan transacciones (activación, reordenación, importación y edición completa de prescripciones).
- Sin backend, red, DI framework ni capas ceremoniales.

El código se organiza en:

- `data/local`: entidades, relaciones, DAO y base Room.
- `data`: repositorio, catálogo, preferencias y contrato Routine JSON.
- `ui`: navegación ligera, pantallas Compose y tema.

## Schema Room v1

El schema exportado está versionado en `app/schemas`.

Tablas del Bloque 1:

- `exercises`
- `routines`
- `workouts`
- `routine_exercises`
- `prescription_sets`

Tablas preparadas para el Bloque 2, todavía sin lógica de ejecución:

- `workout_sessions`
- `session_exercises`
- `session_sets`
- `body_weight_entries`
- `skipped_planned_workouts`

`SessionExercise` conserva nombres, descanso, incremento, notas, grupo y orden. `SessionSet` conserva los objetivos de repeticiones y el tipo de serie, además de los campos que más adelante registrarán peso, repeticiones, RIR, fallo, estado y timestamp. Así, modificar una rutina nunca reescribirá el entrenamiento histórico.

## Catálogo

`app/src/main/assets/exercises_catalog.json` contiene 44 ejercicios habituales con nombre español e inglés, musculatura, equipamiento e instrucciones. Se importa de forma transaccional la primera vez que la base está vacía y la búsqueda funciona completamente offline por ambos nombres.

## Routine JSON

El contrato independiente de los futuros backups se documenta en [docs/routine-json-v1.md](docs/routine-json-v1.md). La aplicación exporta JSON legible e importa únicamente versiones compatibles conocidas. La resolución usa ID, nombre español y nombre inglés, en ese orden; si no encuentra el ejercicio, crea uno personalizado sin abortar el resto de la rutina.

## Verificación

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Las pruebas usan Room real en memoria bajo Robolectric y cubren catálogo, CRUD, cascadas, activación, orden, prescripciones, contrato v1, resolución, ejercicios desconocidos, duplicados evidentes, errores de validación y round-trip exportación/importación.

