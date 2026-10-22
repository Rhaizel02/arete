# Backup JSON v1

El backup completo es un contrato distinto de Routine JSON. Su raíz contiene:

```json
{
  "backupSchemaVersion": 1,
  "exportedAt": 1800000000000,
  "database": {},
  "preferences": {}
}
```

`database` incluye las colecciones completas de ejercicios, rutinas, workouts, ejercicios y prescripciones de rutina, sesiones, ejercicios y series de sesión, pesos corporales y días omitidos. Se conservan IDs, relaciones, posiciones, timestamps, snapshots, estado de sesión y temporizador.

`preferences` incluye tema, temporizador, vibración, objetivo semanal, activación y hora de recordatorios y planificación semanal.

## Compatibilidad

`backupSchemaVersion` es obligatorio. La versión actual acepta exclusivamente v1 y tolera campos desconocidos para facilitar ampliaciones compatibles. Las formas serializadas de v1 deben conservarse en futuras versiones; cualquier evolución incompatible deberá añadir un lector/migrador de contrato anterior en vez de invalidar backups existentes.

Routine JSON conserva `schemaVersion: 1` y no se interpreta como backup ni viceversa.

## Restore

La semántica es reemplazo completo:

1. se decodifica y valida versión, IDs, relaciones, posiciones, valores y preferencias sin escribir datos;
2. se eliminan e insertan todas las tablas Room en una única transacción con claves foráneas activas;
3. tras confirmar la transacción se aplican las preferencias mediante una única edición de DataStore.

Un error de validación no modifica nada. Un fallo de Room revierte la transacción. Room y DataStore no ofrecen una transacción distribuida; el pequeño paso final de preferencias se realiza únicamente después de confirmar la base.
