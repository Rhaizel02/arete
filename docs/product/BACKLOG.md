# Backlog maestro de Arete

Revisado para el macrobloque 9–10 sobre `f23da09`. Este archivo reúne pendientes reales; [STITCH_BACKLOG.md](STITCH_BACKLOG.md) conserva el histórico de alcance, no se copia como requisitos nuevos.

## P0 / P1

Sin incidencias abiertas detectadas en las pruebas y muestra de validación de este macrobloque. No equivale a una auditoría exhaustiva ni sustituye pruebas en móviles reales.

## Resueltos en 9–10

- **P2 — Precarga ante cambios de estructura** (bloques 2/5–8): matching por tipo y orden relativo, aparición histórica separada, fallback conservador y protección de snapshots. [Decisiones y pruebas](../block-9-10-training-progress.md).
- **P2 — Aviso al morir normalmente el proceso durante un descanso** (bloques 2/5–8): AlarmManager/receiver con deadline Room y consumo idempotente. Resuelto dentro de las garantías Android descritas; Forzar detención y restricciones del fabricante no se prometen resueltos.
- Persistencia y claves del borrador de prescripciones; rechazo transaccional de sobrescrituras concurrentes. Ergonomía incremental de registro, sin reemplazar autosave.

## P2 y validaciones pendientes

- Dispositivos físicos: batería/Doze y cuotas de alarmas repetidas, reinicios/desbloqueo, revocación del acceso especial, OEM y versiones Android 24/31–34 además de la muestra API 35. TalkBack y uso prolongado en gimnasio.
- Las notificaciones dependen de permisos/canales y del sistema. Revocar exactas puede terminar el proceso y cancelar alarmas hasta la próxima ejecución/reconciliación. La ventana entre consumir el deadline y publicar feedback puede perder un aviso si hay una terminación abrupta; no se promete exactamente una entrega.
- Los borradores salvan recreación con estado Android, no son documentos persistidos. Salir del editor descarta deliberadamente lo no guardado; un cambio concurrente requiere reabrir, no hace merge automático.
- Duración: cadencia y preparación son supuestos generales, no modelo individual. Los grupos no definen una temporización completa y se estiman conservadoramente.
- Volumen: solo carga externa registrada, no esfuerzo muscular universal. El músculo usa metadato disponible porque Room v2 no conserva otro snapshot muscular. Semanas parciales o sin carga conocida no se comparan como completas.

## Cobertura gráfica

- **45/45 guías ES/EN, 9/45 ejercicios ilustrados, 18 posiciones; 36 pendientes.** [Matriz individual del Bloque 8](../block-8-catalog.md).
- Cuatro prioritarios siguen sin imagen inequívoca: dominadas, jalón al pecho, remo con barra y militar de pie. No sustituir por tras nuca, remo alto/inverso o press sentado. Conservar fallback hasta verificar variante y licencia individual.
- RepDB: no adoptar assets en el repositorio público sin aclarar explícitamente esa redistribución. No es autorización para una nueva investigación multimedia dentro de 9–10.

## Backup y proveedores SAF

- Mantener pruebas en proveedores físicos/cloud además del proveedor local del AVD. Permisos persistentes y sincronización remota dependen del proveedor; Arete no garantiza su disponibilidad.
- Room y DataStore compensan fallos ordinarios del restore, pero no tienen atomicidad distribuida frente a terminación abrupta. [Arquitectura del Bloque 6](../block-6-backup.md).
- Retención solo de archivos automáticos registrados/verificados. Tras reinstalar, archivos antiguos o modificados no se adoptan para borrado automático. Mayor número de archivos es preferible a una eliminación incierta.
- Android Auto Backup sigue siendo complementario y limitado; no sustituye una copia JSON externa verificable.

## Futuro / aplazado, no aprobado por este bloque

- Health Connect, Wear OS, backend, cuentas, sincronización entre dispositivos, coaching/progresión automática y nutrición: fuera de alcance, requieren decisión de producto propia.
- Vídeos/GIF, descargas dinámicas, nuevas ilustraciones y ejercicios: no incluidos en el macrobloque.
- Mayor interacción con gráficas/gestos y otros filtros solo si aportan utilidad comprobada; no duplicar lo ya disponible en el Bloque 5.
- No calcular calentamientos con porcentajes arbitrarios, equivalencias de peso corporal/máquinas o puntuaciones de entrenamiento a partir de volumen. Ideas descartadas para preservar claridad e integridad.
