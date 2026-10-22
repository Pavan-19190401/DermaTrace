# DermaTrace 🩺✨

**AI-Powered Dermatological Lesion Tracking & Longitudinal Skin Health Monitoring**

DermaTrace is a modern Android clinical application designed for high-precision skin lesion tracking, real-time capture quality assurance, and longitudinal comparison using on-device machine learning.

---

## 🚀 Key Features

### 1. 📷 Clinical CameraX Scanner & Ghosting Alignment
- **Real-Time Quality Gate**: Evaluates frame sharpness (Laplacian variance), illumination (EV histogram), and reticle framing before capture.
- **Ghosting Alignment**: Semi-transparent baseline overlay lets patients precisely match previous visit angles and distance for accurate longitudinal monitoring.

### 2. 🧠 Dual-Model ML & Clinical Decision Fusion
- **MobileNetV2 / EfficientNet TFLite Classifier**: Real-time benign vs. suspicious lesion categorization.
- **Siamese CNN Architecture**: 64-dimensional feature embedding for visit-to-visit distance metrics and evolution scoring.
- **Rule-Based ABCDE Criteria Fusion**: Clinical safeguard ensuring that low-confidence AI predictions fall back to transparent medical guidance.

### 3. 📱 Patient Experience & Performance
- **Zero-Collision Side-by-Side Pager Transitions**: GPU-composited animations running at up to 120 FPS matching the refresh rate of modern mobile displays.
- **SlideCommit Interactive Saving**: Spring-physics interactive gesture slider for lesion commits.
- **Biometric Security & App Lock**: Hardware-backed biometric authentication (Fingerprint / Face Unlock) for HIPAA-compliant patient data privacy.
- **Offline-First Room Database**: Complete local encrypted persistence for patient records, lesion timeline history, and follow-up visit metadata.

### 4. 🛡️ Jetpack Compose Admin Workspace
- **Clinical Staffing & Rota Board**: Live shift assignments and doctor availability status.
- **System & Model Health**: Real-time telemetry monitoring classifier uptime and analysis latency.
- **User Directory & Access Control**: Role-based access governance and audit logs.

---

## 🛠️ Tech Stack & Architecture

- **Platform**: Android (Min SDK 26, Target SDK 34)
- **Language**: Kotlin, Modern JavaScript / Web Animations API
- **UI Architecture**: Jetpack Compose (Admin Workspace) + Hybrid Embedded Web Architecture (Patient Portal)
- **Camera**: CameraX (ImageCapture, ImageAnalysis, Preview)
- **Machine Learning**: TensorFlow Lite (TFLite)
- **Database**: Android Jetpack Room (SQLite)
- **Security**: AndroidX BiometricPrompt & KeyStore
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`)

---

## 📦 Building the Project

```bash
# Clone the repository
git clone https://github.com/Pavan-19190401/DermaTrace.git
cd DermaTrace

# Build debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License
This project is licensed under the Apache License 2.0.
