# Arete — recursos del icono aprobado

Origen: imagen aprobada por el usuario de 1536 × 1536 px. No se ha utilizado ningún servicio comercial de vectorización.

## Archivos principales

- `branding/arete-logo-vector.svg`: **reconstrucción vectorial editable** (silueta trazada y degradados aproximados). No es una representación matemáticamente idéntica al raster original.
- `branding/arete-logo-transparent-1024.png`: logomarca azul extraída de la imagen original, transparente; mantiene la textura y degradado originales.
- `branding/arete-logo-monochrome.svg`: silueta vectorial plana para usos monocromos.
- `android/app/src/main/res/...`: paquete para fusionar en el proyecto Android actual. El primer plano del icono usa PNG para máxima fidelidad.
- `preview/`: previsualizaciones a formas/tamaños de launcher.

## Instrucciones para Codex

1. Copiar los archivos del directorio `android/app/src/main/res/` al mismo directorio del proyecto, reemplazando los `ic_launcher.xml` de v26 y v33.
2. **Eliminar** `app/src/main/res/mipmap-anydpi/ic_launcher.xml` (icono vectorial antiguo para anteriores a Android 8), pues el fallback lo proporcionan los PNG de mipmap-mdpi a mipmap-xxxhdpi.
3. El nuevo fondo está en `@drawable/ic_launcher_background` (`drawable-nodpi`). No es necesario cambiar `@color/launcher_background` antiguo para renderizar el nuevo icono, aunque conviene actualizarlo o retirarlo para eliminar verde obsoleto.
4. `AndroidManifest.xml` puede conservar `android:icon="@mipmap/ic_launcher"` y `android:roundIcon="@mipmap/ic_launcher"`.
5. Comprobar launcher adaptativo en recortes circular/redondeado y el icono temático en Android 13+, y testear legibilidad a 48 px. No cambiar lógica de negocio ni Room.
6. Ejecutar `assembleDebug`, hacer commit + push y aportar hash final.

## Limitaciones

- El SVG coloreado es una **aproximación vectorial de apariencia**, no el mismo degradado pixel a pixel. Para Android, la versión raster extraída respeta mejor la imagen seleccionada.
- Las capas adaptativas son fondos sin máscara y primer plano transparente: el launcher Android aplica el recorte, como especifica la documentación oficial.
- El icono monocromático es blanco opaco sobre transparencia, apropiado para teñido del sistema.

Origen de documentación técnica: https://developer.android.com/develop/ui/compose/system/icon_design_adaptive
