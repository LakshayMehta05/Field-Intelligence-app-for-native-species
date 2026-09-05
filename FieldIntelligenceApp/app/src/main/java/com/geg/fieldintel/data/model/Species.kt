package com.geg.fieldintel.data.model

/**
 * Species metadata returned by the AI Botanical Guide backend after a scan.
 * Fields map directly to what the AR overlay displays:
 * Scientific Name, Family, Native Region, Conservation Status, etc.
 */
data class Species(
    val id: String,
    val commonName: String,
    val scientificName: String,
    val family: String,
    val nativeRegion: String,
    val conservationStatus: ConservationStatus,
    val isNative: Boolean,
    val shortDescription: String,
    val funFact: String? = null,
    val imageUrl: String? = null,
    val confidence: Float = 0f
)

enum class ConservationStatus(val label: String) {
    LC("Least Concern"),
    NT("Near Threatened"),
    VU("Vulnerable"),
    EN("Endangered"),
    CR("Critically Endangered"),
    EW("Extinct in the Wild"),
    NE("Not Evaluated"),
    DD("Data Deficient")
}

/** Wrapper for a scan result: either a confident match, several candidates, or nothing found. */
sealed class ScanResult {
    data class Success(val species: Species) : ScanResult()
    data class MultipleCandidates(val candidates: List<Species>) : ScanResult()
    object NoMatch : ScanResult()
    data class Error(val message: String) : ScanResult()
}
