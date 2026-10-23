# Bloque 7 — Catálogo 2.0

Esta página documenta la entrega original del Bloque 7. La **cobertura actual, 45/45 guías ES/EN y 9 ejercicios ilustrados**, está en [Bloque 8 y matriz actualizada](block-8-catalog.md); las cifras inferiores son históricas del Bloque 7.

Base: `ad8c7d54a991f61db130ed38310ee3b1017d5eb4`. Se mantiene el catálogo de 45 ejercicios y todos sus IDs. No cambia ExerciseEntity, Room v2, Routine JSON v1, Backup JSON v1, motor, historial ni snapshots.

## Búsqueda y presentación

Catálogo y todos los selectores usan la misma lista local existente, con búsqueda ES/EN, músculo principal y equipamiento combinables mediante AND. El filtrado en memoria ignora mayúsculas y acentos. Las opciones salen de los datos actuales, incluidos personalizados; no hay taxonomía nueva impuesta al usuario, consultas nuevas ni llamadas de red. Limpiar filtros restaura los tres criterios. Los archivados no aparecen en selección, pero su guía sigue resolviéndose mediante la lectura por ID existente.

Se reutilizan ScreenHeader, AreteField, EditorialRow, SecondaryAction, SectionHeading y los temas A+B. Las miniaturas son de 48 dp y decorativas junto al nombre; las posiciones ampliadas tienen descripción accesible. La ficha y la guía contextual de sesión comparten la misma hoja desplazable, sin imágenes permanentes en la tabla de series. El selector permite previsualizar la ficha sin añadir el ejercicio. No hay reproductor, descargas en la app ni permiso de Internet nuevo.

## Manifest editorial

`app/src/main/assets/exercise_editorial.json` es un recurso de presentación versionado, no una entidad ni datos personales. Su lector está en `catalog/ExerciseEditorial.kt`.

- `version`: formato editorial local, actualmente 1; independiente de los formatos de importación/backup.
- `reviewedAt`: fecha de revisión del contenido, no certificación profesional de Arete.
- `sources[]`: ID, título, URL HTTP(S) y clase `general-evidence` o `practical-technique`.
- `exercises[]`: `exerciseId` estable, `sourceIds`, textos por idioma y lista opcional `illustrations`.
- Cada texto contiene `setup`, `steps`, `cues`, `mistakes`, `muscles`. Los diez enriquecidos tienen ES y EN; el resolver vuelve explícitamente a ES si falta el idioma pedido.
- Cada ilustración incluye una clave local autorizada en `ExerciseArtwork.kt`, alt y pie ES/EN, URL original, autor, licencia, enlace y cambios. No se resuelven URLs como imágenes.

El contenido se lee en IO, fuera de Room. Una ficha desconocida, básica, personalizada o sin manifest válido conserva sus instrucciones existentes y un estado sin ilustración. No se asocia contenido por nombre: `isCustom` siempre impide reemplazarlo, incluso con un ID coincidente. Una imagen ausente no impide consultar texto ni registrar series. Los recursos editoriales no se serializan en backups; un equipo receptor usa su propia versión instalada de Arete.

## Evidencia y técnica

Los resúmenes ES/EN son redacción original de Arete, breve y con referencias enlazadas por ejercicio. No se copian fotografías ni texto completo de las fuentes de técnica. La ficha distingue expresamente:

- **Evidencia general:** Currier et al., posición ACSM de 2026, *Resistance Training Prescription for Muscle Function, Hypertrophy, and Physical Performance in Healthy Adults: An Overview of Reviews*, DOI [10.1249/MSS.0000000000003897](https://doi.org/10.1249/MSS.0000000000003897), Med Sci Sports Exerc 58(4), 851–872, [registro bibliográfico](https://pubmed.ncbi.nlm.nih.gov/41843416/). Trata prescripción del entrenamiento en adultos sanos; no valida estas ilustraciones ni cada cue concreto.
- **Orientación práctica:** CSCCa para banca; NASM para press inclinado, dominadas y prensa; ACE para jalón (protocolo publicado en 2018), remo, sentadilla, rumano y elevaciones; Catalyst Athletics para press de pie. Los enlaces exactos están en el manifest y son accesibles desde la ficha.

Se evitan prescripciones universales de carga/recorrido, beneficios clínicos, inferencias de hipertrofia a partir de un ranking EMG y promesas de prevención de lesiones. La app no sustituye evaluación o supervisión profesional.

## Multimedia y decisión de licencias

Se evaluó [RepDB](https://github.com/RepDB/exercise-dataset) y su [LICENSE-DATA.md](https://github.com/RepDB/exercise-dataset/blob/main/LICENSE-DATA.md), versión de licencia 1.0, `main` observado el 23-10-2026 en `b407c5951a027a27817fb2a5cee9ec9feffc4dc7`. Permite uso dentro de aplicaciones con atribución, pero prohíbe redistribuir como dataset/repositorio/API. No explicita el caso de assets extraíbles dentro del código fuente público de otra app. **No se presume ese permiso:** no se incorporan imágenes, JSON, instrucciones, premium-samples ni animaciones RepDB. Haría falta aclaración expresa antes de incorporarlos al repo público. La lectura de sus metadatos para evaluar variantes no se convierte en un dataset redistribuido.

Alternativa incorporada: seis ilustraciones de **Everkinetic**, distribuidas por **wger**, con licencia por archivo **CC BY-SA 3.0**. Esta licencia permite copia y redistribución con atribución y compartir las adaptaciones bajo la misma licencia, también en repositorios públicos. Los WebP conservan esa licencia, no quedan relicenciados como el código. No se supone aval de Everkinetic/wger.

[Registro de assets y derechos](licenses/exercise-art.md). No se descargó una copia completa de ninguna biblioteca. No hay transformaciones generativas, recorte, recoloración ni eliminación de fondo: solo reducción convencional conservando proporción y conversión WebP sin pérdida. Los seis archivos únicos están en `drawable-nodpi`, sin duplicados por densidad. Se conservan las dos posiciones originales; no son una animación ni una garantía de técnica individual.

### Evaluación del matching

Los diez candidatos RepDB se cotejaron por ID, nombre y equipo, no solo parecido de texto:

| ID Arete (prefijo `arete-`) | Candidato RepDB | Variante/equipo esperado | Resultado incorporado |
| --- | --- | --- | --- |
| barbell-bench-press | bench-press | Banco plano, barra | wger 73, imágenes 68/70, verificación visual |
| incline-dumbbell-press | incline-db-press | Banco inclinado, dos mancuernas | wger 537, imágenes 105/106, verificación visual; se descarta wger 538 con barra |
| pull-up | pull-up | Dominada prona en barra fija, sin asistencia | Texto; sin imagen incorporada |
| lat-pulldown | lat-pulldown | Jalón al pecho, polea y barra | Texto; sin imagen incorporada |
| barbell-row | barbell-row | Remo inclinado con barra, no remo alto ni agarre inverso | Texto; wger 83 tiene archivo denominado rear-delt-row y no se utiliza; tampoco wger 84 inverso |
| back-squat | squat | Sentadilla trasera, no frontal/Smith | Texto; sin imagen incorporada |
| leg-press | leg-press | Prensa en máquina; el catálogo no fija modelo/ángulo | Texto; no se atribuye una máquina específica al ID genérico |
| romanian-deadlift | romanian-deadlift | Bisagra rumana con barra, no convencional | Texto; alternativa wger 507 sin autor explícito en el registro evaluado, no incorporada |
| overhead-press | ohp | Barra, de pie y sin impulso; no seated/paused | Texto; no se usa ilustración sentada o con mancuernas |
| lateral-raise | lateral-raise | Elevación bilateral de pie con mancuernas | wger 348, imágenes 177/178, verificación visual; no polea/máquina |

La coincidencia de metadatos de RepDB no se presenta como aprobación visual de assets no incorporados. Solo los tres pares wger incorporados se revisaron visualmente; el resto conserva fallback explícito.

## Cobertura de los 45 ejercicios

**10 ampliados ES+EN; 3 con imagen (6 posiciones); 35 básicos conservados.** “Básica” conserva instrucciones y clasificación existentes, no significa que el registro del ejercicio esté bloqueado.

| ID estable | Guía | Imágenes locales |
| --- | --- | --- |
| arete-barbell-bench-press | Ampliada ES/EN | 2, Everkinetic/wger |
| arete-incline-dumbbell-press | Ampliada ES/EN | 2, Everkinetic/wger |
| arete-chest-dip | Básica | No |
| arete-cable-fly | Básica | No |
| arete-push-up | Básica | No |
| arete-pull-up | Ampliada ES/EN | Pendiente |
| arete-lat-pulldown | Ampliada ES/EN | Pendiente |
| arete-barbell-row | Ampliada ES/EN | Pendiente |
| arete-seated-cable-row | Básica | No |
| arete-one-arm-dumbbell-row | Básica | No |
| arete-chest-supported-row | Básica | No |
| arete-straight-arm-pulldown | Básica | No |
| arete-back-squat | Ampliada ES/EN | Pendiente |
| arete-front-squat | Básica | No |
| arete-leg-press | Ampliada ES/EN | Pendiente |
| arete-bulgarian-split-squat | Básica | No |
| arete-leg-extension | Básica | No |
| arete-walking-lunge | Básica | No |
| arete-conventional-deadlift | Básica | No |
| arete-romanian-deadlift | Ampliada ES/EN | Pendiente |
| arete-lying-leg-curl | Básica | No |
| arete-seated-leg-curl | Básica | No |
| arete-hip-thrust | Básica | No |
| arete-cable-pull-through | Básica | No |
| arete-overhead-press | Ampliada ES/EN | Pendiente |
| arete-seated-dumbbell-press | Básica | No |
| arete-lateral-raise | Ampliada ES/EN | 2, Everkinetic/wger |
| arete-cable-lateral-raise | Básica | No |
| arete-reverse-fly | Básica | No |
| arete-face-pull | Básica | No |
| arete-barbell-curl | Básica | No |
| arete-incline-dumbbell-curl | Básica | No |
| arete-hammer-curl | Básica | No |
| arete-preacher-curl | Básica | No |
| arete-close-grip-bench | Básica | No |
| arete-triceps-pushdown | Básica | No |
| arete-overhead-triceps-extension | Básica | No |
| arete-skull-crusher | Básica | No |
| arete-standing-calf-raise | Básica | No |
| arete-seated-calf-raise | Básica | No |
| arete-plank | Básica | No |
| arete-hanging-leg-raise | Básica | No |
| arete-cable-crunch | Básica | No |
| arete-ab-wheel | Básica | No |
| arete-farmers-carry | Básica | No |

Los personalizados quedan fuera de esta matriz editorial: conservan sus nombres, instrucciones y clasificaciones, y participan en los filtros.

## Validación

[Pruebas, tamaño del APK y capturas reales](design/validation-block-7/README.md). No se añade una suite Compose extensa ni se introduce nueva lógica de entrenamientos.
