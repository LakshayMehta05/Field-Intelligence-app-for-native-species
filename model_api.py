import os
import json
import csv
import numpy as np
import tensorflow as tf

# Global variables for caching to avoid reloading the model on every call
_MODEL = None
_CLASS_NAMES = None
_PLANT_INFO = None

def _load_plant_info(csv_path):
    info = {}
    try:
        with open(csv_path, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                info[row['class_name']] = row
    except Exception as e:
        print(f"Error loading {csv_path}: {e}")
    return info

def _match_class_name(predicted_class, plant_info):
    """Matches the predicted folder name to the class_name in the CSV."""
    if predicted_class in plant_info:
        return plant_info[predicted_class]
        
    pred_clean = predicted_class.lower().replace(' ', '_').replace('-', '_')
    for key, data in plant_info.items():
        key_clean = key.lower().replace(' ', '_')
        if key_clean in pred_clean or pred_clean in key_clean:
            return data
            
    return None

def predict_plant(image_path):
    """
    Predicts the plant species from an image and returns a dictionary of botanical information.
    """
    global _MODEL, _CLASS_NAMES, _PLANT_INFO
    
    # Lazy load resources only once
    if _MODEL is None:
        # Hide TF logging for cleaner output in production
        os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'
        _MODEL = tf.keras.models.load_model('plant_classifier.keras')
    
    if _CLASS_NAMES is None:
        with open('class_names.json', 'r', encoding='utf-8') as f:
            _CLASS_NAMES = json.load(f)
            
    if _PLANT_INFO is None:
        _PLANT_INFO = _load_plant_info('plant_info.csv')
        
    if not os.path.exists(image_path):
        return {"error": f"Image '{image_path}' not found."}
        
    try:
        # Load and preprocess image
        img = tf.keras.utils.load_img(image_path, target_size=(224, 224))
        img_array = tf.keras.utils.img_to_array(img)
        img_array = tf.expand_dims(img_array, 0)
    except Exception as e:
        return {"error": f"Error processing image: {str(e)}"}
        
    # Run prediction
    predictions = _MODEL.predict(img_array, verbose=0)
    confidence = float(np.max(predictions[0]))
    predicted_index = int(np.argmax(predictions[0]))
    predicted_class = _CLASS_NAMES[predicted_index]
    
    # Requirement 6 & 7
    result = {
        "predicted_class": predicted_class,
        "confidence": confidence,
        "low_confidence": bool(confidence < 0.60)
    }
    
    # Lookup botanical details
    info = _match_class_name(predicted_class, _PLANT_INFO)
    
    if info:
        result.update({
            "common_name": info['common_name'],
            "scientific_name": info['scientific_name'],
            "family": info['family'],
            "native_region": info['native_region'],
            "ecological_importance": info['ecological_importance'],
            "conservation_status": info['conservation_status']
        })
    else:
        # Fallbacks if info is not found for some reason
        result.update({
            "common_name": predicted_class,
            "scientific_name": "Unknown",
            "family": "Unknown",
            "native_region": "Unknown",
            "ecological_importance": "Unknown",
            "conservation_status": "Unknown"
        })
        
    return result

# 9. Small test at the bottom that can be run directly
if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1:
        test_img = sys.argv[1]
        print(f"Testing API with '{test_img}'...")
        res = predict_plant(test_img)
        print(json.dumps(res, indent=4))
    else:
        print("Usage: python model_api.py <image_path>")
