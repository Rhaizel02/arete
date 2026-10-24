# Validación del macrobloque 9–10

Base: `f23da092285633279fd677e69a0f24dd0289e905`. AVD aislado API 35.

Las capturas de esta carpeta son PNG reales de Android, sin retoque. Los datos QA son opt-in de `androidTest`, nunca datos personales ni un modo demo de producción.

## Resultado automatizado

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --no-daemon
```

**99 pruebas unitarias/Robolectric, cero fallos y errores. Ambos APK compilados. Lint: cero errores, 30 avisos existentes y una recomendación existente.** No hay supresiones nuevas ni actualizaciones de dependencias. Los avisos siguen siendo versiones de herramientas/dependencias, iconos y sugerencias KTX del código anterior.

Se añaden 38 pruebas focalizadas a las 61 anteriores: matching puro y apariciones incompletas/repetidas, incluido reordenar una identidad de plantilla sin filas completadas; integración Room con instantáneas, inicio concurrente y sustitución; temporizador con permisos/programación/consumo obsoleto/idempotencia/restore lento; Saver de filas; estimación; fórmula/exclusiones/agregaciones/períodos/semanas/DST de volumen; recálculo por edición, borrado y restore v1. No se añade una suite Compose extensa.

Los nuevos tests de coordinador usan preferencias en memoria inyectadas, para no compartir archivos DataStore entre sandboxes Robolectric. La integración conserva Room real bajo Robolectric. Siguen pasando los tests anteriores de Backup JSON v1 antiguo, compensación de preferencias, copias automáticas, Routine JSON, PR, e1RM, catálogo y snapshots. Se verifica además que el nuevo opt-in de avisos no se exporta/importa por JSON.

En Android real del AVD se ejecutan **dos pruebas instrumentales**:

```powershell
adb shell am instrument -w -e class com.arete.app.data.TrainingEngineDeviceTest,com.arete.app.data.DatabaseMigrationTest com.arete.app.test/androidx.test.runner.AndroidJUnitRunner
```

Resultado: **OK (2 tests)**, repetido con el APK final. La nueva prueba utiliza SQLite Android/Room en memoria y comprueba que un calentamiento no se precarga como trabajo, se excluye una serie incompleta, las series extra reutilizan la referencia correcta y el volumen excluye la sesión activa. La otra prueba valida la migración existente. Los controles opt-in del fixture se cuentan aparte, no inflan esas cifras.

## AVD, datos y reproducción

`Arete_API_35`, Android 15/API 35, 1080×2400, densidad 420. El ejecutable headless del emulador 37.1.11 se cerraba antes de arrancar Arete. Se pudo validar usando su backend Qt oculto, GPU host y Vulkan desactivado; no se cambian paquetes del proyecto para resolver un fallo del host.

```powershell
emulator -avd Arete_API_35 -qt-hide-window -no-audio -no-snapshot -gpu host -feature -Vulkan
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# SOLO AVD desechable: seed reemplaza los datos fitness y desactiva el backup automático.
adb shell am instrument -w -e class com.arete.app.data.TrainingFixtureTest -e trainingFixture seed com.arete.app.test/androidx.test.runner.AndroidJUnitRunner
```

El fixture crea 45 ejercicios existentes, una rutina QA con banca y dominadas, dos sesiones históricas finalizadas y prescripciones conocidas. Las sesiones contienen calentamientos, series completadas y una incompleta con carga que nunca debe usarse. No añade ejercicios a producción. `check-plan` y `check-edited` confirmaron desde Room notas/campos/tipos tras rotación y recuperación. Los modos `arm`, `expired`, `notification`, `no-notification`, `light` y `dark` sirven solo para este AVD; no hay un endpoint/broadcast de depuración en la app de producción.

**No ejecutar instrumentación mientras se espera una alarma:** Android fuerza la parada del paquete al iniciar/finalizar instrumentation. Un primer intento de control interfirió con la espera; se repitió mediante UI y `am kill`, sin instrumentation durante el descanso. Los resultados siguientes corresponden a las repeticiones válidas.

## Flujo de entrenamiento y edición

1. Editar Torso QA: convertir el primer calentamiento en trabajo, ajustar su rango a 6–8, añadir un calentamiento y reordenarlo entre series de trabajo; descanso 20 s y nota «QA draft preserved».
2. Rotar con cambios sin guardar. Guardar explícitamente. `check-plan`: tipos **trabajo/trabajo/trabajo/calentamiento/trabajo**, rango inicial 6–8, nota y descanso correctos.
3. Iniciar desde rutina: precarga visual **70 / 72,5 / 72,5 / 20 / 72,5 kg**, reps **9 / 8 / 8 / 5 / 8**. El calentamiento no desplaza el trabajo y el objetivo 6–8 no se reemplaza por el rendimiento previo.
4. Escribir **81,5 kg**, 10 reps, RIR 1; marcar/desmarcar/completar y marcar fallo en otra serie. Teclado decimal con coma, foco y acciones visibles. Ajustar +15/−15 y saltar. Reinstalar/recuperar: `check-edited` confirma exactamente los campos editados; no se vuelve a precargar.
5. Añadir una serie en una sesión libre de prueba, marcarla calentamiento, subirla/bajarla y borrarla. No se pierde su identidad ni se completa otra fila por posición. La sesión temporal se elimina con confirmación.
6. Notas de sesión con autosave y navegación entre ejercicios. Tras dos reinicios Android se conservan las series, progreso y tipos; la selección inicial encuentra el ejercicio con trabajo pendiente.
7. Completar una serie de dominadas **sin kg**, finalizar con descanso todavía pendiente y verificar ausencia de aviso posterior. No se modifica automáticamente la rutina ni se presenta la duración estimada como duración histórica.
8. Crear otro workout QA con remo: estimación **5–9 min** para sus tres series iniciales; tras cambiar descanso a 300 s, **11–15 min**. Editar con fuente **140 %** y horizontal, guardar tras rotar y recuperar orientación/fuente normales.

## Notificaciones Android — qué se comprobó

| Caso | Evidencia real y resultado |
| --- | --- |
| Pantalla de sesión visible | Cuenta atrás expira y desaparece de Room sin notificación de sistema duplicada. Vibración desactivada para el fixture; no se afirma una prueba física del motor háptico. |
| Segundo plano / proceso terminado normalmente | Se completa una serie, se envía Home y se usa `am kill`, no force-stop. PID desaparece; Android inicia otro proceso para RestAlarmReceiver y aparece el aviso. Repetido con el APK final. |
| Pulsar el aviso | Abre la sesión existente y conserva los registros; no crea otro entrenamiento. |
| Pantalla bloqueada | PIN temporal del AVD, pantalla apagada y notificación visible al despertar en keyguard. Se desbloquea y elimina ese PIN al terminar. |
| POST_NOTIFICATIONS denegado | Estado explicativo en Ajustes y entrenamiento operativo; en segundo plano el deadline se consume sin publicar notificación (`expired` y `no-notification` pasan). Posteriormente se concede mediante diálogo real. |
| Acceso exacto denegado / revocado | Acceso especial real desactivado. La alarma registrada tiene ventana inexacta (muestra de 44,964 s), no ventana cero. Se comprueba el fallback y el contador, no se promete entrega puntual inexacta. |
| Acceso exacto concedido | Concesión en ajustes Android y alarma con `window=0`, `exactAllowReason=permission`. Sin USE_EXACT_ALARM. |
| Reinicio con descanso futuro | Antes de abrir Arete, el log muestra arranque de RestRescheduleReceiver y una alarma futura reprogramada. Después, el contador y la sesión se recuperan. |
| Ajuste / salto / finalización / borrado | +15/−15 reemplazan el deadline; salto elimina la cuenta atrás. Finalizar/cancelar no deja aviso posterior ni una sesión activa de prueba. Los eventos obsoletos y restore se prueban adicionalmente con Room en Robolectric. |

Extracto observado en el APK final (hora del AVD):

```text
09:41:39.274 Killing 7450:com.arete.app ... kill background
09:41:39.284 Got obituary of 7450:com.arete.app
09:41:52.927 Start proc 7490:com.arete.app ... RestAlarmReceiver
```

Y en la prueba de reinicio, antes de abrir la actividad:

```text
09:21:16.439 Start proc 2906:com.arete.app ... RestRescheduleReceiver
Rest alarm recovered before opening Arete
window=0 exactAllowReason=permission
```

La instrumentación solo se ejecutó después de entregar/cancelar el descanso para comprobar estados, o antes de esperar para preparar un deadline de prueba. La prueba final de proceso muerto usa explícitamente un PID vivo, su desaparición y posterior arranque por broadcast. Ninguno de estos resultados promete entrega tras Forzar detención, cuotas Doze o políticas OEM.

## Volumen, filtros y recálculo

- Histórico inicial: **2250 kg·reps**. Con el workout QA finalizado: **4805 kg·reps**, **13 series efectivas**, **111 reps**, **8 series con carga conocida**. Las dominadas cuentan como cinco series / 43 reps de espalda sin inventar kg.
- Editar una serie de banca histórica, **81,5 → 90 kg × 10 reps**: cambio esperado +85, resultado observado **4890 kg·reps**. Room/fixture y pantalla coinciden; series/reps no cambian. Sesión nueva: 2640; anteriores: 1210 y 1040.
- Un cambio transitorio de kg en una serie de calentamiento incompleta no altera el volumen; se restablece su carga vacía. Edición relevante de trabajo, borrado y restore se cubren también con la integración Room automatizada.
- Desglose por sesión/músculo y detalle por ejercicio; pecho con carga conocida y espalda «Sin carga conocida», sin doble contar músculos secundarios. La ficha de dominadas conserva series/reps pese a no tener volumen externo conocido.
- Filtro 1 mes/Todo, gráfico semanal con controles de selección de punto, etiqueta del valor y fecha. Semanas sin carga conocida no se representan como ceros. La comparación de las dos semanas completas anteriores es +170 kg·reps (+16,35 %), sin incluir la semana actual parcial. Las exclusiones de fechas futuras y límites de 3/6 meses, cambio de año y DST de Madrid se comprueban automatizadamente, no se simula un cambio DST en el AVD.
- La nueva sección se compactó tras revisar el emulador: cifras principales primero, gráfico/desgloses plegados por defecto. Se conservan las cuatro métricas originales por encima y no se añade un dashboard paralelo.

## Capturas representativas

35 PNG reales. Se retiraron cinco capturas duplicadas de la iteración intermedia; no se alteraron sus píxeles ni se fabricaron pantallas.

- Precarga/recuperación: [snapshot nuevo](prefill-work-dark.png), [recuperado](session-recovered-dark.png).
- Registro: [teclado](set-numeric-keyboard-dark.png), [RIR/fallo](set-rir-failure-dark.png), [reordenación](session-series-reordered-dark.png).
- Borrador: [sin guardar](prescription-unsaved-dark.png), [rotado](prescription-rotated-dark.png), [horizontal al 140 %](prescription-landscape-large-light.png).
- Estimación: [Torso QA oscuro](duration-template-dark.png), [otro workout claro](duration-other-workout-light.png), [descanso editado](duration-edited-light.png).
- Permisos: [petición POST](notification-runtime-permission.png), [denegado](rest-alert-permission-denied-dark.png), [inexacto](rest-inexact-dark.png), [acceso especial](exact-alarm-access.png), [autorizado](rest-exact-enabled-dark.png), [revocado](exact-revoked-fallback-dark.png), [Ajustes claro](rest-settings-light.png).
- Descansos: [controles](rest-controls-dark.png), [salto](rest-skipped-dark.png), [tras matar el proceso normalmente](rest-after-normal-kill.png), [keyguard](rest-lockscreen.png), [abrir sesión existente](notification-opens-existing-session-dark.png), [sesión tras reboot](session-after-system-reboot.png), [finalizar antes del aviso](finish-before-alarm-confirmation.png).
- Progreso: [dashboard oscuro](volume-dashboard-dark.png), [claro](volume-dashboard-light.png), [período](volume-period-light.png), [tendencia clara](volume-trend-light.png), [punto seleccionado](volume-week-selected-dark.png), [desglose](volume-breakdown-dark.png), [detalle por ejercicio](exercise-volume-dark.png), [sin carga externa](bodyweight-volume-light.png), [edición histórica](historical-edit-dark.png), [recalculado](volume-recalculated-dark.png).

## Límites y cierre

Sin P0/P1 abiertos detectados en esta muestra. [P2 y pendientes revisados](../../product/BACKLOG.md). No se valida en móvil físico, OEM ni todos los niveles API; no se afirma una prueba de TalkBack, intensidad háptica física o entrega garantizada en Doze profundo. El restore v1 y su recálculo están probados con Room bajo Robolectric; no se repite aquí toda la recuperación manual SAF del Bloque 6.

No hay cambios en schemas, entidades, migraciones, contratos JSON, catálogo ni manifiesto editorial. Se restablecen fuente/orientación y se retira el PIN temporal. Se cierra el AVD desechable al terminar; no se borra ni modifica ningún dato personal.
