# Bloque 8 — Catálogo 2.0 completo en texto

Revisión final: **24-10-2026** (investigación iniciada el 23). Base `24c4f927bdb8097ae194576ee9cd8ab4869b4c2f`.

## Resultado y compatibilidad

**45/45 guías ampliadas ES/EN. 9/45 ejercicios ilustrados, con 18 posiciones. 36/45 pendientes de ilustración.** No se declara cobertura gráfica completa.

Se añaden los 35 textos que faltaban al manifest v1 del Bloque 7. Las diez guías anteriores conservan exactamente sus textos y referencias individuales; únicamente se añaden imágenes verificadas a sentadilla, prensa y rumano. `reviewedAt` refleja esta revisión. No cambia el contrato editorial ni se crea otro repositorio de contenido.

Se conservan los 45 IDs y `exercises_catalog.json`, ExerciseEntity, Room v2, Routine JSON v1, Backup JSON v1, entrenamiento, progreso y snapshots. No hay nuevas dependencias, permisos, APIs de ejecución ni descargas en la app. El contenido editorial permanece en el APK, fuera de los datos personales exportados.

## Contenido y verificación de fuentes

Cada guía incluye preparación, dos o más pasos, indicaciones, errores y músculos en ambos idiomas. La redacción es un resumen original de Arete; no se copian textos completos ni imágenes de las bibliotecas de técnica. Se distinguen, por ejemplo, curl con barra/EZ/martillo, extensión tumbada/sobre la cabeza/en polea y press de pie/sentado. Los datos personalizados no se traducen ni sustituyen.

Se consultaron las páginas originales de **ACE**, **NASM**, **CSCCa**, **Catalyst Athletics** y **StrengthLog**. Esta última es una biblioteca editorial de técnica, no se presenta como posición científica. Se revisaron los pasos específicos antes de redactar; no se adoptan sus promesas comerciales de prevención de lesiones, rankings de hipertrofia ni reglas universales de recorrido.

La posición **ACSM 2026**, DOI [10.1249/MSS.0000000000003897](https://doi.org/10.1249/MSS.0000000000003897), sigue siendo evidencia general sobre prescripción en adultos sanos, no aval de cada cue o dibujo. Los 45 ejercicios remiten además a una fuente práctica específica. Los 46 enlaces de `sources` se consultaron por HTTP: **todos devolvieron 200**; los títulos H1 de los 35 nuevos corresponden al ejercicio, no a una página genérica de biblioteca. CSCCa, Catalyst y el PDF de ACE no usan el mismo H1; se revisó su contenido específico. No se añaden chequeos web a los tests offline.

Las URLs y títulos exactos están en [`exercise_editorial.json`](../app/src/main/assets/exercise_editorial.json) y son consultables desde la ficha. La matriz inferior identifica el `sourceId` práctico de cada ejercicio. Las fuentes pueden cambiar posteriormente; no se garantiza disponibilidad de sus páginas sin conexión. La guía local no depende de ellas en ejecución.

## UX conservando las pantallas

- Preparación, ejecución e indicaciones aparecen primero. Las ilustraciones están en un desplegable después de esos bloques; no ocupan espacio permanente de registro en sesión.
- Fuentes y créditos tienen otro desplegable al final. Al desplegar imágenes aparece inmediatamente la atribución corta con licencia; los originales, cambios y enlaces legales siguen en los créditos completos.
- El pie genérico de CC BY-SA 3.0 se sustituye por una indicación por archivo, para no atribuir una licencia incorrecta a los nuevos recursos 4.0.
- «Sin conexión» se reemplaza por **«Guías locales»**: indica dónde reside el contenido, no un estado de conectividad detectado.
- Búsqueda/músculo/equipo pertenecen a cada instancia de catálogo o selector con `rememberSaveable(title)`, no a un filtro global del ViewModel. La rotación conserva los criterios de la pantalla actual. Al salir y abrir otro destino no se hereda una búsqueda que pueda ocultar ejercicios inesperadamente. La ficha/modal dentro de la pantalla no borra sus filtros.
- Se mantienen miniaturas discretas, lista con separadores, scroll, teclado y temas del Bloque 7. No hay nuevas pantallas maestras ni nuevas jerarquías multimedia.

## Investigación gráfica prioritaria

Se revisó primero el material del autor Everkinetic y las entradas wger. El repositorio **everkinetic/data**, revisión `446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6`, contiene los originales usados, y aplica [CC BY-SA 4.0](https://github.com/everkinetic/data/blob/446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6/LICENSE.md) al proyecto. No hay excepción de licencia indicada en las rutas de los diez archivos incorporados; se conserva el nombre Everkinetic y el crédito al proyecto original de Greg Priday. Los hashes fijan la versión de cada imagen. No se infiere que los archivos nuevos tengan la licencia 3.0 de wger: son 4.0 de su fuente directa.

| Prioritario | Evaluación concreta | Decisión |
| --- | --- | --- |
| Dominadas | `dist/md/pull-ups.md` / `src/images-web/pull-ups-{1,2}.png`. La pose elevada muestra la barra en la zona posterior del cuello desde la vista trasera; no es suficientemente inequívoca para una dominada al frente. La entrada wger 475 atribuye a Imobard una composición visual de esas poses sin aclarar allí el origen del dibujo. | No se integra el par ni se recorta solo una pose para simular cobertura. Fallback de texto. |
| Jalón al pecho | `wide-grip-lat-pull-down-{1,2}.png`: la pose baja es **tras nuca**, aunque el texto asociado menciona pecho. | Rechazado por discrepancia visual. |
| Remo con barra | `bent-over-row-with-barbell.md` no tiene imágenes asociadas. wger 83 usa dibujos denominados `Barbell-rear-delt-row`; wger 84 es agarre inverso. | No se sustituye por remo alto ni inverso. Pendiente. |
| Sentadilla trasera | `barbell-squat-{1,2}.png`: barra en espalda alta, pies apoyados, poses erguida/flexionada. | Dos posiciones incorporadas; no frontal ni Smith. |
| Prensa | `leg-press-{1,2}.png`: prensa inclinada con plataforma, pelvis/espalda apoyadas, flexión/extensión de piernas. | Dos posiciones, pies de imagen **«Ejemplo: prensa inclinada»**. No se afirma que todas las máquinas del ID genérico tengan ese ángulo. |
| Rumano | `romanian-dead-lift-{1,2}.png`: barra delante de muslos y bisagra con barra próxima y flexión suave de rodillas. | Dos posiciones incorporadas. El texto de Arete no obliga a tocar el suelo. |
| Militar de pie con barra | `barbell-shoulder-press.md` identifica explícitamente el movimiento como **sentado**; las imágenes de press sentado de wger tampoco son equivalentes. No se encontró un par de pie inequívoco en la revisión acotada. | Pendiente; no se reutiliza el sentado. |

No se incorpora RepDB, material premium ni un dataset completo. Se consultó el árbol/los metadatos y solo se descargaron candidatos concretos; los descartados quedan en `app/build` ignorado, no se publican ni empaquetan.

Tras los prioritarios se incorporan tres oportunidades claras: **curl femoral tumbado, curl femoral sentado y curl martillo**. Las poses y el equipo se revisaron individualmente. Se descartó para el press de hombros sentado un par sin respaldo que no correspondía al apoyo indicado en la ficha actual. La búsqueda no se amplía indefinidamente para forzar imágenes para todos los ejercicios.

Todas las incorporaciones son originales en blanco/negro compatibles con el lenguaje visual existente. Sin recorte, recoloración o transformación generativa: únicamente escala proporcional y WebP sin pérdida, una copia `drawable-nodpi` por posición. [Registro por archivo, hashes y licencia](licenses/exercise-art.md).

## Matriz actual de los 45 ejercicios

Todos tienen guía ampliada **ES/EN**. `sourceId` identifica la referencia de técnica del manifest; todos conservan también `acsm-2026` como evidencia general. Los nombres abreviados de la columna gráfica remiten a los créditos del archivo, no cambian los IDs.

| ID | sourceId de técnica | Ilustraciones |
| --- | --- | --- |
| arete-barbell-bench-press | bench-cscca | 2 · Bloque 7, Everkinetic/wger 3.0 |
| arete-incline-dumbbell-press | incline-nasm | 2 · Bloque 7, Everkinetic/wger 3.0 |
| arete-chest-dip | tech-chest-dip | Pendiente |
| arete-cable-fly | tech-cable-fly | Pendiente |
| arete-push-up | tech-push-up | Pendiente |
| arete-pull-up | pullup-nasm | Pendiente · candidato rechazado |
| arete-lat-pulldown | pulldown-ace | Pendiente · candidato tras nuca |
| arete-barbell-row | row-ace | Pendiente · variantes distintas |
| arete-seated-cable-row | tech-seated-cable-row | Pendiente |
| arete-one-arm-dumbbell-row | tech-one-arm-dumbbell-row | Pendiente |
| arete-chest-supported-row | tech-chest-supported-row | Pendiente |
| arete-straight-arm-pulldown | tech-straight-arm-pulldown | Pendiente |
| arete-back-squat | squat-ace | 2 · Nuevo, Everkinetic 4.0 |
| arete-front-squat | tech-front-squat | Pendiente |
| arete-leg-press | legpress-nasm | 2 · Nuevo, ejemplo inclinado, Everkinetic 4.0 |
| arete-bulgarian-split-squat | tech-bulgarian-split-squat | Pendiente |
| arete-leg-extension | tech-leg-extension | Pendiente |
| arete-walking-lunge | tech-walking-lunge | Pendiente |
| arete-conventional-deadlift | tech-conventional-deadlift | Pendiente |
| arete-romanian-deadlift | rdl-ace | 2 · Nuevo, Everkinetic 4.0 |
| arete-lying-leg-curl | tech-lying-leg-curl | 2 · Nuevo, Everkinetic 4.0 |
| arete-seated-leg-curl | tech-seated-leg-curl | 2 · Nuevo, Everkinetic 4.0 |
| arete-hip-thrust | tech-hip-thrust | Pendiente |
| arete-cable-pull-through | tech-cable-pull-through | Pendiente |
| arete-overhead-press | press-catalyst | Pendiente · no sentado |
| arete-seated-dumbbell-press | tech-seated-dumbbell-press | Pendiente |
| arete-lateral-raise | raise-ace | 2 · Bloque 7, Everkinetic/wger 3.0 |
| arete-cable-lateral-raise | tech-cable-lateral-raise | Pendiente |
| arete-reverse-fly | tech-reverse-fly | Pendiente |
| arete-face-pull | tech-face-pull | Pendiente |
| arete-barbell-curl | tech-barbell-curl | Pendiente |
| arete-incline-dumbbell-curl | tech-incline-dumbbell-curl | Pendiente |
| arete-hammer-curl | tech-hammer-curl | 2 · Nuevo, Everkinetic/wger 3.0 |
| arete-preacher-curl | tech-preacher-curl | Pendiente |
| arete-close-grip-bench | tech-close-grip-bench | Pendiente |
| arete-triceps-pushdown | tech-triceps-pushdown | Pendiente |
| arete-overhead-triceps-extension | tech-overhead-triceps-extension | Pendiente |
| arete-skull-crusher | tech-skull-crusher | Pendiente |
| arete-standing-calf-raise | tech-standing-calf-raise | Pendiente |
| arete-seated-calf-raise | tech-seated-calf-raise | Pendiente |
| arete-plank | tech-plank | Pendiente |
| arete-hanging-leg-raise | tech-hanging-leg-raise | Pendiente |
| arete-cable-crunch | tech-cable-crunch | Pendiente |
| arete-ab-wheel | tech-ab-wheel | Pendiente |
| arete-farmers-carry | tech-farmers-carry | Pendiente |

## Validación y límites

[Pruebas, APK y capturas reales](design/validation-block-8/README.md). Permanecen cuatro prioritarios sin ilustración (dominadas, jalón, remo y militar), además del resto indicado. No se difiere la entrega por recursos cuya variante o procedencia no sea suficientemente clara. Los P2 previos de temporizador y matching de precarga siguen fuera de alcance.
