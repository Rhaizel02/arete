# Validación del Bloque 6

23 de octubre de 2026. Base: `745f345f14157e335e5da936c6dd292eb13b7ee7`. [Arquitectura, integridad y límites](../../block-6-backup.md).

## Pruebas automatizadas

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --no-daemon
```

Resultado final: **52 pruebas unitarias superadas**, build debug correcto y **lint sin errores** (31 avisos y una recomendación). Los avisos son de versiones de herramientas/dependencias, iconos aprobados del Bloque 5 y sugerencias KTX; no se suprimen ni se actualizan dependencias fuera de alcance.

Se mantienen las 38 pruebas anteriores y se añaden 14:

- Diez pruebas agrupadas del coordinador: round-trip v1 real, distinción manual/automática, retención, archivos ajenos con nombres similares, cambio de reloj, escritura parcial/cierre fallido, lectura distinta, modificación externa, borrado fallido, cambio de carpeta, permiso revocado/destino inexistente/error transitorio, cancelación, concurrencia, reinstalación sin adoptar archivos antiguos, URI existente, fallo del registro local y fallo de programación.
- Dos pruebas WorkManager con inicialización oficial de test, TestDriver y workers retenidos bajo control: KEEP conserva la identidad del trabajo diario y del inmediato, desactivar cancela, sin esperar 24 horas. Los workers retenidos se resuelven y cancelan al finalizar para no dejar callbacks después del cierre de Room en Robolectric.
- Dos pruebas BackupService: fixture con la forma legacy v1 y fallo inyectado de preferencias después de escribirlas, verificando rollback de entrenamientos y compensación de preferencias. Se reutilizan, sin duplicarlas, las pruebas previas de relaciones, snapshots, round-trip completo, JSON inválido y rollback por fallo de Room.

Las primeras ejecuciones de desarrollo detectaron problemas de inicialización/liberación del entorno WorkManager de pruebas y de contexto en el builder instrumental; se corrigieron antes de la ejecución final. No quedan fallos en el resultado final anterior.

## Dispositivo y datos

AVD desechable `Arete_API_35`, Android 15/API 35, 1080 × 2400. Se reutiliza el fixture opt-in A+B, con 4 ejercicios, 1 rutina, 8 sesiones —7 finalizadas y una activa—, 128 series y 7 registros de peso; tema oscuro y objetivo semanal 3. No se introducen datos demo en producción.

Se eligió mediante DocumentsUI la carpeta `Documents/Arete_Block_6_QA`. Las copias JSON externas son legibles y se conservan fuera de los datos privados de Arete. No se han probado cuentas o proveedores cloud reales.

## Comprobaciones reales

| Flujo | Evidencia y resultado |
| --- | --- |
| Configuración | Desde el switch diario sin destino se muestra el aviso de JSON sin cifrado y se abre ACTION_OPEN_DOCUMENT_TREE. Crear subcarpeta, elegir y confirmar permiso persistente, sin permisos generales de almacenamiento. |
| Primera automática | WorkManager ejecutó la primera tarea al poder hacerlo, sin esperar un día. El archivo existe y Ajustes diferencia programación de fecha/resultado verificado. |
| Copia inmediata | Crear copia ahora produjo otro JSON verificado, marcado Manual. No entra en retención. |
| Retención real | Se ejecutó nueve veces el mismo BackupWorker usando TestListenableWorkerBuilder sobre el proveedor SAF real. Se comprobaron siete archivos gestionados, sus SHA-256 y la igualdad de todo el modelo de base/preferencias respecto del export original. No se esperaron nueve días. |
| Archivos ajenos | Dos documentos de prueba con prefijo automático, pero sin registro de propiedad, permanecen intactos. También permanecen las tres copias manuales de las distintas comprobaciones. La carpeta termina con 7 automáticas gestionadas + 3 manuales + 2 ajenos. |
| Reinicio Android | Reinicio real del AVD. Tras completar el arranque se conservan carpeta, acceso persistente, último éxito y programación diaria, sin volver a seleccionar destino. |
| Acceso revocado | La prueba opt-in libera el permiso persistente y reconcilia: automático desactivado, error visible y último éxito anterior conservado. Los archivos externos no se borran. |
| Instalación limpia | Se verificó que el backup era del fixture y que el dispositivo era el emulador antes de `pm clear com.arete.app`. Se borraron únicamente los datos locales de Arete; la carpeta y la copia recuperable de Downloads permanecieron. No se utilizaron datos personales. |
| Recuperación | Seleccionar `arete-block6-recovery.json` con DocumentsUI y confirmar Reemplazar y restaurar. La prueba instrumental posterior confirma 1 rutina, 4 ejercicios, 8 sesiones, 128 series, 7 pesos, tema DARK y objetivo 3. Automático queda desactivado y sin carpeta. Se repitió la comprobación sobre el APK final. |
| Copia anterior real | Se seleccionó también `arete-block-b-backup.json`, exportado el 22 de octubre durante el Bloque B. La restauración fue correcta y reaparecieron Torso/Pierna y Fuerza B. El archivo tiene schema 1, 2 rutinas, 5 ejercicios, 8 sesiones, 128 series y 7 pesos. |
| Presentación | Ajustes en claro/oscuro, aviso previo, selector Android, confirmaciones, scroll y estado de error con último éxito visible. No se añadieron controles o notificaciones decorativos. |

Los fallos de escritura parcial, cierre, lectura alterada y actualización local se prueban con destinos simulados; **no se afirma haberlos provocado en el proveedor local de Android**. La pérdida de acceso sí se validó contra SAF real. No se ha esperado una ejecución de 24 horas ni se ha medido la sincronización remota de un proveedor.

## Reproducción controlada

**Estos comandos operan sobre datos de prueba.** El fixture reemplaza la base del dispositivo; no usar en un móvil con datos personales.

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.arete.app.data.DesignFixtureTest -e designFixture true -e theme DARK com.arete.app.test/androidx.test.runner.AndroidJUnitRunner
```

Abrir Ajustes, configurar una carpeta vacía de pruebas y activar la copia diaria. Después:

```powershell
adb shell am instrument -w -e class com.arete.app.data.BackupDeviceTest -e backupFixture retention com.arete.app.test/androidx.test.runner.AndroidJUnitRunner
adb shell am instrument -w -e class com.arete.app.data.BackupDeviceTest -e backupFixture revoke com.arete.app.test/androidx.test.runner.AndroidJUnitRunner
```

Para recuperación: conservar una de las copias automáticas fuera de los datos privados, limpiar solo la aplicación de pruebas y seleccionarla desde Restaurar backup, con confirmación. El modo `-e backupFixture restored` valida los recuentos y preferencias del fixture anterior y que no se haya recuperado la configuración automática. La prueba no restaura por sí sola un archivo. Estos modos son opt-in de `androidTest`, no acciones ocultas de producción.

## Capturas reales

Obtenidas con `adb shell screencap -p`, sin mockups ni retoque:

- [Sin configurar, oscuro](settings-unconfigured-dark.png), [aviso previo](folder-disclosure-dark.png), [carpeta configurada](settings-folder-dark.png), [copia manual verificada](settings-copy-dark.png).
- [Retención](settings-retention-dark.png), [tras reiniciar Android](settings-after-reboot-dark.png), [acceso perdido, oscuro](settings-access-revoked-dark.png) y [claro](settings-access-revoked-light.png).
- [Instalación limpia](settings-clean-install-light.png), [selector de archivos](restore-file-picker-light.png), [confirmación](restore-confirm-light.png), [Home recuperada](recovered-home-dark.png), [Ajustes recuperados](settings-recovered-dark.png) y [claro](settings-recovered-light.png).
- [Confirmación de copia del Bloque B](restore-legacy-confirm-dark.png) y [rutinas recuperadas del archivo anterior](legacy-recovered-routines-dark.png).

## Límites y pendientes

No se detectan P0/P1 en los flujos validados. Se mantienen los P2 anteriores: aviso del temporizador si Android mata el proceso y matching avanzado de precarga. Los límites específicos son explícitos: proveedor remoto no validado, sin garantía de hora exacta ni commit remoto, posible archivo parcial tras matar el proceso, limpieza conservadora que puede dejar más de siete archivos y ausencia de atomicidad distribuida Room/DataStore ante una terminación abrupta. Una reinstalación no adopta archivos anteriores para limpieza.

Se han revisado las reglas XML de Auto Backup/transferencia y su empaquetado. No se ha ejecutado una restauración cloud de Android ni una transferencia entre dos dispositivos físicos; no se presentan como garantías probadas. Los contratos JSON y schema Room permanecen en sus versiones anteriores.
