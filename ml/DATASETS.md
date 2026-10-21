# DermaTrace Training Datasets

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
