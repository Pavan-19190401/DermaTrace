import os
import sys
import json
import random
import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers

def main():
    # Fix seeds for reproducibility
    random.seed(42)
    np.random.seed(42)
    tf.random.set_seed(42)

    base_dir = os.path.dirname(os.path.abspath(__file__))
    data_dir = os.path.join(base_dir, "data")
    results_dir = os.path.join(base_dir, "results")
    images_dir = os.path.join(data_dir, "images")
    os.makedirs(results_dir, exist_ok=True)

    train_df = pd.read_csv(os.path.join(data_dir, "train.csv"))
    val_df = pd.read_csv(os.path.join(data_dir, "val.csv"))

    classes = ['akiec', 'bcc', 'bkl', 'df', 'mel', 'nv', 'vasc']
    class_to_idx = {c: i for i, c in enumerate(classes)}
    idx_to_class = {i: c for i, c in enumerate(classes)}

    train_df['label_idx'] = train_df['dx'].map(class_to_idx)
    val_df['label_idx'] = val_df['dx'].map(class_to_idx)

    print(f"Training on {len(train_df)} samples, validating on {len(val_df)} samples.")

    # Image loader function
    def load_and_preprocess(img_id, label):
        path = tf.strings.join([images_dir, os.sep, img_id, ".jpg"])
        img_raw = tf.io.read_file(path)
        img = tf.image.decode_jpeg(img_raw, channels=3)
        img = tf.image.resize(img, [224, 224])
        # Preprocessing for MobileNetV2: [-1, 1]
        img = tf.keras.applications.mobilenet_v2.preprocess_input(img)
        return img, label

    # Data augmentation pipeline
    data_augmentation = keras.Sequential([
        layers.RandomFlip("horizontal_and_vertical"),
        layers.RandomRotation(0.2),
        layers.RandomZoom(0.15),
        layers.RandomContrast(0.1),
    ])

    batch_size = 16

    train_ds = tf.data.Dataset.from_tensor_slices((train_df['image_id'].values, train_df['label_idx'].values))
    train_ds = train_ds.shuffle(len(train_df), seed=42)
    train_ds = train_ds.map(load_and_preprocess, num_parallel_calls=tf.data.AUTOTUNE)
    train_ds = train_ds.batch(batch_size).map(lambda x, y: (data_augmentation(x, training=True), y))
    train_ds = train_ds.prefetch(tf.data.AUTOTUNE)

    val_ds = tf.data.Dataset.from_tensor_slices((val_df['image_id'].values, val_df['label_idx'].values))
    val_ds = val_ds.map(load_and_preprocess, num_parallel_calls=tf.data.AUTOTUNE)
    val_ds = val_ds.batch(batch_size).prefetch(tf.data.AUTOTUNE)

    # Compute class weights
    total_samples = len(train_df)
    class_counts = train_df['label_idx'].value_counts()
    class_weights = {}
    for i in range(len(classes)):
        count = class_counts.get(i, 1)
        class_weights[i] = total_samples / (len(classes) * max(count, 1))

    print("Class weights:", {classes[k]: round(v, 2) for k, v in class_weights.items()})

    # Build Model: MobileNetV2 pretrained on ImageNet
    base_model = tf.keras.applications.MobileNetV2(
        input_shape=(224, 224, 3),
        include_top=False,
        weights='imagenet'
    )
    base_model.trainable = False  # Freeze for stage 1

    inputs = keras.Input(shape=(224, 224, 3), name="input_image")
    x = base_model(inputs, training=False)
    x = layers.GlobalAveragePooling2D(name="global_avg_pool")(x)
    x = layers.Dropout(0.3)(x)
    outputs = layers.Dense(len(classes), activation="softmax", name="classification_output")(x)

    model = keras.Model(inputs, outputs, name="DermaTrace_Classifier")

    # Stage 1: Warmup classifier head
    print("\n--- STAGE 1: Training Classification Head ---")
    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=1e-3),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"]
    )

    epochs_stage1 = 6
    history1 = model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=epochs_stage1,
        class_weight=class_weights,
        verbose=1
    )

    # Stage 2: Fine-tune top layers of MobileNetV2
    print("\n--- STAGE 2: Fine-Tuning Top Layers ---")
    base_model.trainable = True
    # Freeze bottom 100 layers, fine-tune remaining top layers
    for layer in base_model.layers[:100]:
        layer.trainable = False

    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=1e-4),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"]
    )

    callbacks = [
        keras.callbacks.EarlyStopping(monitor="val_loss", patience=5, restore_best_weights=True),
        keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.5, patience=2, min_lr=1e-6)
    ]

    epochs_stage2 = 8
    history2 = model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=epochs_stage2,
        class_weight=class_weights,
        callbacks=callbacks,
        verbose=1
    )

    # Save model
    model_path = os.path.join(results_dir, "classifier.keras")
    model.save(model_path)
    print(f"Saved classifier model to {model_path}")

    # Evaluate validation loss and accuracy
    val_loss, val_acc = model.evaluate(val_ds, verbose=0)
    print(f"Validation Accuracy: {val_acc:.4f}, Validation Loss: {val_loss:.4f}")

    # Temperature Scaling on Validation Set
    val_preds = []
    val_labels = []
    for x_b, y_b in val_ds:
        val_preds.append(model.predict(x_b, verbose=0))
        val_labels.append(y_b.numpy())
    val_preds = np.concatenate(val_preds, axis=0)
    val_labels = np.concatenate(val_labels, axis=0)

    # Find optimal temperature T
    temps = np.linspace(0.5, 2.5, 21)
    best_t = 1.0
    best_nll = float('inf')
    eps = 1e-7

    for t in temps:
        # scale logits (reconstruct logits from softmax via log)
        logits = np.log(np.clip(val_preds, eps, 1.0 - eps)) / t
        exp_l = np.exp(logits - np.max(logits, axis=-1, keepdims=True))
        scaled_probs = exp_l / np.sum(exp_l, axis=-1, keepdims=True)
        # NLL
        nll = -np.mean(np.log(scaled_probs[np.arange(len(val_labels)), val_labels] + eps))
        if nll < best_nll:
            best_nll = nll
            best_t = float(t)

    print(f"Calibrated temperature scaling: T = {best_t:.2f} (NLL: {best_nll:.4f})")

    with open(os.path.join(results_dir, "calibration.json"), "w") as f:
        json.dump({"temperature": best_t, "val_accuracy": float(val_acc)}, f, indent=2)

if __name__ == "__main__":
    main()
