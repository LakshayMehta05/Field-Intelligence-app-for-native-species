# Plant Classification System 🌿

This project features an AI-powered image classification system built to identify native plants. Designed for a hackathon, it provides rapid, automated plant identification alongside scientifically accurate botanical data.

## 🎯 What This Model Does
The system accepts an image of a plant as input, preprocesses it, and runs it through a trained deep learning model to predict its species. Upon a confident prediction, it outputs detailed botanical information—including the scientific name, family, native region, and ecological importance—by matching the prediction against a structured CSV database.

## 🪴 Supported Classes
The model currently recognizes **8** plant classes:
1. Aloe Vera (`aloe_vera`)
2. Crown of Thorns (`euphorbia_milii`)
3. Frangipani (`frangipani`)
4. Gulmohar (`gulmohar`)
5. Indian Pennywort (`Indian_pennywort`)
6. Lantana (`lantana`)
7. Neem (`neem`)
8. Passionflower (`passiflora`)

## 📊 Dataset Details
- **Total Size:** 227 valid images
- **Data Cleaning:** 2 exact duplicate files were automatically detected via MD5 hashing and removed prior to training.
- **Split Ratio:** The dataset is strictly partitioned into an **80/10/10** split for Training, Validation, and Testing respectively, ensuring an unbiased evaluation of the model.

## 🧠 Model Architecture & Performance
- **Architecture:** The classifier leverages transfer learning using **MobileNetV2** (pre-trained on ImageNet). The base layers are frozen to act as a robust feature extractor, with a custom dense classification head trained specifically on our dataset.
- **Input Size:** Images are scaled to 224x224 pixels.
- **Test Accuracy:** The model achieves a **78.57%** accuracy on the held-out test set.

## 🚀 How to Run a Prediction

To classify a new image, simply execute the `predict.py` script via the command line, providing the path to your image:

```bash
python predict.py path/to/your/image.jpg
```

### Required Files for Prediction
Ensure the following files are present in the same directory before running inference:
1. `predict.py` (The main inference script)
2. `plant_classifier.keras` (The trained MobileNetV2 model weights)
3. `class_names.json` (The JSON mapping array of model output indices to folder names)
4. `plant_info.csv` (The botanical database containing scientific and ecological information)

### ⚠️ Confidence Thresholding
To ensure reliability, the system features a strict confidence threshold. 
**If the model's prediction confidence is below 60%, it treats the prediction as unreliable.** 
Instead of providing potentially incorrect botanical information, the application will output a warning: `! Low confidence - please retake the photo.`

### 📝 Expected Output (Example)
```text
============================================================
                    PREDICTION RESULTS
============================================================

Plant Name:      Lantana
Confidence:      72.9%

------------------------------------------------------------
Scientific Name:       Lantana camara
Family:                Verbenaceae
Native Region:         Central and South America
Conservation Status:   Least Concern

Ecological Importance:
Flowers provide abundant nectar for butterflies, though it forms dense thickets that can become highly invasive outside its native range.

============================================================
```
