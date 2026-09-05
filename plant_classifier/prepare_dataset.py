import os
import hashlib
import shutil
import random
import csv
from pathlib import Path
from PIL import Image

def get_file_hash(filepath):
    """Generate MD5 hash of a file to detect duplicates."""
    hasher = hashlib.md5()
    with open(filepath, 'rb') as f:
        # Read the file in chunks to handle potentially large files
        for chunk in iter(lambda: f.read(4096), b""):
            hasher.update(chunk)
    return hasher.hexdigest()

def is_valid_image(filepath):
    """Check if the image file is readable and not corrupted."""
    try:
        with Image.open(filepath) as img:
            img.verify() # verify() checks for truncation and corruption
        return True
    except Exception:
        return False

def process_dataset(input_dir="dataset", output_dir="dataset_split", seed=42):
    """Process the dataset, remove duplicates/corrupted, and split into train/val/test."""
    # Set a fixed random seed for reproducibility
    random.seed(seed)
    
    input_path = Path(input_dir)
    output_path = Path(output_dir)
    
    if not input_path.exists():
        print(f"Error: Input directory '{input_dir}' does not exist.")
        print("Please ensure your image folders are inside the 'dataset' directory.")
        return

    # Create output base directories
    for split in ['train', 'val', 'test']:
        (output_path / split).mkdir(parents=True, exist_ok=True)
        
    classes = [d.name for d in input_path.iterdir() if d.is_dir()]
    if not classes:
        print(f"No class directories found in '{input_dir}'.")
        return
    
    summary_data = []
    total_duplicates = 0
    total_corrupted = 0
    total_valid_processed = 0
    
    print(f"Found {len(classes)} classes. Starting processing...")
    
    for cls in classes:
        print(f"Processing class: {cls}...")
        cls_dir = input_path / cls
        images = []
        
        valid_exts = {'.jpg', '.jpeg', '.png'}
        
        # Recursively read all files with valid extensions
        for f in cls_dir.rglob('*'):
            if f.is_file() and f.suffix.lower() in valid_exts:
                images.append(f)
                
        # 1. Remove exact duplicate files based on file hash
        seen_hashes = set()
        unique_images = []
        for img_path in images:
            h = get_file_hash(img_path)
            if h in seen_hashes:
                total_duplicates += 1
            else:
                seen_hashes.add(h)
                unique_images.append(img_path)
                
        # 2. Check for corrupted/unreadable images
        valid_images = []
        for img_path in unique_images:
            if is_valid_image(img_path):
                valid_images.append(img_path)
            else:
                total_corrupted += 1
                
        total_valid = len(valid_images)
        if total_valid == 0:
            print(f"  Warning: No valid images found for class '{cls}'. Skipping.")
            continue
            
        # 3. Create dataset_split/ with train, val, and test folders (80/10/10 split)
        random.shuffle(valid_images)
        
        train_end = int(total_valid * 0.8)
        val_end = train_end + int(total_valid * 0.1)
        
        train_imgs = valid_images[:train_end]
        val_imgs = valid_images[train_end:val_end]
        test_imgs = valid_images[val_end:]
        
        # Create class subfolders in each split
        for split in ['train', 'val', 'test']:
            (output_path / split / cls).mkdir(parents=True, exist_ok=True)
            
        # 4. Copy the images rather than modifying the originals
        for img_list, split in zip([train_imgs, val_imgs, test_imgs], ['train', 'val', 'test']):
            for img in img_list:
                shutil.copy2(img, output_path / split / cls / img.name)
                
        summary_data.append({
            'class_name': cls,
            'total_images': total_valid,
            'train_count': len(train_imgs),
            'val_count': len(val_imgs),
            'test_count': len(test_imgs)
        })
        
        total_valid_processed += total_valid
        
    if not summary_data:
        print("No valid images were processed across all classes.")
        return
        
    # 5. Generate dataset_summary.csv
    csv_path = 'dataset_summary.csv'
    with open(csv_path, 'w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=['class_name', 'total_images', 'train_count', 'val_count', 'test_count'])
        writer.writeheader()
        writer.writerows(summary_data)
        
    # 6. Print a final summary of the dataset
    print("\n--- Dataset Processing Summary ---")
    print(f"Total duplicates removed: {total_duplicates}")
    print(f"Total corrupted files removed: {total_corrupted}")
    print(f"Total valid images processed: {total_valid_processed}")
    print("\nClass-wise split:")
    print(f"{'Class Name':<20} | {'Total':<8} | {'Train':<8} | {'Val':<8} | {'Test':<8}")
    print("-" * 65)
    for row in summary_data:
        print(f"{row['class_name']:<20} | {row['total_images']:<8} | {row['train_count']:<8} | {row['val_count']:<8} | {row['test_count']:<8}")
    print(f"\nProcessing complete! Output saved to '{output_dir}'. Summary saved to '{csv_path}'.")

if __name__ == '__main__':
    # You can change the input_dir here if your dataset is named differently
    process_dataset(input_dir="dataset", output_dir="dataset_split")
