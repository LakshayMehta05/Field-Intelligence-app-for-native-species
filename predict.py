import sys
import os
import argparse
import json
import csv
import numpy as np

try:
    import tensorflow as tf
except ImportError:
    print("TensorFlow is required. Please install it using: pip install tensorflow")
    sys.exit(1)

def load_plant_info(csv_path):
    """Loads plant botanical data from CSV into a dictionary."""
    info = {}
    try:
        with open(csv_path, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                info[row['class_name']] = row
    except Exception as e:
        print(f"Error loading {csv_path}: {e}")
    return info

def match_class_name(predicted_class, plant_info):
    """Matches the predicted folder name to the class_name in the CSV."""
    # Try exact match first
    if predicted_class in plant_info:
        return plant_info[predicted_class]
        
    # Try flexible matching in case dataset folder names have spaces or suffixes
    pred_clean = predicted_class.lower().replace(' ', '_').replace('-', '_')
    for key, data in plant_info.items():
        key_clean = key.lower().replace(' ', '_')
        if key_clean in pred_clean or pred_clean in key_clean:
            return data
            
    return None

def main():
    parser = argparse.ArgumentParser(description="Predict plant species from an image.")
    parser.add_argument("image_path", help="Path to the plant image file")
    args = parser.parse_args()
    
    img_path = args.image_path
    
    # Validate files exist
    if not os.path.exists(img_path):
        print(f"Error: Image '{img_path}' not found.")
        sys.exit(1)
        
    required_files = ['plant_classifier.keras', 'class_names.json', 'plant_info.csv']
    for req_file in required_files:
        if not os.path.exists(req_file):
            print(f"Error: Required file '{req_file}' not found in the current directory.")
            sys.exit(1)
            
    # Load class mapping and info
    with open('class_names.json', 'r', encoding='utf-8') as f:
        class_names = json.load(f)
        
    plant_info = load_plant_info('plant_info.csv')
    
    # Hide TF logging for cleaner output
    os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'
    
    print("Loading model (this might take a moment)...")
    try:
        model = tf.keras.models.load_model('plant_classifier.keras')
    except Exception as e:
        print(f"Error loading model: {e}")
        sys.exit(1)
    
    # Load and preprocess image
    # The saved model already contains the MobileNetV2 preprocessing layer, 
    # so we just need to load, resize to 224x224, and batch it.
    try:
        img = tf.keras.utils.load_img(img_path, target_size=(224, 224))
        img_array = tf.keras.utils.img_to_array(img)
        img_array = tf.expand_dims(img_array, 0) # Create a batch
    except Exception as e:
        print(f"Error processing image: {e}")
        sys.exit(1)
    
    # Run prediction
    predictions = model.predict(img_array, verbose=0)
    confidence = np.max(predictions[0])
    predicted_index = np.argmax(predictions[0])
    predicted_class = class_names[predicted_index]
    
    print("\n" + "="*60)
    print(" " * 20 + "PREDICTION RESULTS")
    print("="*60)
    
    # Requirement 7: Confidence check
    if confidence < 0.60:
        print(f"\nPredicted Plant: {predicted_class}")
        print(f"Confidence:      {confidence * 100:.1f}%")
        print("\n! Low confidence - please retake the photo.")
        print("\n" + "="*60)
        sys.exit(0)
        
    # Get botanical details
    info = match_class_name(predicted_class, plant_info)
    
    if info:
        print(f"\nPlant Name:      {info['common_name']}")
        print(f"Confidence:      {confidence * 100:.1f}%\n")
        print("-" * 60)
        print(f"Scientific Name:       {info['scientific_name']}")
        print(f"Family:                {info['family']}")
        print(f"Native Region:         {info['native_region']}")
        print(f"Conservation Status:   {info['conservation_status']}")
        print(f"\nEcological Importance:\n{info['ecological_importance']}")
    else:
        # Fallback if the folder name somehow wasn't in the CSV
        print(f"\nPredicted Folder: {predicted_class}")
        print(f"Confidence:       {confidence * 100:.1f}%")
        print("\nNote: Botanical information not found in plant_info.csv.")
        
    print("\n" + "="*60)

if __name__ == "__main__":
    main()
