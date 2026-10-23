@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.arete.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arete.app.catalog.EditorialExercise
import com.arete.app.catalog.isWebLink
import com.arete.app.data.local.ExerciseEntity

@Composable
fun ExerciseGuideSheet(id: String?, viewModel: AreteViewModel, title: String = "Guía técnica", onEdit: ((String) -> Unit)? = null, onDismiss: () -> Unit) {
    var loaded by remember(id) { mutableStateOf(false) }
    val exercise by produceState<ExerciseEntity?>(null, id) {
        value = id?.let { viewModel.getExerciseGuide(it) }
        loaded = true
    }
    val manifest by viewModel.exerciseEditorial.collectAsStateWithLifecycle()
    val editorial = manifest?.forExercise(exercise)
    var language by rememberSaveable(id) { mutableStateOf("es") }
    var showArtwork by rememberSaveable(id) { mutableStateOf(false) }
    var showReferences by rememberSaveable(id) { mutableStateOf(false) }
    val localized = editorial?.localized(language)
    val english = localized?.first == "en"
    val scroll = rememberScrollState()
    val context = LocalContext.current
    fun openLink(url: String) {
        if (isWebLink(url)) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { viewModel.showMessage("No se pudo abrir el enlace") }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(scroll).navigationBarsPadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar guía") }
            }
            if (!loaded) Text("Cargando ejercicio…")
            else if (exercise == null) EmptyState("Ejercicio no disponible", "Su registro de entrenamiento se conserva. No hay una ficha vinculada a este ID.")
            exercise?.let { current ->
                Text(if (english) current.nameEn.ifBlank { current.nameEs } else current.nameEs, style = MaterialTheme.typography.titleLarge)
                Text("${current.primaryMuscle} · ${current.equipment}", color = MaterialTheme.colorScheme.primary)
                if (!english && current.nameEn.isNotBlank()) Text(current.nameEn, style = MaterialTheme.typography.bodySmall)
                if (editorial != null && localized != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(selected = !english, onClick = { language = "es" }, label = { Text("Español") })
                        if ("en" in editorial.text) FilterChip(selected = english, onClick = { language = "en" }, label = { Text("English") })
                    }
                    Text(if (english) "Offline guide · Arete editorial" else "Guía offline · Editorial Arete", style = MaterialTheme.typography.labelSmall)
                    val text = localized.second
                    GuideSection(if (english) "Preparation" else "Preparación", text.setup)
                    GuideSection(if (english) "Execution" else "Ejecución", text.steps, numbered = true)
                    GuideSection(if (english) "Technique cues" else "Indicaciones técnicas", text.cues)
                    if (editorial.illustrations.isNotEmpty()) {
                        SecondaryAction(onClick = { showArtwork = !showArtwork }) {
                            Text(if (english) "${if (showArtwork) "Hide" else "View"} illustrations (${editorial.illustrations.size})"
                                else "${if (showArtwork) "Ocultar" else "Ver"} ilustraciones (${editorial.illustrations.size})")
                        }
                        if (showArtwork) {
                            GuideIllustrations(editorial, localized.first)
                            Text(editorial.illustrations.distinctBy { it.author to it.license }.joinToString(" · ") { "${it.author} · ${it.license}" },
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else Text(if (english) "Text guide · Illustration pending" else "Guía de texto · Ilustración pendiente",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    GuideSection(if (english) "Common mistakes" else "Errores habituales", text.mistakes)
                    GuideSection(if (english) "Muscles involved" else "Músculos implicados", text.muscles)
                    HorizontalDivider()
                    TextButton(onClick = { showReferences = !showReferences }) {
                        Text(if (english) "${if (showReferences) "Hide" else "View"} sources and credits" else "${if (showReferences) "Ocultar" else "Ver"} fuentes y créditos")
                    }
                    if (showReferences) {
                    SectionHeading(if (english) "Evidence and sources" else "Evidencia y fuentes")
                    Text(if (english) "ACSM 2026 reviews resistance-training prescription in healthy adults. It does not validate each technique cue or these illustrations. Technique cues are practical guidance, not a personal prescription."
                        else "ACSM 2026 revisa la prescripción del entrenamiento de fuerza en adultos sanos. No valida cada indicación técnica ni estas ilustraciones. La técnica descrita es orientación práctica, no una prescripción personal.", style = MaterialTheme.typography.bodySmall)
                    manifest?.sources?.filter { it.id in editorial.sourceIds }?.forEach { source ->
                        Text(if (source.kind == "general-evidence") { if (english) "General evidence" else "Evidencia general" }
                            else { if (english) "Practical technique" else "Técnica práctica" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { openLink(source.url) }) { Text(source.title) }
                    }
                    Text(if (english) "Original summary by Arete; linked sources are not endorsements. Adjust load and range to your control. Stop if you experience pain and seek qualified advice. No injury-prevention guarantee."
                        else "Resumen original de Arete; las fuentes enlazadas no implican aval. Adapta carga y recorrido a tu control. Ante dolor, detente y consulta a un profesional. No garantiza prevenir lesiones.", style = MaterialTheme.typography.bodySmall)
                    if (editorial.illustrations.isNotEmpty()) {
                        SectionHeading(if (english) "Illustration credits" else "Créditos de ilustraciones")
                        editorial.illustrations.forEach { art ->
                            Text("${art.captionText(localized.first)} · ${art.author} · ${art.license}", style = MaterialTheme.typography.bodySmall)
                            Text(art.changes, style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { openLink(art.sourceUrl) }) { Text("Original") }
                                TextButton(onClick = { openLink(art.licenseUrl) }) { Text(if (english) "License" else "Licencia") }
                            }
                        }
                        Text(if (english) "Images retain the license stated for each file. No endorsement by the author or distributor."
                            else "Las imágenes conservan la licencia indicada para cada archivo. Sin aval del autor ni del distribuidor.", style = MaterialTheme.typography.labelSmall)
                    }
                    }
                } else {
                    Text(if (current.isCustom) "Contenido personalizado · Sin guía editorial asociada" else "Ficha básica · Sin guía ampliada disponible", style = MaterialTheme.typography.labelMedium)
                    if (current.secondaryMuscles.isNotEmpty()) Text("Secundarios: ${current.secondaryMuscles.joinToString()}", style = MaterialTheme.typography.bodySmall)
                    SectionHeading("Instrucciones guardadas")
                    Text(current.instructions.ifBlank { "No hay instrucciones guardadas para este ejercicio." })
                    Text("Sin ilustración disponible. Puedes registrar el ejercicio con normalidad.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                current.externalUrl?.takeIf(::isWebLink)?.let { url -> SecondaryAction(onClick = { openLink(url) }) { Text("Abrir guía externa") } }
                if (current.isCustom && onEdit != null) PrimaryAction(onClick = { onEdit(current.id) }) { Text("Editar ejercicio") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GuideSection(title: String, lines: List<String>, numbered: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeading(title)
        lines.forEachIndexed { index, line -> Text(if (numbered) "${index + 1}. $line" else "• $line", style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun GuideIllustrations(editorial: EditorialExercise, language: String) {
    val images = editorial.illustrations.filter { artworkResource(it.resource) != null }
    if (images.isEmpty()) {
        Text(if (language == "en") "No illustration available. The offline text guide remains available."
            else "Sin ilustración disponible. La guía de texto funciona sin conexión.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        BoxWithConstraints {
            if (maxWidth >= 320.dp && images.size == 2) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                images.forEach { art -> ExercisePosition(art, language, Modifier.weight(1f)) }
            } else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                images.forEach { art -> ExercisePosition(art, language) }
            }
        }
    }
}
