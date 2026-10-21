# DermaTrace Machine Learning Pipeline

## Overview
DermaTrace is a deep-learning mobile framework for longitudinal skin lesion monitoring and cancer risk pattern recognition. It runs **100% on-device** using TensorFlow Lite.

## Pipeline Architecture
1. **Classifier Model (`classifier.tflite`)**:
   - MobileNetV2 backbone (ImageNet pre-trained), input shape `[1, 224, 224, 3]`.
   - 7 diagnostic classes from HAM10000:
     - `mel`: Melanoma (Malignant)
     - `bcc`: Basal Cell Carcinoma (Malignant)
     - `akiec`: Actinic Keratoses (Suspicious)
     - `bkl`: Benign Keratosis (Benign)
     - `df`: Dermatofibroma (Benign)
     - `nv`: Melanocytic Nevi (Benign)
     - `vasc`: Vascular Lesion (Benign)
   - Temperature scaling calibrated on validation set for reliable posterior probabilities.

2. **Siamese Change Detection Network (`siamese_embedder.tflite`)**:
   - Shared-weight MobileNetV2 trunk mapping lesions to 128-dimensional L2-normalized embeddings.
   - Trained using Contrastive Loss on:
     - Natural same-lesion pairs (`lesion_id` matching).
     - Realistic re-capture augmentation (lighting, angle, camera distance).
     - Synthetic evolution (controlled expansion, border irregularity, pigmentation shift).
   - Generates change score gauge (0 to 100).

3. **ABCDE Explainability Engine**:
   - **A**symmetry: Principal axis mirroring XOR difference.
   - **B**order: Convexity, compactness, and radial variance on segmented mask.
   - **C**olor: RGB / LAB color entropy and variance across lesion regions.
   - **D**iameter: Millimeter calculation calibrated with optional reference marker (e.g. coin / scale sticker).
   - **E**volution: Embedding distance & metric delta vs baseline visit.

4. **Decision Fusion Engine**:
   - **No Visit Needed**: Low change score (< 35) AND confident benign prediction (> 0.55).
   - **Visit a Doctor**: High change score (> 65) OR suspicious/malignant risk tier.
   - **Inconclusive**: Low image quality (blur / poor lighting) or conflicting signals.

## Scripts
- `prepare_data.py`: Downloads stratified HAM10000 sample, enforces grouped split by `lesion_id` (70/15/15), and generates Siamese pairs.
- `train_classifier.py`: Trains 2-stage MobileNetV2 classifier with class weights and temperature calibration.
- `train_siamese.py`: Trains Siamese change detection network with contrastive loss.
- `evaluate.py`: Generates full test set metrics, ROC-AUC, melanoma sensitivity/specificity, and `RESULTS.md`.
- `export_tflite.py`: Quantizes models to Float16 TFLite and generates `model_card.json`.
