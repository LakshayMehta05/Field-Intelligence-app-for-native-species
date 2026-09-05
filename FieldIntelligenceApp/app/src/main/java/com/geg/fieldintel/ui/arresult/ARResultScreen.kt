package com.geg.fieldintel.ui.arresult

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.geg.fieldintel.camera.CameraXController
import com.geg.fieldintel.data.model.ConservationStatus
import com.geg.fieldintel.data.model.ScanResult
import com.geg.fieldintel.data.model.Species
import com.geg.fieldintel.ui.theme.AlertAmber
import com.geg.fieldintel.ui.theme.ConservationRed
import com.geg.fieldintel.ui.theme.LeafGreen

/**
 * AR Visualization screen — displays species metadata (Scientific Name, Family, Native Region,
 * Conservation Status, etc.) as a card overlay anchored over the live camera/plant view.
 *
 * This keeps the camera feed running behind a translucent metadata card so it reads as an
 * AR heads-up display. Swap the background preview for an ARCore Session + HitResult anchor
 * (see camera/ARSessionController.kt notes in the implementation guide) to anchor the card to
 * a real-world plane instead of the screen center.
 */
@Composable
fun ARResultScreen(
    result: ScanResult,
    onBack: () -> Unit,
    onAskAboutSpecies: (Species?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { CameraXController(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    cameraController.startCamera(this, lifecycleOwner)
                }
            }
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back to scanner", tint = Color.White)
        }

        when (result) {
            is ScanResult.Success -> SpeciesOverlayCard(
                species = result.species,
                modifier = Modifier.align(Alignment.BottomCenter),
                onAsk = { onAskAboutSpecies(result.species) }
            )
            is ScanResult.MultipleCandidates -> CandidatesOverlay(
                candidates = result.candidates,
                modifier = Modifier.align(Alignment.BottomCenter),
                onSelect = { onAskAboutSpecies(it) }
            )
            ScanResult.NoMatch -> MessageOverlay(
                title = "No confident match",
                message = "Try moving closer, improving lighting, or framing a single leaf/flower.",
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            is ScanResult.Error -> MessageOverlay(
                title = "Something went wrong",
                message = result.message,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun SpeciesOverlayCard(species: Species, modifier: Modifier = Modifier, onAsk: () -> Unit) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Nature, contentDescription = null, tint = LeafGreen)
                Spacer(Modifier.width(8.dp))
                Text(species.commonName, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(Modifier.weight(1f))
                ConservationBadge(species.conservationStatus)
            }
            Text(
                species.scientificName,
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(8.dp))
            MetadataRow("Family", species.family)
            MetadataRow("Native Region", species.nativeRegion)
            MetadataRow("Native to India", if (species.isNative) "Yes" else "No")
            if (species.confidence > 0f) {
                MetadataRow("Match confidence", "${(species.confidence * 100).toInt()}%")
            }
            Spacer(Modifier.height(8.dp))
            Text(species.shortDescription, color = Color.White.copy(alpha = 0.85f))
            species.funFact?.let {
                Spacer(Modifier.height(4.dp))
                Text("🌿 $it", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAsk, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Chat, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ask the AI Botanical Guide about this plant")
            }
        }
    }
}

@Composable
private fun CandidatesOverlay(candidates: List<Species>, modifier: Modifier = Modifier, onSelect: (Species) -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("A few possible matches — tap the closest one:", color = Color.White)
            Spacer(Modifier.height(8.dp))
            candidates.forEach { candidate ->
                TextButton(onClick = { onSelect(candidate) }, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(candidate.commonName, color = Color.White)
                        Text(candidate.scientificName, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageOverlay(title: String, message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(message, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label: ", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ConservationBadge(status: ConservationStatus) {
    val color = when (status) {
        ConservationStatus.LC, ConservationStatus.NT -> LeafGreen
        ConservationStatus.VU -> AlertAmber
        ConservationStatus.EN, ConservationStatus.CR, ConservationStatus.EW -> ConservationRed
        else -> Color.Gray
    }
    Surface(color = color, shape = RoundedCornerShape(50)) {
        Text(
            status.label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
