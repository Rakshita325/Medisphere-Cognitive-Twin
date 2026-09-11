import pandas as pd
import joblib

from sklearn.model_selection import train_test_split, RandomizedSearchCV
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
    roc_auc_score
)

# ============================================================
# 1. LOAD DATASET
# ============================================================

df = pd.read_csv("data/framingham_cleaned.csv")

print("Dataset loaded successfully!")
print("Dataset shape:", df.shape)


# ============================================================
# 2. SEPARATE FEATURES AND TARGET
# ============================================================

X = df.drop(columns=["TenYearCHD"])
y = df["TenYearCHD"]

print("\nFeatures:", X.shape)
print("Target:", y.shape)

print("\nTarget distribution:")
print(y.value_counts())


# ============================================================
# 3. TRAIN / TEST SPLIT
# ============================================================

X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.20,
    random_state=42,
    stratify=y
)

print("\nTraining data:", X_train.shape)
print("Testing data:", X_test.shape)


# ============================================================
# 4. BASE RANDOM FOREST
# ============================================================

base_model = RandomForestClassifier(
    random_state=42,
    class_weight="balanced",
    n_jobs=-1
)

# ============================================================
# 5. HYPERPARAMETER SEARCH
# ============================================================

param_grid = {
    "n_estimators": [200, 300, 500, 700],
    "max_depth": [None, 5, 8, 10, 15, 20],
    "min_samples_split": [2, 5, 10, 20],
    "min_samples_leaf": [1, 2, 4, 8],
    "max_features": ["sqrt", "log2", None],
    "class_weight": ["balanced", "balanced_subsample"]
}

print("\nStarting Random Forest hyperparameter tuning...")
print("This may take some time...")

search = RandomizedSearchCV(
    estimator=base_model,
    param_distributions=param_grid,
    n_iter=30,
    scoring="f1",
    cv=5,
    random_state=42,
    n_jobs=-1,
    verbose=1
)

search.fit(X_train, y_train)


# ============================================================
# 6. BEST MODEL
# ============================================================

model = search.best_estimator_

print("\nBest parameters:")
print(search.best_params_)

print("\nBest cross-validation F1 score:")
print(round(search.best_score_, 4))


# ============================================================
# 7. PREDICTIONS
# ============================================================

y_pred = model.predict(X_test)

# Probability of class 1
y_probability = model.predict_proba(X_test)[:, 1]


# ============================================================
# 8. MODEL EVALUATION
# ============================================================

accuracy = accuracy_score(y_test, y_pred)
roc_auc = roc_auc_score(y_test, y_probability)

print("\n========================================")
print("IMPROVED CVD RANDOM FOREST RESULTS")
print("========================================")

print("\nAccuracy:")
print(round(accuracy, 4))
print("Accuracy percentage:", round(accuracy * 100, 2), "%")

print("\nROC-AUC:")
print(round(roc_auc, 4))

print("\nClassification Report:")
print(classification_report(y_test, y_pred))

print("\nConfusion Matrix:")
print(confusion_matrix(y_test, y_pred))


# ============================================================
# 9. FEATURE IMPORTANCE
# ============================================================

feature_importance = pd.DataFrame({
    "Feature": X.columns,
    "Importance": model.feature_importances_
})

feature_importance = feature_importance.sort_values(
    by="Importance",
    ascending=False
)

print("\nFeature Importance:")
print(feature_importance)


# ============================================================
# 10. SAVE MODEL
# ============================================================

joblib.dump(
    model,
    "models/cvd_random_forest.pkl"
)

print("\nModel saved successfully!")
print("models/cvd_random_forest.pkl")


# ============================================================
# 11. SAVE FEATURE NAMES
# ============================================================

joblib.dump(
    list(X.columns),
    "models/cvd_features.pkl"
)

print("Feature names saved successfully!")
print("models/cvd_features.pkl")