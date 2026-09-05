"""
Minimal example backend for the AI Botanical Guide API that the Android app
(BotanicalGuideApi.kt) talks to. This is meant for your AI/Backend teammate to
adapt — it demonstrates the two required endpoints:

    POST /identify   multipart image upload -> species metadata
    POST /chat        {message, speciesId?} -> {reply}

Run locally:
    pip install fastapi uvicorn python-multipart anthropic
    export ANTHROPIC_API_KEY=...   (or set in a .env file)
    uvicorn main:app --reload --port 8000

Then in the Android app, set BOTANICAL_API_BASE_URL to
"http://10.0.2.2:8000/" (emulator) or your machine's LAN IP (physical device).

This example calls the Anthropic API for both species identification (via
image understanding) and conversational chat, then reshapes the response
into the JSON contract the app expects. Swap in your own trained
classifier / vector DB of the 7+ campus species for a more reliable
"AI + AR Integration" score if you have time before the deadline.
"""

import base64
import json
import os

import anthropic
from fastapi import FastAPI, File, Form, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

app = FastAPI(title="AI Botanical Guide API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

client = anthropic.Anthropic(api_key=os.environ.get("ANTHROPIC_API_KEY"))

# Curate this with your team's real campus species list (min. 7 per the
# challenge brief). Used to ground the model and reduce hallucination.
CAMPUS_SPECIES_CONTEXT = """
You are identifying native Indian plant species for a campus biodiversity
field guide. Prioritize matching against this shortlist when plausible:
1. Indian Cork Tree (Millingtonia hortensis)
2. Malabar Neem (Melia dubia)
3. Ashoka Tree (Saraca asoca)
4. Indian Trumpet Tree (Oroxylum indicum)
5. Malabar Kino Tree (Pterocarpus marsupium)
6. Flame of the Forest (Butea monosperma)
7. Indian Sandalwood (Santalum album)
If the plant clearly isn't one of these, identify it anyway, but note lower confidence.
"""

IDENTIFY_SCHEMA_PROMPT = """
Respond ONLY with minified JSON (no prose, no markdown fences) matching:
{
  "status": "success" | "no_match" | "error",
  "species": {
    "id": string, "commonName": string, "scientificName": string,
    "family": string, "nativeRegion": string,
    "conservationStatus": "LC"|"NT"|"VU"|"EN"|"CR"|"EW"|"NE"|"DD",
    "isNative": boolean, "shortDescription": string,
    "funFact": string, "confidence": number (0-1)
  } | null
}
"""


@app.post("/identify")
async def identify(image: UploadFile = File(...), lat: str = Form(None), lng: str = Form(None)):
    image_bytes = await image.read()
    b64_image = base64.b64encode(image_bytes).decode("utf-8")
    media_type = image.content_type or "image/jpeg"

    response = client.messages.create(
        model="claude-sonnet-4-6",
        max_tokens=600,
        system=CAMPUS_SPECIES_CONTEXT + IDENTIFY_SCHEMA_PROMPT,
        messages=[{
            "role": "user",
            "content": [
                {"type": "image", "source": {"type": "base64", "media_type": media_type, "data": b64_image}},
                {"type": "text", "text": "Identify the plant species in this photo."},
            ],
        }],
    )

    raw_text = "".join(block.text for block in response.content if block.type == "text")
    try:
        parsed = json.loads(raw_text)
    except json.JSONDecodeError:
        return {"status": "error", "message": "Could not parse model response."}

    return parsed


class ChatRequest(BaseModel):
    message: str
    speciesId: str | None = None
    conversationId: str | None = None


@app.post("/chat")
async def chat(req: ChatRequest):
    system_prompt = (
        "You are a friendly, scientifically accurate AI Botanical Guide inside a "
        "field biodiversity app. Keep answers concise (2-4 sentences) and focused "
        "on native species, ecology, and conservation."
    )
    if req.speciesId:
        system_prompt += f" The user just scanned species with id '{req.speciesId}'; ground your answer in that context if relevant."

    response = client.messages.create(
        model="claude-sonnet-4-6",
        max_tokens=400,
        system=system_prompt,
        messages=[{"role": "user", "content": req.message}],
    )

    reply = "".join(block.text for block in response.content if block.type == "text")
    return {"reply": reply, "conversationId": req.conversationId or "demo-conversation"}
