import base64
import json
import os
import sys
import tempfile

from fastapi import FastAPI, File, Form, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from groq import Groq
from dotenv import load_dotenv

# Add the plant_classifier directory to the python path so we can import model_api
CLASSIFIER_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), 'plant_classifier'))
sys.path.append(CLASSIFIER_DIR)
from model_api import predict_plant

load_dotenv()

app = FastAPI(title="AI Botanical Backend API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize Groq client
client = Groq(api_key=os.environ.get("GROQ_API_KEY"))

GROQ_MODEL = "groq/compound"

IDENTIFY_SCHEMA_PROMPT = """
You are a highly capable botanical assistant. You will be provided with structured metadata about a plant that has been identified by a computer vision model.

Your task is to generate ecological insights and format the output EXACTLY as the requested JSON structure.

DO NOT identify the plant from scratch. Use the provided species information. If the confidence is low, clearly communicate uncertainty.

Respond ONLY with minified JSON (no prose, no markdown fences) matching this structure:
{
  "shortDescription": string (concise explanation of the species and its ecological importance),
  "funFact": string (useful field observations or conservation recommendations)
}
"""

@app.post("/identify")
async def identify(image: UploadFile = File(...), lat: str = Form(None), lng: str = Form(None)):
    # 1. Save uploaded image to temp file for the ML model
    with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as tmp:
        content = await image.read()
        tmp.write(content)
        tmp_path = tmp.name

    # 2. Run local ML prediction
    try:
        # Note: model_api reads relative to its own path since we moved it
        # but just in case, we change dir temporarily if needed.
        # Actually model_api was not updated to use absolute paths, so let's temporarily cd
        cwd = os.getcwd()
        os.chdir(CLASSIFIER_DIR)
        ml_result = predict_plant(tmp_path)
        os.chdir(cwd)
    except Exception as e:
        os.remove(tmp_path)
        return {"status": "error", "message": str(e)}
    
    os.remove(tmp_path)
        
    if "error" in ml_result:
        return {"status": "error", "message": ml_result["error"]}
        
    is_low_confidence = ml_result.get("low_confidence", False)
    
    # 3. Call Groq for ecological insights
    user_prompt = f"""
    Computer Vision Prediction Results:
    Species: {ml_result.get('common_name')} ({ml_result.get('scientific_name')})
    Confidence: {ml_result.get('confidence')}
    Low Confidence Flag: {is_low_confidence}
    Family: {ml_result.get('family')}
    Native Region: {ml_result.get('native_region')}
    Ecological Importance: {ml_result.get('ecological_importance')}
    Conservation Status: {ml_result.get('conservation_status')}
    
    Please generate the shortDescription and funFact JSON for this plant.
    """
    
    try:
        completion = client.chat.completions.create(
            model=GROQ_MODEL,
            messages=[
                {"role": "system", "content": IDENTIFY_SCHEMA_PROMPT},
                {"role": "user", "content": user_prompt}
            ],
            temperature=0.2,
            response_format={"type": "json_object"}
        )
        
        raw_text = completion.choices[0].message.content
        ai_insights = json.loads(raw_text)
    except Exception as e:
        print(f"GROQ ERROR: {str(e)}")
        ai_insights = {
            "shortDescription": ml_result.get('ecological_importance', "Ecological data unavailable."),
            "funFact": "Groq API integration failed or timed out."
        }

    # 4. Construct response matching IdentifyResponse / SpeciesDto in Android
    species_dto = {
        "id": ml_result.get("predicted_class", "unknown").replace(" ", "_"),
        "commonName": ml_result.get("common_name", "Unknown"),
        "scientificName": ml_result.get("scientific_name", "Unknown"),
        "family": ml_result.get("family", "Unknown"),
        "nativeRegion": ml_result.get("native_region", "Unknown"),
        "conservationStatus": ml_result.get("conservation_status", "Unknown"),
        "isNative": True,
        "shortDescription": ai_insights.get("shortDescription", ""),
        "funFact": ai_insights.get("funFact", ""),
        "confidence": float(ml_result.get("confidence", 0.0))
    }

    status = "success"
    message = None
    if is_low_confidence:
        status = "multiple_candidates" 
        message = "Low confidence prediction. Please retake the photo for better accuracy."
        
    return {
        "status": status,
        "species": species_dto,
        "message": message
    }


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

    try:
        completion = client.chat.completions.create(
            model=GROQ_MODEL,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": req.message}
            ],
            temperature=0.7
        )
        reply = completion.choices[0].message.content
    except Exception as e:
        reply = f"Error communicating with AI service: {str(e)}"
        
    return {"reply": reply, "conversationId": req.conversationId or "demo-conversation"}

# Run with: uvicorn backend_api:app --reload
