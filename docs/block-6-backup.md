# Bloque 6 — Copias externas y recuperación

Se amplía la V1 sin modificar `backupSchemaVersion = 1`, Routine JSON, schema Room ni snapshots. `BackupService.export()` y su lector/validador siguen siendo la única serialización del histórico.

## Arquitectura y programación

`AutomaticBackupCoordinator` serializa con un mutex las copias, cambios de destino y restore. El acceso bloqueante al proveedor se ejecuta en `Dispatchers.IO`. `SafBackupDestination` concentra DocumentsContract y ContentResolver; sus interfaces de destino, configuración y programación permiten inyectar fallos en pruebas sin duplicar el formato JSON.

Un DataStore independiente, `automatic_backup.preferences_pb`, guarda destino, activación, resultado y registro de copias automáticas. No forma parte del JSON ni del backup/transferencia de Android. El registro guarda URI de carpeta/archivo, fecha y SHA-256; nunca se descubre propiedad por un prefijo de nombre.

WorkManager 2.12.0 registra `arete.backup.daily` mediante trabajo periódico único, política KEEP, intervalo de 24 horas y sin demora inicial. La primera ejecución es elegible cuanto antes; Android puede retrasarla. `arete.backup.now` es un trabajo único inmediato con KEEP. No se imponen red o carga para impedir copias locales. Tras un fallo transitorio se informa del error y se mantiene la siguiente oportunidad diaria; una copia inmediata también puede solicitarse de nuevo.

La desactivación cancela la tarea diaria, no elimina archivos. Los workers periódicos comprueban además la activación antes de escribir. Inicio de aplicación, entrada en Ajustes y restore reconcilian configuración y permisos. Un acceso revocado o destino eliminado desactiva la programación; se exige elegir/conceder acceso y activar de nuevo. Proveedores con permiso solo temporal permiten copias inmediatas, no automatización.

## Integridad y retención

1. Exportar y validar Backup JSON v1 antes de crear un archivo.
2. Crear un documento nuevo con nombre `Arete_backup_auto|manual_fecha_hora_milisegundos_UUID.json`. Consultar los documentos existentes y rechazar que el proveedor devuelva uno de ellos.
3. Escribir, vaciar y cerrar el stream. Releer; comprobar tamaño, SHA-256 y validación v1.
4. Solo entonces registrar la copia como correcta. Una copia verificada cuyo registro local falle se conserva como archivo no gestionado.
5. Solo después de una nueva automática correcta, verificar las automáticas registradas y retirar las más antiguas que excedan siete. El orden de verificación serializado prevalece sobre el reloj del dispositivo, de modo que un ajuste de fecha no elimine la copia recién creada. Revalidar su hash inmediatamente antes de borrar.

Las copias manuales —tanto Crear copia ahora como exportaciones anteriores— nunca entran en retención. Tampoco se borran archivos ajenos, aunque su nombre parezca automático. Una copia cambiada externamente, ilegible o no eliminable interrumpe la limpieza y produce una advertencia: puede haber más de siete archivos, priorizando su conservación. Cambiar de carpeta no limpia la anterior. Tras reinstalar se pierde deliberadamente el registro local de propiedad: las copias antiguas se pueden restaurar, pero no se adoptan ni borran automáticamente.

No se presupone un rename atómico ni una confirmación remota duradera del proveedor. Se escribe directamente un documento nuevo y solo se anuncia éxito tras releerlo. Si falla, se intenta retirar únicamente el documento recién creado. Si Android mata el proceso puede quedar un archivo parcial con nombre final; no se reconoce como copia verificada ni provoca limpieza y el lector rechaza su JSON si está incompleto. El último éxito anterior se conserva. Los JSON no tienen cifrado adicional; una carpeta exclusivamente local no cubre la pérdida física del móvil.

## Recuperación y preferencias

Se conserva el selector de archivos y la confirmación de reemplazo total, sin mezclar ni restaurar automáticamente por encontrar archivos. Lectura, exportación y restore se realizan fuera del hilo principal. Los formatos manuales anteriores y los automáticos usan el mismo lector v1.

El restore valida todo antes de escribir. Room no confirma la sustitución hasta que se aplican las preferencias. Si estas fallan, la base revierte y se intenta compensar las preferencias previas en contexto no cancelable; un fallo de compensación se comunica sin afirmar que todas las preferencias quedaron intactas. Room y DataStore no tienen una transacción distribuida ante terminación abrupta: puede quedar una diferencia de preferencias si el proceso muere entre ambos commits. Los datos fitness están protegidos por la transacción Room.

La configuración automática no se importa desde ningún JSON. Después del restore se comprueba la configuración local y los permisos reales; una instalación limpia queda sin destino y desactivada aunque el JSON proceda de un móvil que hacía copias diarias.

## Android Auto Backup

Se mantiene `allowBackup=true` con listas explícitas para cloud y transferencia de dispositivo en Android moderno y reglas legacy. Solo se incluyen `arete.db`/sus archivos auxiliares y el DataStore de preferencias generales. No se incluyen el DataStore automático ni la base de WorkManager. Los permisos SAF deben concederse localmente; no se reconstruyen desde backups. El transporte cloud requiere capacidades de cifrado del sistema en Android moderno. Esta protección complementaria no sustituye el JSON que el usuario puede seleccionar y recuperar.

Referencias oficiales: [SAF y permisos persistentes](https://developer.android.com/training/data-storage/shared/documents-files), [trabajo único](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work), [pruebas WorkManager](https://developer.android.com/develop/background-work/background-tasks/testing/persistent/integration-testing), [reglas Auto Backup](https://developer.android.com/identity/data/autobackup).

Resultados y capturas: [validación del Bloque 6](design/validation-block-6/README.md). Los P2 anteriores de temporizador y matching de precarga se mantienen fuera de alcance.
