package com.arete.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arete.app.R
import com.arete.app.catalog.ExerciseIllustration

/** Explicit allow-list: no remote media, reflection or arbitrary resource lookup. */
fun artworkResource(name: String): Int? = when (name) {
    "bench_press_lowered" -> R.drawable.bench_press_lowered
    "bench_press_extended" -> R.drawable.bench_press_extended
    "incline_dumbbell_lowered" -> R.drawable.incline_dumbbell_lowered
    "incline_dumbbell_extended" -> R.drawable.incline_dumbbell_extended
    "lateral_raise_lowered" -> R.drawable.lateral_raise_lowered
    "lateral_raise_raised" -> R.drawable.lateral_raise_raised
    else -> null
}

@Composable
fun ExerciseThumbnail(art: ExerciseIllustration?, modifier: Modifier = Modifier) {
    val resource = art?.let { artworkResource(it.resource) }
    Box(modifier.size(48.dp).clip(MaterialTheme.shapes.small)
        .background(if (resource != null) Color.White else MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center) {
        if (resource != null) Image(painterResource(resource), contentDescription = null,
            modifier = Modifier.fillMaxSize().padding(3.dp), contentScale = ContentScale.Fit)
        else Icon(Icons.Default.FitnessCenter, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun ExercisePosition(art: ExerciseIllustration, language: String, modifier: Modifier = Modifier) {
    val resource = artworkResource(art.resource) ?: return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(painterResource(resource), contentDescription = art.altText(language), contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium).background(Color.White).padding(8.dp))
        Text(art.captionText(language), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
