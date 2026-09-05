# Field Intelligence — AR Plant Identifier & AI Botanical Guide

An app that uses the phone camera and an image classifier to identify plants and display botanical metadata using an AR-style overlay, plus an integrated AI Botanical Guide chat. This repository contains the Android app (Kotlin), a Python-based plant classifier backend, and model/dataset artifacts.

Live demo & distribution plan
- Mobile: Publish the Android app on Google Play so any user can install it and scan plants in the field.
- Web: Provide a marketing/demo website (GitHub Pages or small hosted site) with screenshots, a demo video, and a simple photo-upload demo that calls the same backend.
- Backend: Host the identify / chat endpoints on a cloud provider (Render, Cloud Run, Vercel functions, or Heroku) so the web demo and app share the same API.

Key features
- Real-time camera scanner (CameraX) + AR-style overlay for species metadata.
- Species metadata includes: Common Name, Scientific Name, Family, Native Region, Conservation Status, Ecological Importance (displayed in overlay), and match confidence.
- Trained Keras classifier (plant_classifier/plant_classifier.keras) and prediction server (plant_classifier/predict.py, model_api.py).
- AI Botanical Guide (chat endpoint) to ask species-specific questions.
- Demo fallback so UI can be showcased without a live backend.

Repository structure (high-level)
- FieldIntelligenceApp/ — Android application (Kotlin)
  - app/src/main/java/com/geg/fieldintel — app code (camera, UI, data layer)
  - app/src/main/java/.../ui/arresult — AR overlay UI (Species overlay updated to show Ecological Importance)
- plant_classifier/ — model and dataset utilities
  - plant_classifier.keras — Keras model artifact (edge model; convert to TFLite for on-device inference)
  - class_names.json, plant_info.csv, dataset_summary.csv, MODEL_INTEGRATION.md
  - model_api.py, predict.py — model server and inference examples
- backend_api.py — example Python backend pairing classifier + chat

Quickstart (developer)
Requirements:
- Android Studio (for building app)
- Python 3.8+ with dependencies from plant_classifier/requirements.txt (if provided) or root requirements.txt
- Git and GitHub access

Run the backend locally (recommended: use virtualenv)
1. cd plant_classifier
2. pip install -r ../requirements.txt  # or plant_classifier/requirements.txt if present
3. Start the model API:
   - python model_api.py
   - The service should expose POST /identify and POST /chat (see BotanicalGuideApi.kt contract).
4. Optionally start the backend proxy (backend_api.py) if it consolidates identify + chat.

Run the Android app (demo mode)
1. Open FieldIntelligenceApp in Android Studio.
2. Ensure BuildConfig.BOTANICAL_API_BASE_URL points to your local backend (emulator host mapping may be needed: http://10.0.2.2:8000 or your host IP).
3. Run on a device/emulator with camera support.
4. If the backend is unreachable, the app has a demo fallback that seeds species for demonstration.

API contract (important endpoints)
- POST /identify (multipart/form-data)
  - image: file
  - lat, lng: optional
  - Response (IdentifyResponse):
    - status: "success" | "multiple_candidates" | "no_match" | "error"
    - species: object with {id, commonName, scientificName, family, nativeRegion, conservationStatus, isNative, shortDescription, funFact, imageUrl, confidence, ecologicalImportance}
    - candidates: list of the same species objects if status == "multiple_candidates"
- POST /chat
  - { message, speciesId?, conversationId? }
  - Response: { reply, conversationId? }

Important implementation notes (we updated these in code)
- Ecological Importance: added to the domain Species model and displayed in the AR overlay. Backend should supply `ecologicalImportance` in identify responses.
- Class-id normalization: the app normalizes class names into canonical lowercase_underscore IDs (strip suffixes like " - Google Search" and convert spaces/punctuation to underscores). Backend should either return a canonical id or allow the app to normalize.
- Conservation status parsing: the app accepts both codes ("LC", "EN") and human-readable labels ("Least Concern"). The ConservationBadge renders the appropriate color.

Deploy the backend (recommended approaches)
- Quick: Deploy model_api.py to Render or Heroku (container or basic Python app). Ensure dependencies and model files are included.
- Scalable: Package as a Docker image and deploy to Cloud Run (GCP) or AWS ECS/Fargate. Use Cloud Storage for model artifacts if large.
- On-device inference: convert the Keras model to TensorFlow Lite for on-device inference in the app (smaller binary and faster inference), and ship tflite model under app/src/main/assets or use ML Kit.

Web demo ideas
- Upload photo UI: Let visitors upload a photo which the demo server forwards to /identify, display returned metadata.
- Live camera in browser: Use WebRTC + a short upload flow (browser capture -> send image to backend).
- Interactive gallery: show sample images and model predictions with confidence and links to plant info.

Publishing the Android app
- Prepare icons, screenshots, and a short pitch.
- Build a release APK / AAB and test on physical devices.
- Create a Play Console entry (privacy policy, app category, contact).
- Publish a staged rollout and collect early user feedback.

Showcase materials (what to prepare)
- 60–90s demo video showing:
  - Scanning a plant → AR overlay appears with all metadata including Ecological Importance and Conservation Status
  - Tapping "Ask the AI Botanical Guide" and getting a contextual reply
  - Multiple-candidate flow and the demo fallback
- 5–6 slide deck:
  - Team & problem statement
  - Demo screenshots/video + technical architecture
  - Dataset & model summary (8 species, dataset counts)
  - User testing insights & impact
  - Next steps & roadmap
- One-page test plan & expected outcomes for UX testers

Privacy & ethics
- Notify users the app collects images of plants; store images only if users consent.
- If using location (lat/lng), disclose it and allow opt-out.
- Provide dataset provenance and attribution for plant info and photos, especially if using third-party images.

Contributing and maintenance
- Add contribution guidelines (CONTRIBUTING.md) and a code of conduct if the repo will accept external contributions.
- Track issues and feature requests (label backlog, bug, enhancement).
- Add unit/integration tests for PlantRepository mapping and SpeciesDto->Species conversion to protect the normalization and parsing code.

Showcase checklist (short)
- [ ] Backend deployed and stable (public URL)
- [ ] Demo website with upload demo and screenshots
- [ ] Release candidate AAB built and tested on multiple devices
- [ ] Demo video and 5–6 slide deck ready
- [ ] User testing: 5+ testers completed; feedback logged
- [ ] README updated (this file)
- [ ] Play Store listing created (if publishing)

Contact / Team
- Project maintainers: see repository owners and commit history.
- For live demos and support, include a contact email in the Play Store listing / demo site.
