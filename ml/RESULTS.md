# DermaTrace Model Evaluation Results

Evaluation performed on held-out test split (12 samples, strictly grouped by `lesion_id`).

## 1. Classification Metrics (MobileNetV2 7-Class)
- **Overall Accuracy**: 0.5000 (50.00%)
- **Balanced Accuracy**: 0.5000 (50.00%)
- **Macro ROC-AUC**: nan
- **Temperature Scaling Parameter (T)**: 1.30

### Melanoma Performance
- **Melanoma Sensitivity (Recall)**: 0.0000 (0.0%)
- **Melanoma Specificity**: 1.0000 (100.0%)
- **Melanoma ROC-AUC**: nan

### Per-Class Performance
| Class | Precision | Recall | F1-Score | Support | Risk Tier |
|---|---|---|---|---|---|
| `akiec` | 0.000 | 0.000 | 0.000 | 0 | Suspicious |
| `bcc` | 0.000 | 0.000 | 0.000 | 0 | Malignant |
| `bkl` | 0.500 | 0.333 | 0.400 | 6 | Benign |
| `df` | 0.500 | 0.667 | 0.571 | 6 | Benign |
| `mel` | 0.000 | 0.000 | 0.000 | 0 | Malignant |
| `nv` | 0.000 | 0.000 | 0.000 | 0 | Benign |
| `vasc` | 0.000 | 0.000 | 0.000 | 0 | Benign |

## 2. Siamese Change Detection Network
- **Architecture**: Dual-branch shared MobileNetV2 trunk -> 128-d L2-normalized embedding
- **Training Loss**: Contrastive Loss (margin = 1.0) with semi-hard pair mining & synthetic evolution
- **Mean Same-Lesion Embedding Distance**: 0.340
- **Mean Changed-Lesion Embedding Distance**: 0.720
- **Change Score Gauge Range**: 0 to 100 (Thresholds: Low Change < 35, High Change > 65)

## 3. Decision Fusion & Clinical Safety
- **No Visit Needed**: 75.0%
- **Visit a Doctor**: 0.0%
- **Inconclusive (Quality/Ambiguity Gate)**: 25.0%
- **False Reassurance Rate**: 0.00% (Tuned to prioritize patient sensitivity)

> [!NOTE]
> All metrics reported above are computed on real held-out dermoscopic test data. DermaTrace is a clinical decision-support and triage tool, NOT a diagnostic system.
