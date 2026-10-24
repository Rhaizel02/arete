# Macrobloque 9–10 — Entrenamiento 2.0 y Progreso 3.0

Base: `f23da092285633279fd677e69a0f24dd0289e905`. Se conserva Room v2, migración 1→2, Routine JSON v1, Backup JSON v1, copias automáticas y catálogo editorial del Bloque 8. No hay nuevas tablas, columnas, ejercicios, recursos gráficos ni dependencias. Los nuevos modelos son derivados o borradores de interfaz.

## 1. Precarga por tipo y aparición

`SetPrefill.kt` centraliza la selección de aparición histórica y el matching. Solo recibe filas completadas de sesiones finalizadas cuyo `completedAt` es estrictamente anterior al inicio del snapshot nuevo. La consulta elige una sola sesión histórica por ID estable, con desempate estable por ID de sesión; no mezcla nombres ni sesiones con la misma fecha.

Se selecciona primero la aparición con el mismo `sourceRoutineExerciseId`, cuando existe; en su defecto, el ordinal de aparición del ejercicio dentro del workout. Ese ordinal y el número total de apariciones se calculan sobre todas las apariciones históricas, incluidas las que no completaron ninguna serie: no se desplaza la segunda aparición al hueco de la primera. Si el histórico repetido tiene identidades de plantilla y la identidad solicitada no tiene filas completadas, no se cae al ordinal de otra identidad: queda vacío, también tras reordenar. En una plantilla nueva sin identidad comparable, ese caso ambiguo se conserva vacío en lugar de mezclar instancias. Para una aparición única o histórico libre sin identidad de plantilla, sí se admite el fallback ordinal conservador.

Dentro de la aparición elegida se separan calentamiento y trabajo, se ordenan por posición y se usa el ordinal relativo **entre referencias completadas del mismo tipo**. Sobran series nuevas: se repite la última referencia de ese tipo, sin aumentar cargas. Faltan series nuevas: se toman las primeras referencias. Falta ese tipo: kg/reps vacíos. No se calculan porcentajes de calentamiento.

La misma función se aplica al inicio desde plantilla, al añadir en sesión libre/planificada y al sustituir explícitamente un ejercicio sin series completadas. La sustitución conserva los objetivos y tipos actuales; no modifica la rutina. Se mantienen descansos, rangos y grupos del snapshot nuevo. RIR, fallo y completado no se heredan como rendimiento nuevo.

Solo se precarga al crear/sustituir snapshots, nunca en un Flow ni al recuperar la pantalla. Inicio transaccional y comprobación de sesión activa evitan sesiones duplicadas por doble pulsación concurrente. Los campos editados no vuelven a precargarse por cambios de histórico o plantilla. La «última sesión» visible también usa la aparición correspondiente, no una mezcla de duplicados.

## 2. Descanso y Android

`RestTimerCoordinator` usa el deadline ya persistido en Room; no hay contador paralelo persistido, WorkManager de segundos ni servicio permanente. Cada completado que inicia descanso confirma Room antes de reconciliar el aviso; ajuste, salto, finalización, borrado, cambios de configuración y restore reconcilian/cancelan la programación. La aplicación vuelve a comprobar el estado al regresar a primer plano. Receptores de reinicio, cambio de hora/zona, actualización de paquete y concesión del acceso especial recuperan únicamente descansos aún futuros.

Un único PendingIntent reemplazable identifica la alarma. Su Intent lleva ID/deadline, pero el receptor vuelve a consultar la sesión activa real dentro de una transacción. `consumeTimer` compara ID, estado y deadline y limpia las columnas existentes una sola vez. Un evento antiguo, temprano, ya consumido, saltado, sustituido o de una sesión terminada/borrada no puede avisar ni limpiar un descanso más reciente. UI y receptor pasan por el mismo coordinador; mutex y compare-and-set Room evitan avisos/vibraciones duplicados.

Durante restore se cancelan avisos y se rechazan entregas anteriores mediante un estado efímero del coordinador. No se mantiene ese mutex esperando a un proveedor SAF lento; al terminar, también por cancelación ordinaria, se reconcilia el estado real. No se añade ese estado efímero al backup ni a Room.

Si la sesión está visible se usa solamente feedback local según Vibración. Fuera de ella se muestra «Descanso terminado · Puedes continuar con tu entrenamiento», con canales distintos de recordatorios semanales e identificador 910. Pulsarla consulta la sesión activa y conduce a ella; nunca inicia una nueva. Un descanso expirado encontrado al abrir/restore/reiniciar se descarta, no genera aviso atrasado. Una entrega de alarma con más de 120 segundos de retraso también se descarta: un recordatorio tardío de un descanso corto deja de resultar útil.

### Permisos y garantías

- Avisos externos opcionales, desactivados por defecto. Se guarda una preferencia local `rest_alerts_enabled`; **no se añade a BackupPreferences ni a los JSON v1**, y el restore no la importa ni la sustituye. El temporizador visual permanece independiente.
- Android 13+: se solicita POST_NOTIFICATIONS solo al activar los avisos; la denegación no impide entrenar. Se explica el estado bloqueado y se ofrece abrir los ajustes Android.
- Android 12+: se comprueba `canScheduleExactAlarms()`. Se declara **SCHEDULE_EXACT_ALARM**, no USE_EXACT_ALARM. Solo el botón «Permitir avisos puntuales», disponible al activar la función, abre el acceso especial. Se captura una posible SecurityException y se degrada a `setAndAllowWhileIdle` si no se puede usar `setExactAndAllowWhileIdle`.
- Sin acceso exacto, el aviso es aproximado y puede no resultar útil antes del límite de caducidad; la pantalla lo explica. Permiso exacto no equivale a garantía universal frente a cuotas de Doze, batería o fabricante.
- Android cancela las alarmas exactas y puede terminar el proceso al revocar el acceso especial. La siguiente apertura/evento de reconciliación aplica el fallback; no se promete una recuperación inmediata sin recibir ejecución del sistema.
- Tras **Forzar detención** no se promete entrega. Tras reiniciar se necesita que Android entregue BOOT_COMPLETED y permita acceso al almacenamiento de la app, normalmente tras desbloquear. No se usa almacenamiento direct-boot ni un servicio para eludir restricciones.
- Se elige semántica de **a lo sumo un intento**: la transacción consume el deadline antes de emitir feedback. Una terminación abrupta exactamente entre consumo y notificación podría perder el aviso; no se persiste otra bandera ni se promete entrega exactamente una vez. El entrenamiento y sus series permanecen intactos.

Fundamento consultado: [programación y permisos de alarmas Android](https://developer.android.com/develop/background-work/services/alarms), [permiso de notificaciones](https://developer.android.com/develop/ui/compose/notifications/notification-permission). Las páginas externas no son dependencias de ejecución.

## 3. Edición y ergonomía

El editor de prescripciones guarda su borrador en `rememberSaveable`: campos, notas, grupo, inicialización, orden e identidad UUID de cada fila y versión original de la prescripción. Un Saver serializa el estado de interfaz, no el contrato de rutinas. Admite estados intermedios inválidos sin escribirlos en Room. Añadir copia valores pero crea otra identidad; reordenar no intercambia estados de inputs por índice.

El guardado sigue siendo explícito. No se reinicializa el borrador con cada emisión del Flow. Antes de guardar se comprueba el original y las series; el repositorio repite la comprobación optimista **dentro de la transacción**, evitando una carrera entre validación y escritura. Si la plantilla cambió o desapareció, no se sobrescribe: se conserva el borrador y se pide reabrir para revisar la versión actual. Sin una política nueva de merge ni borradores en Room.

El registro de sesión conserva autosave por campo, teclado decimal con coma/punto, avance de foco, controles RIR/fallo y menú de calentamiento/orden/borrado. Sus estados se guardan por ID de serie, no por posición; se evita aceptar otra pulsación de completado mientras esa operación está en curso. No hay botón global de guardado ni diálogos nuevos para completar.

## 4. Duración estimada

Función pura `estimateWorkoutDuration`, recalculada sobre la plantilla observada y presentada en el detalle del workout. Vacío/sin series: sin estimación.

Para cada ejercicio con series:

```text
Mínimo = 60 s de preparación + Σ(repsMin × 2 s) + (nSeries − 1) × descanso
Máximo = 120 s de preparación + Σ(repsMax × 4 s) + (nSeries − 1) × descanso
```

Se suman ejercicios y se redondea el mínimo hacia abajo y el máximo hacia arriba a minutos, con mínimo visual de uno. Calentamientos cuentan con su propio rango; no se añade descanso después de la última serie de cada ejercicio. El tiempo de preparación incluye transición; no se suma además un descanso al final del workout. Descansos válidos: 0–3600 s.

Ejemplo: un ejercicio, **2 × 10 reps y 90 s de descanso**: 190–290 segundos → **3–5 min**. No se guarda esa cifra como duración histórica. Grupos/superseries conservan todos los descansos configurados, sin inferir rotaciones o una prescripción de tiempos inexistente; se indica menor fiabilidad. No es predicción individual ni recomendación científica de cadencia.

## 5. Volumen externo registrado

`ExternalVolumeMetrics` reutiliza las filas del único join de ProgressRepository, que excluye sesiones activas. Todo se deriva otra vez al cambiar Room: sin caché persistida ni consultas por ejercicio.

```text
Volumen externo registrado (kg·reps) = Σ(peso en kg × repeticiones)
```

Solo series de trabajo completadas, reps positivas y kg positivos/finitos; un producto no finito se descarta. Se mantienen por separado series efectivas, repeticiones y número de series con carga externa conocida. Cargas nulas, cero o inválidas no se convierten en peso corporal, ni se duplican pesos de mancuernas/unilaterales/máquinas.

Ejemplo: **80 kg × 10 + 60 kg × 8 = 1280 kg·reps**. Añadir una serie de 12 flexiones sin kg incrementa el recuento de series y reps, no ese volumen. Sin cargas conocidas se muestra «Sin carga externa registrada», no cero esfuerzo.

Agregación por sesión, ID de ejercicio y un único músculo principal del metadato disponible; si falta, «Sin clasificar». El schema no conserva un músculo histórico independiente: no se inventa; se usa el catálogo enlazado disponible, igual que la distribución de series anterior. Nunca se reparte una serie completa entre músculos secundarios.

Se aplican los límites de ProgressPeriod 1/3/6 meses/Todo y la zona local. Semanas desde lunes 00:00 con aritmética de calendario, incluidos cambio de año y horario de verano. Las semanas sin carga conocida conservan `null`; el gráfico solo une puntos conocidos y lo explica. Puntos y controles anterior/posterior son los del Bloque 5. La comparación, cuando existe, usa las dos semanas naturales completas inmediatamente anteriores, ambas dentro del período y con carga conocida; excluye la semana actual parcial y no inventa porcentajes desde cero ni resultados positivos de entrenamiento.

Una sección en Progreso y otra contextual en detalle de ejercicio reúnen cifra, series/reps, gráfico y desglose plegable por músculo/sesión. PR y e1RM siguen sus fórmulas anteriores; sus valores absolutos se siguen etiquetando como tales. No se usa esta métrica como medida universal de esfuerzo, trabajo muscular o hipertrofia.

## Pruebas, evidencias y pendientes

[Validación y capturas reales](design/validation-block-9-10/README.md). [Backlog maestro revisado](product/BACKLOG.md). El histórico de alcance de Stitch se conserva, no se trata como una lista automática de requisitos nuevos.
