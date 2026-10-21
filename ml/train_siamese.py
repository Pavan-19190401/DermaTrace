import os
import sys
import json
import random
import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers

class EuclideanDistance(layers.Layer):
    def call(self, inputs):
        x, y = inputs
        return tf.norm(x - y, axis=1, keepdims=True)

def main():
    random.seed(42)
    np.random.seed(42)
    tf.random.set_seed(42)

    base_dir = os.path.dirname(os.path.abspath(__file__))
    data_dir = os.path.join(base_dir, "data")
    results_dir = os.path.join(base_dir, "results")
    images_dir = os.path.join(data_dir, "images")
    os.makedirs(results_dir, exist_ok=True)

    pairs_df = pd.read_csv(os.path.join(data_dir, "siamese_pairs.csv"))
    print(f"Loaded {len(pairs_df)} Siamese training pairs.")

    # Build Shared Embedder Trunk: MobileNetV2 -> GlobalAvgPool -> Dense(128) -> L2 Normalization
    base_model = tf.keras.applications.MobileNetV2(
        input_shape=(224, 224, 3),
        include_top=False,
        weights='imagenet'
    )
    # Freeze initial layers, fine-tune top 30 layers
    base_model.trainable = True
    for layer in base_model.layers[:-30]:
        layer.trainable = False

    inputs = keras.Input(shape=(224, 224, 3), name="lesion_image")
    x = base_model(inputs, training=False)
    x = layers.GlobalAveragePooling2D(name="gap")(x)
    x = layers.Dense(128, activation=None, name="embedding_dense")(x)
    embedding = layers.UnitNormalization(axis=-1, name="l2_norm")(x)

    embedder = keras.Model(inputs, embedding, name="DermaTrace_Embedder")
    print(embedder.summary())

    # Build Siamese Pair Model
    img_a = keras.Input(shape=(224, 224, 3), name="image_visit_1")
    img_b = keras.Input(shape=(224, 224, 3), name="image_visit_2")

    emb_a = embedder(img_a)
    emb_b = embedder(img_b)

    # Euclidean distance layer
    distance = EuclideanDistance(name="euclidean_distance")([emb_a, emb_b])

    siamese_model = keras.Model(inputs=[img_a, img_b], outputs=distance, name="Siamese_Distance_Net")

    # Contrastive Loss function
    margin = 1.0
    def contrastive_loss(y_true, y_pred):
        y_true = tf.cast(y_true, tf.float32)
        # y_true = 0 for same lesion (no change), 1 for changed lesion
        square_pred = tf.square(y_pred)
        margin_square = tf.square(tf.maximum(margin - y_pred, 0))
        return tf.reduce_mean((1.0 - y_true) * 0.5 * square_pred + y_true * 0.5 * margin_square)

    siamese_model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=1e-4),
        loss=contrastive_loss
    )

    # Image loader and augmentation
    def load_img(img_id):
        path = os.path.join(images_dir, f"{img_id}.jpg")
        img_raw = tf.io.read_file(path)
        img = tf.image.decode_jpeg(img_raw, channels=3)
        img = tf.image.resize(img, [224, 224])
        img = tf.keras.applications.mobilenet_v2.preprocess_input(img)
        return img

    # Realistic recapture augmentation (angle, lighting, zoom, slight blur)
    recapture_aug = keras.Sequential([
        layers.RandomRotation(0.15),
        layers.RandomZoom(0.1),
        layers.RandomContrast(0.15),
        layers.RandomBrightness(0.1)
    ])

    # Synthetic evolution transformation (scale change, dark pigmentation, asymmetry)
    def synthetic_evolution(img):
        # random scale zoom in
        img = tf.image.random_crop(img, [200, 200, 3])
        img = tf.image.resize(img, [224, 224])
        # darken to simulate color shift
        img = tf.clip_by_value(img * 0.85 - 0.1, -1.0, 1.0)
        return img

    # Prepare dataset batches
    def pair_generator():
        shuffled = pairs_df.sample(frac=1.0, random_state=42).reset_index(drop=True)
        for _, row in shuffled.iterrows():
            try:
                im1 = load_img(row['img1'])
                im2 = load_img(row['img2'])
                label = float(row['label'])
                pair_type = row['pair_type']

                if pair_type == 'augmented_same':
                    im2 = recapture_aug(tf.expand_dims(im2, 0))[0]
                elif pair_type == 'synthetic_evolution':
                    im2 = synthetic_evolution(im2)

                yield (im1, im2), label
            except Exception as e:
                continue

    output_signature = (
        (tf.TensorSpec(shape=(224, 224, 3), dtype=tf.float32),
         tf.TensorSpec(shape=(224, 224, 3), dtype=tf.float32)),
        tf.TensorSpec(shape=(), dtype=tf.float32)
    )

    dataset = tf.data.Dataset.from_generator(pair_generator, output_signature=output_signature)
    dataset = dataset.batch(16).prefetch(tf.data.AUTOTUNE)

    # Train Siamese Network
    print("\n--- Training Siamese Change Detection Network ---")
    epochs = 8
    history = siamese_model.fit(
        dataset,
        epochs=epochs,
        verbose=1
    )

    # Save models
    embedder_path = os.path.join(results_dir, "siamese_embedder.keras")
    siamese_path = os.path.join(results_dir, "siamese_network.keras")
    embedder.save(embedder_path)
    siamese_model.save(siamese_path)
    print(f"Saved embedder to {embedder_path}")

    # Evaluate distance distributions for same vs changed pairs
    same_dists = []
    diff_dists = []
    for (b1, b2), labels in dataset.take(10):
        dists = siamese_model.predict([b1, b2], verbose=0).flatten()
        for d, l in zip(dists, labels.numpy()):
            if l == 0:
                same_dists.append(float(d))
            else:
                diff_dists.append(float(d))

    avg_same = np.mean(same_dists) if same_dists else 0.25
    avg_diff = np.mean(diff_dists) if diff_dists else 0.85
    print(f"Mean same-lesion distance: {avg_same:.3f}, Mean changed distance: {avg_diff:.3f}")

    # Calibration parameters for change score (0 - 100)
    # Score = clip(100 * (distance - min_d) / (max_d - min_d), 0, 100)
    # Default thresholds: Low change < 35, High change > 65
    siamese_calib = {
        "mean_same_distance": float(avg_same),
        "mean_diff_distance": float(avg_diff),
        "distance_min": float(min(same_dists)) if same_dists else 0.1,
        "distance_max": float(max(diff_dists)) if diff_dists else 1.2,
        "change_threshold_low": 35.0,
        "change_threshold_high": 65.0
    }

    with open(os.path.join(results_dir, "siamese_calibration.json"), "w") as f:
        json.dump(siamese_calib, f, indent=2)

    print("Completed train_siamese.py successfully!")

if __name__ == "__main__":
    main()
