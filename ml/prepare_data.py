import os
import sys
import random
import time
import subprocess
import pandas as pd
import numpy as np
from concurrent.futures import ThreadPoolExecutor, as_completed

def main():
    random.seed(42)
    np.random.seed(42)

    data_dir = os.path.join(os.path.dirname(__file__), "data")
    images_dir = os.path.join(data_dir, "images")
    os.makedirs(images_dir, exist_ok=True)

    csv_path = os.path.join(data_dir, "HAM10000_metadata.csv")
    if not os.path.exists(csv_path):
        print(f"Error: metadata CSV not found at {csv_path}")
        sys.exit(1)

    df = pd.read_csv(csv_path)
    print(f"Loaded HAM10000 metadata: {len(df)} records across {df['lesion_id'].nunique()} unique lesions.")

    # Select stratified sample to balance classes and include multi-image lesions
    # 7 classes: akiec, bcc, bkl, df, mel, nv, vasc
    classes = ['akiec', 'bcc', 'bkl', 'df', 'mel', 'nv', 'vasc']
    
    # 1. First pick lesions with multiple images (for Siamese natural pairs)
    multi_lesions = df['lesion_id'].value_counts()
    multi_lesions = multi_lesions[multi_lesions >= 2].index.tolist()
    
    selected_lesion_ids = set()
    # Sample multi-image lesions across classes
    for cls in classes:
        cls_multi = df[(df['lesion_id'].isin(multi_lesions)) & (df['dx'] == cls)]['lesion_id'].unique().tolist()
        sample_size = min(25, len(cls_multi))
        if sample_size > 0:
            selected_lesion_ids.update(random.sample(cls_multi, sample_size))

    # 2. Pick single-image lesions to balance classes
    # Target ~35-50 images per class for a fast, robust local training run
    target_per_class = {
        'mel': 50,
        'bcc': 45,
        'akiec': 40,
        'bkl': 45,
        'df': 35,
        'vasc': 35,
        'nv': 55
    }

    for cls, target in target_per_class.items():
        curr_count = len(df[df['lesion_id'].isin(selected_lesion_ids) & (df['dx'] == cls)])
        needed = target - curr_count
        if needed > 0:
            cls_pool = df[(~df['lesion_id'].isin(selected_lesion_ids)) & (df['dx'] == cls)]['lesion_id'].unique().tolist()
            sample_size = min(needed, len(cls_pool))
            if sample_size > 0:
                selected_lesion_ids.update(random.sample(cls_pool, sample_size))

    sample_df = df[df['lesion_id'].isin(selected_lesion_ids)].copy().reset_index(drop=True)
    sample_df['img_num'] = sample_df['image_id'].apply(lambda x: int(x.split('_')[1]))
    sample_df['part'] = sample_df['img_num'].apply(lambda x: 'HAM10000_images_part_1' if x < 29306 else 'HAM10000_images_part_2')

    print(f"Selected {len(sample_df)} images across {sample_df['lesion_id'].nunique()} unique lesions.")
    print("Class distribution in selected sample:")
    print(sample_df['dx'].value_counts())

    # Download images concurrently if not already downloaded
    images_to_download = []
    for _, row in sample_df.iterrows():
        img_id = row['image_id']
        dest_file = os.path.join(images_dir, f"{img_id}.jpg")
        if not os.path.exists(dest_file):
            remote_path = f"{row['part']}/{img_id}.jpg"
            images_to_download.append((remote_path, img_id))

    print(f"Images to download: {len(images_to_download)} (already cached: {len(sample_df) - len(images_to_download)})")

    def download_image(item):
        remote_path, img_id = item
        dest_file = os.path.join(images_dir, f"{img_id}.jpg")
        cmd = [
            'kaggle', 'datasets', 'download',
            '-d', 'kmader/skin-cancer-mnist-ham10000',
            '-f', remote_path,
            '-p', images_dir
        ]
        res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        return img_id, os.path.exists(dest_file)

    if images_to_download:
        print("Downloading images in parallel...")
        success = 0
        with ThreadPoolExecutor(max_workers=8) as executor:
            futures = [executor.submit(download_image, item) for item in images_to_download]
            for future in as_completed(futures):
                img_id, ok = future.result()
                if ok:
                    success += 1
                if success % 25 == 0 or success == len(images_to_download):
                    print(f"Downloaded {success}/{len(images_to_download)} images...")

    # Filter to only successfully available images
    valid_mask = sample_df['image_id'].apply(lambda x: os.path.exists(os.path.join(images_dir, f"{x}.jpg")))
    final_df = sample_df[valid_mask].copy().reset_index(drop=True)
    print(f"Final verified images on disk: {len(final_df)}")

    # Split strictly by lesion_id (70% train, 15% val, 15% test)
    lesion_summary = final_df.groupby('lesion_id').first().reset_index()
    
    train_lesions, val_lesions, test_lesions = [], [], []
    for cls in classes:
        cls_lesions = lesion_summary[lesion_summary['dx'] == cls]['lesion_id'].tolist()
        random.shuffle(cls_lesions)
        n = len(cls_lesions)
        n_train = int(round(0.70 * n))
        n_val = int(round(0.15 * n))
        train_lesions.extend(cls_lesions[:n_train])
        val_lesions.extend(cls_lesions[n_train:n_train + n_val])
        test_lesions.extend(cls_lesions[n_train + n_val:])

    train_df = final_df[final_df['lesion_id'].isin(train_lesions)].copy()
    val_df = final_df[final_df['lesion_id'].isin(val_lesions)].copy()
    test_df = final_df[final_df['lesion_id'].isin(test_lesions)].copy()

    def map_risk(dx):
        if dx in ['mel', 'bcc']:
            return 'malignant'
        elif dx in ['akiec']:
            return 'suspicious'
        else:
            return 'benign'

    for d in [train_df, val_df, test_df, final_df]:
        d['risk_tier'] = d['dx'].apply(map_risk)

    train_df.to_csv(os.path.join(data_dir, "train.csv"), index=False)
    val_df.to_csv(os.path.join(data_dir, "val.csv"), index=False)
    test_df.to_csv(os.path.join(data_dir, "test.csv"), index=False)
    final_df.to_csv(os.path.join(data_dir, "all_samples.csv"), index=False)

    print("--- SPLIT SUMMARY ---")
    print(f"Train: {len(train_df)} images ({train_df['lesion_id'].nunique()} lesions)")
    print(f"Val:   {len(val_df)} images ({val_df['lesion_id'].nunique()} lesions)")
    print(f"Test:  {len(test_df)} images ({test_df['lesion_id'].nunique()} lesions)")

    # Generate Siamese pairs
    pairs = []
    
    # 1. Natural same-lesion pairs
    for lesion_id, group in train_df.groupby('lesion_id'):
        img_list = group['image_id'].tolist()
        if len(img_list) >= 2:
            for i in range(len(img_list)):
                for j in range(i + 1, len(img_list)):
                    pairs.append({
                        'img1': img_list[i],
                        'img2': img_list[j],
                        'label': 0,
                        'pair_type': 'natural_same'
                    })

    # 2. Re-capture augmented same-lesion pairs
    for img_id in train_df['image_id'].tolist():
        pairs.append({
            'img1': img_id,
            'img2': img_id,
            'label': 0,
            'pair_type': 'augmented_same'
        })

    # 3. Negative pairs (different lesions)
    all_imgs = train_df['image_id'].tolist()
    all_lesions = train_df['lesion_id'].tolist()
    img_to_lesion = dict(zip(all_imgs, all_lesions))

    pos_count = len(pairs)
    neg_needed = pos_count
    attempts = 0
    while len([p for p in pairs if p['label'] == 1]) < neg_needed and attempts < neg_needed * 10:
        attempts += 1
        i1, i2 = random.sample(all_imgs, 2)
        if img_to_lesion[i1] != img_to_lesion[i2]:
            pairs.append({
                'img1': i1,
                'img2': i2,
                'label': 1,
                'pair_type': 'different_lesions'
            })

    # 4. Synthetic evolution pairs
    for img_id in random.sample(all_imgs, min(len(all_imgs), 50)):
        pairs.append({
            'img1': img_id,
            'img2': img_id,
            'label': 1,
            'pair_type': 'synthetic_evolution'
        })

    pairs_df = pd.DataFrame(pairs)
    pairs_df.to_csv(os.path.join(data_dir, "siamese_pairs.csv"), index=False)
    print(f"Generated {len(pairs_df)} Siamese training pairs.")

    datasets_md = """# DermaTrace Training Datasets

## Source
- **Dataset**: HAM10000 (ISIC 2018 Task 3)
- **Reference**: Tschandl et al., Scientific Data 5:180161 (2018)
- **License**: Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC-SA 4.0)
- **Usage**: Academic, non-commercial clinical decision-support research.

## Classes
1. `akiec` - Actinic keratoses and intraepithelial carcinoma (Pre-cancerous / Suspicious)
2. `bcc` - Basal cell carcinoma (Malignant)
3. `bkl` - Benign keratosis-like lesions (Benign)
4. `df` - Dermatofibroma (Benign)
5. `mel` - Melanoma (Malignant)
6. `nv` - Melanocytic nevi (Benign)
7. `vasc` - Vascular lesions (Benign)

## Splitting Strategy
- **Unit of Splitting**: `lesion_id` (grouped split) to prevent data leakage across visits.
- **Ratios**: 70% Train, 15% Validation, 15% Test.
- **Stratification**: Balanced across 7 diagnostic classes.
- **Random Seed**: 42 (fixed for reproducibility).
"""
    with open(os.path.join(os.path.dirname(__file__), "DATASETS.md"), "w", encoding="utf-8") as f:
        f.write(datasets_md)

    print("Completed prepare_data.py successfully!")

if __name__ == "__main__":
    main()
