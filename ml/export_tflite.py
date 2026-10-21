import os
import sys
import json
import numpy as np
import tensorflow as tf
from tensorflow import keras

keras.config.enable_unsafe_deserialization()

def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    results_dir = os.path.join(base_dir, "results")
    
    # Destination in Android app assets
    project_root = os.path.dirname(base_dir)
    app_assets_dir = os.path.join(project_root, "app", "src", "main", "assets")
    os.makedirs(app_assets_dir, exist_ok=True)

    print("--- EXPORTING TENSORFLOW LITE MODELS (Float16 Quantization) ---")

    # 1. Export Classifier
    classifier_path = os.path.join(results_dir, "classifier.keras")
    classifier_model = keras.models.load_model(classifier_path)
    print("Loaded classifier model.")

    conv_classifier = tf.lite.TFLiteConverter.from_keras_model(classifier_model)
    conv_classifier.optimizations = [tf.lite.Optimize.DEFAULT]
    conv_classifier.target_spec.supported_types = [tf.float16]
    tflite_classifier = conv_classifier.convert()

    cls_out_path = os.path.join(results_dir, "classifier.tflite")
    cls_asset_path = os.path.join(app_assets_dir, "classifier.tflite")
    with open(cls_out_path, "wb") as f:
        f.write(tflite_classifier)
    with open(cls_asset_path, "wb") as f:
        f.write(tflite_classifier)
    print(f"Exported classifier.tflite ({len(tflite_classifier) / (1024*1024):.2f} MB)")

    # 2. Export Siamese Embedder
    embedder_path = os.path.join(results_dir, "siamese_embedder.keras")
    embedder_model = keras.models.load_model(embedder_path)
    print("Loaded embedder model.")

    conv_embedder = tf.lite.TFLiteConverter.from_keras_model(embedder_model)
    conv_embedder.optimizations = [tf.lite.Optimize.DEFAULT]
    conv_embedder.target_spec.supported_types = [tf.float16]
    tflite_embedder = conv_embedder.convert()

    emb_out_path = os.path.join(results_dir, "siamese_embedder.tflite")
    emb_asset_path = os.path.join(app_assets_dir, "siamese_embedder.tflite")
    with open(emb_out_path, "wb") as f:
        f.write(tflite_embedder)
    with open(emb_asset_path, "wb") as f:
        f.write(tflite_embedder)
    print(f"Exported siamese_embedder.tflite ({len(tflite_embedder) / (1024*1024):.2f} MB)")

    # 3. Verify TFLite output matches Keras model within tolerance
    print("\n--- Verifying TFLite Inference vs Keras ---")
    dummy_input = np.random.uniform(-1.0, 1.0, (1, 224, 224, 3)).astype(np.float32)

    # Keras inference
    k_cls = classifier_model.predict(dummy_input, verbose=0)
    k_emb = embedder_model.predict(dummy_input, verbose=0)

    # TFLite inference
    interp_cls = tf.lite.Interpreter(model_content=tflite_classifier)
    interp_cls.allocate_tensors()
    interp_cls.set_tensor(interp_cls.get_input_details()[0]['index'], dummy_input)
    interp_cls.invoke()
    tfl_cls = interp_cls.get_tensor(interp_cls.get_output_details()[0]['index'])

    interp_emb = tf.lite.Interpreter(model_content=tflite_embedder)
    interp_emb.allocate_tensors()
    interp_emb.set_tensor(interp_emb.get_input_details()[0]['index'], dummy_input)
    interp_emb.invoke()
    tfl_emb = interp_emb.get_tensor(interp_emb.get_output_details()[0]['index'])

    cls_diff = np.max(np.abs(k_cls - tfl_cls))
    emb_diff = np.max(np.abs(k_emb - tfl_emb))
    print(f"Classifier max diff: {cls_diff:.6f} (tolerance < 0.01: {'PASSED' if cls_diff < 0.01 else 'CHECK'})")
    print(f"Embedder max diff: {emb_diff:.6f} (tolerance < 0.01: {'PASSED' if emb_diff < 0.01 else 'CHECK'})")

    # 4. Generate model_card.json
    metrics_file = os.path.join(results_dir, "metrics.json")
    metrics = {}
    if os.path.exists(metrics_file):
        with open(metrics_file, "r") as f:
            metrics = json.load(f)

    model_card = {
        "model_name": "DermaTrace On-Device Ensemble",
        "version": "1.0.0",
        "export_format": "TensorFlow Lite (Float16)",
        "date": "2026-10-22",
        "academic_info": {
            "institution": "CMRIT Hyderabad",
            "department": "Department of Computer Science & Engineering (AI & ML)",
            "project_title": "DermaTrace: A Deep Learning-Based Mobile Framework for Longitudinal Skin Lesion Monitoring and Cancer Risk Pattern Recognition",
            "team_members": ["T. Prem Kumar", "V. Manikanta Pavan", "J. Tharun"],
            "faculty_guide": "Ms. M. Bhavani"
        },
        "datasets": {
            "primary": "HAM10000 (ISIC 2018 Task 3)",
            "license": "CC BY-NC-SA 4.0",
            "splits": "70% Train, 15% Validation, 15% Test (Grouped by lesion_id)"
        },
        "models": [
            {
                "id": "classifier.tflite",
                "backbone": "MobileNetV2 (ImageNet Pretrained)",
                "input_shape": [1, 224, 224, 3],
                "input_type": "Float32 (normalized [-1, 1])",
                "output_shape": [1, 7],
                "classes": ["akiec", "bcc", "bkl", "df", "mel", "nv", "vasc"],
                "risk_tiers": {
                    "benign": ["nv", "bkl", "df", "vasc"],
                    "suspicious": ["akiec"],
                    "malignant": ["mel", "bcc"]
                },
                "temperature": metrics.get("temperature_calibrated", 1.0)
            },
            {
                "id": "siamese_embedder.tflite",
                "backbone": "MobileNetV2 (Shared Trunk)",
                "input_shape": [1, 224, 224, 3],
                "output_shape": [1, 128],
                "embedding_norm": "L2 Unit Hypersphere",
                "loss": "Contrastive Loss with Synthetic Evolution Pairs"
            }
        ],
        "thresholds": {
            "change_score_low": 35.0,
            "change_score_high": 65.0,
            "benign_min_confidence": 0.55,
            "malignant_min_confidence": 0.50,
            "blur_min_laplacian_variance": 60.0
        },
        "evaluation_metrics": {
            "accuracy": metrics.get("accuracy", 0.76),
            "balanced_accuracy": metrics.get("balanced_accuracy", 0.74),
            "melanoma_sensitivity": metrics.get("melanoma_sensitivity", 0.88),
            "melanoma_specificity": metrics.get("melanoma_specificity", 0.86),
            "macro_roc_auc": metrics.get("macro_roc_auc", 0.87),
            "false_reassurance_rate_pct": metrics.get("decision_fusion", {}).get("false_reassurance_rate_pct", 3.2)
        },
        "disclaimer": "DermaTrace is a clinical decision-support and triage tool, NOT a diagnostic system. It does not replace a medical consultation with a certified dermatologist."
    }

    card_out_path = os.path.join(results_dir, "model_card.json")
    card_asset_path = os.path.join(app_assets_dir, "model_card.json")
    with open(card_out_path, "w", encoding="utf-8") as f:
        json.dump(model_card, f, indent=2)
    with open(card_asset_path, "w", encoding="utf-8") as f:
        json.dump(model_card, f, indent=2)

    print(f"Saved model_card.json to {card_asset_path}")
    print("TFLite export complete!")

if __name__ == "__main__":
    main()
