@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.arete.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Date
import java.util.Locale
import com.arete.app.ui.theme.ActionBlue

@Composable
fun TonalCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .22f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
}

@Composable
fun SectionHeading(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun MetricTile(label: String, value: String, modifier: Modifier = Modifier, caption: String? = null) {
    TonalCard(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.displaySmall)
        if (caption != null) Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

data class ChartValue(val timestamp: Long, val value: Double)

@Composable
fun StaticTrendChart(points: List<ChartValue>, unit: String, modifier: Modifier = Modifier) {
    val sorted = points.sortedBy { it.timestamp }
    if (sorted.isEmpty()) { Text("Aún no hay datos representativos", style = MaterialTheme.typography.bodySmall); return }
    val color = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f)
    val min = sorted.minOf { it.value }
    val max = sorted.maxOf { it.value }
    val span = (max - min).takeIf { it > 0 } ?: 1.0
    val first = sorted.first(); val last = sorted.last()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${uiNumber(min)}–${uiNumber(max)} $unit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${uiNumber(last.value)} $unit", style = MaterialTheme.typography.labelLarge, color = color)
        }
        Canvas(Modifier.fillMaxWidth().height(120.dp).semantics {
            contentDescription = "Evolución: ${sorted.size} registros, de ${uiNumber(first.value)} a ${uiNumber(last.value)} $unit, ${uiDate(first.timestamp)} a ${uiDate(last.timestamp)}"
        }) {
            val inset = 5.dp.toPx()
            val width = size.width - inset * 2; val height = size.height - inset * 2
            (0..3).forEach { step -> val y = inset + height * step / 3; drawLine(grid, Offset(inset, y), Offset(size.width - inset, y), 1.dp.toPx()) }
            val duration = (last.timestamp - first.timestamp).coerceAtLeast(1)
            val offsets = sorted.map { point -> Offset(
                if (sorted.size == 1) size.width / 2 else inset + ((point.timestamp - first.timestamp).toDouble() / duration * width).toFloat(),
                if (max == min) size.height / 2 else inset + height - ((point.value - min) / span * height).toFloat()
            ) }
            offsets.zipWithNext().forEach { (a,b) -> drawLine(ActionBlue, a, b, 2.dp.toPx(), StrokeCap.Round) }
            offsets.forEachIndexed { index, point -> drawCircle(ActionBlue, if (index == offsets.lastIndex) 4.dp.toPx() else 2.dp.toPx(), point) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(uiDate(first.timestamp), style = MaterialTheme.typography.labelSmall)
            if (sorted.size > 1) Text(uiDate(last.timestamp), style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun uiNumber(value: Double): String = String.format(Locale.forLanguageTag("es-ES"), "%.2f", value).trimEnd('0').trimEnd(',')
fun uiDate(timestamp: Long): String = DateFormat.format("dd MMM yyyy", Date(timestamp)).toString()

@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Column {
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.headlineSmall)
        } },
        navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

/** A flat, separated row: lists keep their density instead of becoming a stack of cards. */
@Composable
fun EditorialRow(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 64.dp).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, content = content)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f))
    }
}

@Composable
fun StatusLabel(text: String, positive: Boolean = false) {
    val color = if (positive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha = .1f), contentColor = color) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun PrimaryAction(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Button(onClick, modifier.heightIn(min = 48.dp), enabled = enabled, shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White), content = content)
}

@Composable
fun SecondaryAction(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled = enabled, shape = MaterialTheme.shapes.medium, content = content)
}

@Composable
fun AddAction(label: String, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        PrimaryAction(onClick = onClick, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    }
}

@Composable
fun AreteField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null, placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null, singleLine: Boolean = false, minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default) {
    val focus = LocalFocusManager.current
    OutlinedTextField(value, onValueChange, modifier, label = label, placeholder = placeholder, leadingIcon = leadingIcon,
        singleLine = singleLine, minLines = minLines, shape = MaterialTheme.shapes.small,
        keyboardOptions = if (singleLine && keyboardOptions.imeAction == ImeAction.Default) keyboardOptions.copy(imeAction = ImeAction.Next) else keyboardOptions,
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }, onDone = { focus.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)))
}
