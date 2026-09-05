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
            // New: Ecological Importance
            MetadataRow("Ecological Importance", species.ecologicalImportance ?: "Not available")
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
