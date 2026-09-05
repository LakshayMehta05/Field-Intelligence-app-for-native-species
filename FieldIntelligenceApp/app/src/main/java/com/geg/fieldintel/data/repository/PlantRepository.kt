package com.geg.fieldintel.data.repository

import com.geg.fieldintel.data.model.ConservationStatus
import com.geg.fieldintel.data.model.ScanResult
import com.geg.fieldintel.data.model.Species
import com.geg.fieldintel.data.remote.BotanicalGuideApi
import com.geg.fieldintel.data.remote.SpeciesDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class PlantRepository(
    private val api: BotanicalGuideApi,
    /** Flip to false once the real backend from the AI/Data teammate is live. */
    private val useDemoDataWhenOffline: Boolean = true
) {

    suspend fun identify(imageFile: File, lat: Double?, lng: Double?): ScanResult =
        withContext(Dispatchers.IO) {
            try {
                val body = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("image", imageFile.name, body)
                val response = api.identifySpecies(part, null, null)

                when (response.status) {
                    "success" -> response.species?.toDomain()?.let { ScanResult.Success(it) }
                        ?: ScanResult.NoMatch
                    "multiple_candidates" -> ScanResult.MultipleCandidates(
                        response.candidates.orEmpty().map { it.toDomain() }
                    )
                    "no_match" -> ScanResult.NoMatch
                    else -> ScanResult.Error(response.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                if (useDemoDataWhenOffline) demoScanResult() else ScanResult.Error(
                    e.message ?: "Could not reach the AI Botanical Guide backend"
                )
            }
        }

    /**
     * Local fallback so the AR + chat flow can be demoed at the hackathon table even if the
     * backend teammate's endpoint isn't reachable. Swap the seed list in DemoSpeciesData for
     * your team's 7+ campus species.
     */
    private fun demoScanResult(): ScanResult =
        ScanResult.Success(DemoSpeciesData.all.random())
}

private fun SpeciesDto.toDomain() = Species(
    id = id,
    commonName = commonName,
    scientificName = scientificName,
    family = family,
    nativeRegion = nativeRegion,
    conservationStatus = runCatching { ConservationStatus.valueOf(conservationStatus) }
        .getOrDefault(ConservationStatus.NE),
    isNative = isNative,
    shortDescription = shortDescription,
    funFact = funFact,
    imageUrl = imageUrl,
    confidence = confidence
)

/** Seed data for at least 7 native/campus species, per the challenge's minimum requirement. */
object DemoSpeciesData {
    val all = listOf(
        Species(
            id = "sp1", commonName = "Indian Cork Tree", scientificName = "Millingtonia hortensis",
            family = "Bignoniaceae", nativeRegion = "Western Ghats & Indian Subcontinent",
            conservationStatus = ConservationStatus.LC, isNative = true,
            shortDescription = "A tall flowering tree with fragrant white blossoms, often used in Ayurvedic medicine.",
            funFact = "Its corky bark inspired the common name 'cork tree'.", confidence = 0.93f
        ),
        Species(
            id = "sp2", commonName = "Malabar Neem", scientificName = "Melia dubia",
            family = "Meliaceae", nativeRegion = "Western Ghats",
            conservationStatus = ConservationStatus.LC, isNative = true,
            shortDescription = "Fast-growing native tree important for soil restoration in degraded land.",
            confidence = 0.88f
        ),
        Species(
            id = "sp3", commonName = "Ashoka Tree", scientificName = "Saraca asoca",
            family = "Fabaceae", nativeRegion = "Indian Subcontinent",
            conservationStatus = ConservationStatus.EN, isNative = true,
            shortDescription = "Culturally significant flowering tree, now endangered due to habitat loss.",
            funFact = "Considered sacred in Hindu and Buddhist traditions.", confidence = 0.91f
        ),
        Species(
            id = "sp4", commonName = "Indian Trumpet Tree", scientificName = "Oroxylum indicum",
            family = "Bignoniaceae", nativeRegion = "Western Ghats & Himalayan Foothills",
            conservationStatus = ConservationStatus.NT, isNative = true,
            shortDescription = "Known for its long sword-shaped seed pods and medicinal bark.",
            confidence = 0.85f
        ),
        Species(
            id = "sp5", commonName = "Malabar Kino Tree", scientificName = "Pterocarpus marsupium",
            family = "Fabaceae", nativeRegion = "Western Ghats",
            conservationStatus = ConservationStatus.VU, isNative = true,
            shortDescription = "Source of 'vijaysar', a traditional remedy; slow-growing and over-harvested.",
            confidence = 0.79f
        ),
        Species(
            id = "sp6", commonName = "Flame of the Forest", scientificName = "Butea monosperma",
            family = "Fabaceae", nativeRegion = "Indian Subcontinent & Southeast Asia",
            conservationStatus = ConservationStatus.LC, isNative = true,
            shortDescription = "Bright orange-flowered tree, a key nectar source for pollinators in spring.",
            confidence = 0.94f
        ),
        Species(
            id = "sp7", commonName = "Indian Sandalwood", scientificName = "Santalum album",
            family = "Santalaceae", nativeRegion = "Western Ghats & Deccan Plateau",
            conservationStatus = ConservationStatus.VU, isNative = true,
            shortDescription = "Fragrant heartwood species, historically over-exploited and now protected.",
            funFact = "A partial root parasite — it needs a host plant nearby to thrive.",
            confidence = 0.90f
        )
    )
}
