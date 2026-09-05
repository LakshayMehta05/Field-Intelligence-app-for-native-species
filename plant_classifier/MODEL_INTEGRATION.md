# Model Integration Guide 🔌

This guide explains how backend and frontend developers on the team can seamlessly integrate the Plant Image Classification AI into the main hackathon application.

## Prerequisites

Ensure the following 4 files are present in the same directory as the backend application you are running:
- `model_api.py` (The integration module)
- `plant_classifier.keras` (The trained neural network weights)
- `class_names.json` (The class mapping array)
- `plant_info.csv` (The botanical database)

If you haven't already, install TensorFlow in your environment:
```bash
pip install tensorflow
```

## How to use `predict_plant()`

The `model_api.py` module exposes a single, simple function: `predict_plant(image_path)`. 

> [!TIP]
> **Performance Optimization:** 
> The neural network and CSV data are **lazily loaded and cached** in memory on the first call. This means the very first time you call `predict_plant()`, it might take a few seconds to initialize TensorFlow, but all subsequent calls will be extremely fast!

### Example Backend Integration

```python
from model_api import predict_plant

# Path to the image the user uploaded from the app
image_path = "path/to/uploaded/image.jpg"

# Call the API
result = predict_plant(image_path)

# Handle potential file errors
if "error" in result:
    print(f"Failed to process image: {result['error']}")
    
# Handle the 60% confidence threshold (Low Confidence Fallback)
elif result["low_confidence"]:
    print(f"Confidence was only {result['confidence']*100:.1f}%.")
    print("Action needed: Ask the user to retake the photo for a clearer result.")
    
# Successful and confident prediction
else:
    print(f"Identified Plant: {result['common_name']}")
    print(f"Scientific Name: {result['scientific_name']}")
    print(f"Conservation Status: {result['conservation_status']}")
```

### The Output Dictionary

When successful, `predict_plant` returns a clean, structured Python dictionary that is ready to be returned as a JSON response from your web API:

```json
{
    "predicted_class": "lantana - Google Search",
    "confidence": 0.729,
    "low_confidence": false,
    "common_name": "Lantana",
    "scientific_name": "Lantana camara",
    "family": "Verbenaceae",
    "native_region": "Central and South America",
    "ecological_importance": "Flowers provide abundant nectar for butterflies...",
    "conservation_status": "Least Concern"
}
```

This guarantees your app interface can immediately display accurate, structured biological information without parsing terminal strings!
