# Bloque 3: progreso y consistencia

## Fuente de verdad

Room sigue siendo la única fuente de verdad. `ProgressDao` obtiene en una consulta conjunta las sesiones completadas, ejercicios, series y músculo principal. `ProgressRepository` deriva de ese flujo el progreso por ejercicio, PR, estadísticas, actividad y sugerencias. Por tanto, editar o eliminar una serie o sesión recalcula todos los resultados sin tablas auxiliares ni N+1.

`BodyWeightEntry` ya existía en el schema v2 y ahora tiene CRUD completo, histórico, gráfica, cambio reciente y una media móvil de hasta siete registros. El objetivo semanal se almacena en DataStore. El Bloque 3 no cambia el schema Room ni requiere migración; Routine JSON permanece en v1.

## Reglas explícitas

- **e1RM:** fórmula de Epley, `peso × (1 + repeticiones / 30)`. Se presenta siempre como estimación. Solo usa series de trabajo completadas, peso positivo finito y entre 1 y 12 repeticiones.
- **PR:** se deriva cronológicamente del histórico real. Se detecta mayor peso, mayor e1RM estimado y mayor número de repeticiones para un peso exacto (normalizado a centésimas de kg). Calentamientos e incompletas quedan fuera.
- **Series efectivas:** series de trabajo completadas con al menos una repetición. La distribución muscular usa únicamente el músculo principal del catálogo.
- **Duración fiable:** solo sesiones entre 1 minuto y 6 horas entran en la media.
- **Semana y racha:** semana local de lunes 00:00 a domingo 23:59. La racha cuenta semanas consecutivas que alcanzan el objetivo. Si la semana actual aún no lo alcanza, se empieza a contar desde la anterior para no penalizarla antes de terminar.
- **Sugerencia de progresión:** se propone exactamente el `incrementKg` configurado cuando, en la ejecución más reciente, todas las series de trabajo prescritas están completadas al mismo peso, alcanzan el extremo superior del rango, no son al fallo y cualquier RIR registrado es al menos 1. La sugerencia nunca edita la rutina ni el peso automáticamente.
- **Sustitución segura:** un ejercicio de sesión solo puede reasignarse si no tiene ninguna serie completada. Si la tiene, la UI permite añadir el sustituto como ejercicio nuevo y preserva la identidad del histórico.

## Interfaz

Home mantiene el CTA de entrenamiento como elemento principal y añade un resumen compacto de objetivo, racha y PR reciente. El destino Progreso conduce al detalle por ejercicio y al peso corporal, y muestra una sola actividad semanal compacta en lugar de acumular dashboards. El detalle del ejercicio prioriza la serie temporal de e1RM y conserva peso/repeticiones en el historial.

## Fuera de alcance mantenido

Siguen en backlog los P2 conocidos: notificación del temporizador si Android termina completamente el proceso y matching avanzado de precarga cuando cambia mucho la estructura de series. No se añadieron calendario, recordatorios, Health Connect, backup, coaching automático ni recomendaciones adaptativas.
