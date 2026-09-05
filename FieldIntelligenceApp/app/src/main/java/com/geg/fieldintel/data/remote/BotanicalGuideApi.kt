package com.geg.fieldintel.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Contract for the AI Botanical Guide backend that the AI/Backend teammate exposes.
 * This is the boundary Teammate 2 (AR & Frontend) integrates against:
 *   1. POST /identify   -> takes a plant photo, returns species metadata for the AR overlay
 *   2. POST /chat       -> takes a free-text question (optionally with species context),
 *                          returns a conversational answer for the in-app chat interface
 *
 * Point BuildConfig.BOTANICAL_API_BASE_URL at whichever backend implements this contract
 * (a custom Flask/FastAPI service, Firebase Function, or a thin proxy in front of a
 * vision + LLM model).
 */
interface BotanicalGuideApi {

    @Multipart
    @POST("identify")
    suspend fun identifySpecies(
        @Part image: MultipartBody.Part,
        @Part("lat") lat: RequestBody? = null,
        @Part("lng") lng: RequestBody? = null
    ): IdentifyResponse

    @POST("chat")
    suspend fun sendChatMessage(@Body request: ChatRequest): ChatResponse
}

data class IdentifyResponse(
    val status: String,               // "success" | "multiple_candidates" | "no_match" | "error"
    val species: SpeciesDto? = null,
    val candidates: List<SpeciesDto>? = null,
    val message: String? = null
)

data class SpeciesDto(
    val id: String,
    val commonName: String,
    val scientificName: String,
    val family: String,
    val nativeRegion: String,
    val conservationStatus: String,   // e.g. "LC", "VU", "EN" or full label
    val isNative: Boolean,
    val shortDescription: String,
    val funFact: String? = null,
    val imageUrl: String? = null,
    val confidence: Float = 0f,
    // New: optional ecological importance text sent by backend
    val ecologicalImportance: String? = null
)

data class ChatRequest(
    val message: String,
    val speciesId: String? = null,    // optional: grounds the chat in the last scanned plant
    val conversationId: String? = null
)

data class ChatResponse(
    val reply: String,
    val conversationId: String? = null
)
