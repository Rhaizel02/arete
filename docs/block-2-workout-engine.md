# Bloque 2: motor de entrenamiento

## Inicio y secuencia

`SessionRepository` calcula el siguiente workout a partir de la rutina activa y la última sesión terminada de esa rutina. El orden es secuencial y vuelve al primer workout al completar el último. La creación de una sesión se hace dentro de una transacción y devuelve la sesión activa existente si ya hay una, evitando duplicados por doble pulsación.

Los entrenamientos planificados copian a la sesión nombres, notas, descansos, incremento de carga, posición, `groupId`, objetivos de repeticiones y tipo de serie. La sesión conserva además el ID del ejercicio real usado, necesario para sustituciones, histórico y búsqueda de la última ejecución.

## Persistencia y recuperación

Room es la única fuente de verdad. Peso, repeticiones, RIR, fallo, tipo y estado de la serie; notas; orden; sustituciones; y temporizador se actualizan en el momento mediante consultas parciales. No existe un borrador exclusivamente en memoria ni un botón global de guardado.

La Home observa la sesión con estado `IN_PROGRESS`. Tras una recreación de Activity o proceso muestra **Continuar entrenamiento** y reconstruye toda la pantalla desde Room. El temporizador almacena un instante final absoluto, no un contador volátil, por lo que al volver se muestra el tiempo correcto o se limpia si ya terminó.

## Última ejecución

El DAO resuelve en una consulta por lote la sesión terminada más reciente para todos los ejercicios presentes. Las filas se agrupan en memoria por ejercicio para mostrarlas como `Última vez: 80×10 · 80×9…`. Se evita lanzar una consulta por cada tarjeta.

## Temporizador

Completar una serie de trabajo inicia el descanso del ejercicio cuando el ajuste global está activo. El banner no bloquea el resto de la sesión y ofrece +15 s, -15 s y saltar. Al alcanzar cero mientras la app está viva se emite feedback háptico. Al volver desde segundo plano, el deadline persistido mantiene el tiempo correcto.

No se programa una notificación del sistema si Android termina completamente el proceso: al reabrir se recupera el estado, pero no hay aviso fuera de la app. Se eligió esta limitación para evitar introducir servicios o alarmas innecesarios en este bloque.

## Schema y compatibilidad

Room pasa de v1 a v2 mediante `MIGRATION_1_2`; no se usa fallback destructivo. Se conservan el schema v1 y el contrato Routine JSON v1. Las columnas añadidas son:

- `workout_sessions.restTimerEndAt` y `restTimerDurationSeconds`;
- `session_exercises.sourceRoutineExerciseId` y `sessionNotes`.

## Límites intencionados

No se persisten PR, estadísticas, rachas ni otras métricas derivadas. No hay calendario, planificación por día, recordatorios, Health Connect ni lógica deportiva adicional para superseries; `groupId` solamente expresa y presenta la unidad lógica compartida.
