# Bloque 4: cierre funcional de la V1

## Planificación

La rutina activa sigue avanzando únicamente al completar sesiones según el histórico. La planificación relaciona cada día de la semana con un workout como orientación y para decidir cuándo avisar, pero nunca inicia, omite ni adelanta entrenamientos automáticamente.

Los días usan una clave estable derivada de la fecha del calendario local. Un día omitido guarda snapshots de rutina/workout en `skipped_planned_workouts`; no crea `WorkoutSession`, no afecta a rendimiento y puede deshacerse. La vista Actividad cubre los últimos 14 días y los próximos 7.

## Recordatorios

Se utiliza `AlarmManager.setAndAllowWhileIdle`, sin solicitar alarmas exactas. Se calcula el siguiente día planificado a la hora seleccionada y se reprograma después del aviso, al cambiar preferencias, tras reinicio y cuando cambia fecha/zona. Antes de notificar se consulta Room y se descarta el aviso si existe una sesión completada hoy, un día omitido o una sesión activa.

Android 13 o superior solicita `POST_NOTIFICATIONS` al activar la opción. La exactitud final depende de Doze y las políticas del fabricante; este comportamiento se explica en Ajustes.

## Peso corporal y preferencias

El editor de peso abre un selector de fecha tanto al crear como al editar, convierte la fecha elegida de manera estable entre UTC del DatePicker y calendario local y mantiene el histórico ordenado por `recordedAt`.

Tema, temporizador, vibración, objetivo, recordatorios, hora y planificación están centralizados en DataStore e incluidos en el backup.

## Compatibilidad

No hay cambio de schema: Room continúa en v2 y conserva la migración explícita v1→v2. Routine JSON continúa en v1. El nuevo Backup JSON nace en v1 y se documenta por separado.

Los P2 previos permanecen fuera de alcance: notificación del temporizador si Android termina el proceso y matching avanzado de precarga ante cambios fuertes en la estructura de series.

## Verificación

- 28 pruebas unitarias: round-trip de todas las entidades y preferencias, relaciones/IDs/snapshots, rechazo de JSON/versiones/colecciones incompletas, rollback real con fallo SQLite inyectado, fecha editable y orden de peso, planificación, omisión/deshacer y supresión de avisos.
- `testDebugUnitTest`, `assembleDebug` y `lintDebug`; lint conserva únicamente avisos de dependencias y una sugerencia de estado primitivo.
- Dos pruebas instrumentales en AVD API 35: migración v1→v2 y publicación real de notificación con permiso concedido.
- Comprobación manual mediante ADB y jerarquía accesible: restore por Storage Access Framework con confirmación, Home planificada, omisión manteniendo el próximo workout, Actividad, selector de fecha y guardado de peso para un día anterior, exportación real de Backup JSON y lectura del archivo resultante. Ajustes revisados visualmente en claro/oscuro y orientación horizontal; la navegación conserva el destino al girar.
