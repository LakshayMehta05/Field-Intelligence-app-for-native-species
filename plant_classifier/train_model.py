import os
import json
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns

try:
    import tensorflow as tf
    from tensorflow.keras.applications import MobileNetV2
    from tensorflow.keras import layers, models
    from sklearn.metrics import classification_report, confusion_matrix
except ImportError:
    print("TensorFlow or required libraries are not installed.")
    print("Please install them using: pip install tensorflow scikit-learn matplotlib seaborn")
    import sys
    sys.exit(1)

def build_and_train_model():
    dataset_dir = 'dataset_split'
    train_dir = os.path.join(dataset_dir, 'train')
    val_dir = os.path.join(dataset_dir, 'val')
    test_dir = os.path.join(dataset_dir, 'test')
    
    if not os.path.exists(train_dir):
        print(f"Error: Could not find '{train_dir}'. Please ensure dataset_split is prepared.")
        return
        
    img_size = (224, 224)
    batch_size = 16 # Small batch size to run comfortably on a laptop
    
    print("Loading datasets...")
    # Load training dataset
    train_ds = tf.keras.utils.image_dataset_from_directory(
        train_dir,
        shuffle=True,
        image_size=img_size,
        batch_size=batch_size
    )
    
    # Load validation dataset
    val_ds = tf.keras.utils.image_dataset_from_directory(
        val_dir,
        shuffle=True,
        image_size=img_size,
        batch_size=batch_size
    )
    
    # Load test dataset (shuffle=False is required for matching predictions to true labels)
    test_ds = tf.keras.utils.image_dataset_from_directory(
        test_dir,
        shuffle=False,
        image_size=img_size,
        batch_size=batch_size
    )
    
    class_names = train_ds.class_names
    num_classes = len(class_names)
    print(f"Detected {num_classes} classes: {class_names}")
    
    # 10. Save the class-name mapping as class_names.json
    with open('class_names.json', 'w') as f:
        json.dump(class_names, f)
    print("Saved class mapping to 'class_names.json'")
    
    # 5. Apply reasonable training augmentation
    data_augmentation = tf.keras.Sequential([
        layers.RandomFlip('horizontal'),
        layers.RandomRotation(0.2),
        layers.RandomZoom(0.2),
    ])
    
    # MobileNetV2 preprocessing (scales pixels to [-1, 1])
    preprocess_input = tf.keras.applications.mobilenet_v2.preprocess_input
    
    # 1. Use transfer learning with MobileNetV2
    base_model = MobileNetV2(
        input_shape=(224, 224, 3),
        include_top=False,
        weights='imagenet'
    )
    base_model.trainable = False # Freeze the base model to use it as a feature extractor
    
    # Build full model
    inputs = tf.keras.Input(shape=(224, 224, 3))
    x = data_augmentation(inputs)
    x = preprocess_input(x)
    x = base_model(x, training=False)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dropout(0.2)(x) # Dropout to prevent overfitting
    outputs = layers.Dense(num_classes, activation='softmax')(x)
    
    model = models.Model(inputs, outputs)
    
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
        loss=tf.keras.losses.SparseCategoricalCrossentropy(),
        metrics=['accuracy']
    )
    
    print("\nModel Summary:")
    model.summary()
    
    # 6. Train using the train set and validate using val
    epochs = 10
    print("\nStarting training for 10 epochs...")
    history = model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=epochs
    )
    
    # 9. Save the trained model
    print("\nSaving model...")
    model.save('plant_classifier.keras')
    print("Model saved to 'plant_classifier.keras'")
    
    # 7 & 8. Evaluate on the test set and print accuracy
    print("\nEvaluating on the test set...")
    loss, accuracy = model.evaluate(test_ds)
    print(f"Test Accuracy: {accuracy * 100:.2f}%")
    
    # Get predictions for detailed metrics and confusion matrix
    print("\nGenerating predictions for test set...")
    y_true = []
    y_pred = []
    
    for images, labels in test_ds:
        preds = model.predict(images, verbose=0)
        y_pred.extend(np.argmax(preds, axis=1))
        y_true.extend(labels.numpy())
        
    y_true = np.array(y_true)
    y_pred = np.array(y_pred)
    
    # Per-class performance
    print("\n--- Per-Class Performance (Classification Report) ---")
    print(classification_report(y_true, y_pred, target_names=class_names))
    
    # 11. Generate a confusion matrix
    cm = confusion_matrix(y_true, y_pred)
    plt.figure(figsize=(10, 8))
    sns.heatmap(cm, annot=True, fmt='d', cmap='Blues', xticklabels=class_names, yticklabels=class_names)
    plt.title('Confusion Matrix on Test Set')
    plt.ylabel('True Label')
    plt.xlabel('Predicted Label')
    plt.xticks(rotation=45, ha='right')
    plt.tight_layout()
    plt.savefig('confusion_matrix.png')
    print("Confusion matrix saved to 'confusion_matrix.png'")

if __name__ == '__main__':
    build_and_train_model()
