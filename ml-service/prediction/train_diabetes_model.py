import os
import sys
import numpy as np
import pandas as pd
import joblib

from sklearn.model_selection import train_test_split, RandomizedSearchCV
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    roc_auc_score,
    confusion_matrix,
    classification_report
)


def main():
    print("============================================================")
    print("      MEDISPHERE COGNITIVE TWIN - DIABETES RISK MODEL       ")
    print("============================================================\n")

    # ============================================================
    # 1. RESOLVE DATASET AND MODEL PATHS
    # ============================================================
    # Allow running from either ml-service/ or the project root
    data_path = "data/diabetes.csv"
    if not os.path.exists(data_path):
        data_path = os.path.join("ml-service", "data", "diabetes.csv")
    
    if not os.path.exists(data_path):
        print(f"Error: Dataset file not found at {data_path}")
        sys.exit(1)

    models_dir = "models"
    if not os.path.exists(models_dir) and os.path.exists(os.path.join("ml-service", "models")):
        models_dir = os.path.join("ml-service", "models")
    os.makedirs(models_dir, exist_ok=True)

    # ============================================================
    # 2. LOAD DATASET
    # ============================================================
    print(f"Loading dataset from: {data_path}...")
    df = pd.read_csv(data_path)
    print("Dataset loaded successfully!\n")

    print(f"Dataset Shape: {df.shape[0]} rows, {df.shape[1]} columns")
    print(f"Column Names: {list(df.columns)}")
    
    print("\nMissing Values per Column (Standard NaNs):")
    print(df.isnull().sum())

    print("\nTarget Distribution ('Outcome'):")
    outcome_counts = df["Outcome"].value_counts()
    print(outcome_counts)
    print(f"Class 0 (Negative): {outcome_counts.get(0, 0)} ({outcome_counts.get(0, 0)/len(df)*100:.2f}%)")
    print(f"Class 1 (Positive): {outcome_counts.get(1, 0)} ({outcome_counts.get(1, 0)/len(df)*100:.2f}%)")

    # ============================================================
    # 3. VALIDATE DATASET COLUMNS
    # ============================================================
    expected_columns = [
        "Pregnancies",
        "Glucose",
        "BloodPressure",
        "SkinThickness",
        "Insulin",
        "BMI",
        "DiabetesPedigreeFunction",
        "Age",
        "Outcome"
    ]

    print("\nValidating expected columns...")
    missing_cols = [col for col in expected_columns if col not in df.columns]
    if missing_cols:
        raise ValueError(
            f"Dataset validation failed! Missing expected columns: {missing_cols}"
        )
    print("Dataset validation passed! All expected columns are present.")

    # ============================================================
    # 4. SEPARATE FEATURES AND TARGET
    # ============================================================
    X = df.drop(columns=["Outcome"]).copy()
    y = df["Outcome"].copy()
    feature_names = list(X.columns)

    print(f"\nFeatures (X) shape: {X.shape}")
    print(f"Target (y) shape: {y.shape}")

    # ============================================================
    # 5. INSPECT AND HANDLE INVALID MEDICAL ZERO VALUES
    # ============================================================
    # In medical measurements like Glucose, BloodPressure, SkinThickness,
    # Insulin, and BMI, a value of 0 is physiologically invalid and represents
    # missing data. Pregnancies and DiabetesPedigreeFunction can legitimately be 0.
    invalid_zero_cols = ["Glucose", "BloodPressure", "SkinThickness", "Insulin", "BMI"]

    print("\nChecking for physiologically invalid zero values:")
    for col in invalid_zero_cols:
        zero_count = (X[col] == 0).sum()
        print(f" - {col}: {zero_count} invalid zero values")
        # Replace invalid zeros with NaN
        X[col] = X[col].replace(0, np.nan)

    # ============================================================
    # 6. TRAIN / TEST SPLIT (AVOIDING DATA LEAKAGE)
    # ============================================================
    # Splitting into 80% training and 20% test before calculating medians
    # ensures zero information from the test set leaks into training.
    print("\nSplitting data into 80% train and 20% test (stratified by outcome)...")
    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=0.20,
        random_state=42,
        stratify=y
    )

    print(f"Training set: {X_train.shape[0]} samples")
    print(f"Testing set:  {X_test.shape[0]} samples")

    # ============================================================
    # 7. IMPUTE MISSING VALUES USING TRAINING MEDIAN
    # ============================================================
    # Calculate medians ONLY on the training split
    train_medians = X_train.median()
    print("\nTraining set medians computed for imputation:")
    for col in invalid_zero_cols:
        print(f" - {col}: {train_medians[col]:.2f}")

    # Impute missing values for both train and test using training medians
    X_train = X_train.fillna(train_medians)
    X_test = X_test.fillna(train_medians)

    print(f"NaNs remaining in X_train: {X_train.isnull().sum().sum()}")
    print(f"NaNs remaining in X_test:  {X_test.isnull().sum().sum()}")

    # ============================================================
    # 8. BUILD RANDOM FOREST & HYPERPARAMETER TUNING
    # ============================================================
    print("\nConfiguring Random Forest with 5-fold cross-validation...")
    base_rf = RandomForestClassifier(random_state=42)

    param_distributions = {
        "n_estimators": [100, 200, 300, 500],
        "max_depth": [None, 3, 5, 8, 10, 15],
        "min_samples_split": [2, 5, 10],
        "min_samples_leaf": [1, 2, 4],
        "max_features": ["sqrt", "log2", None],
        "class_weight": [None, "balanced", "balanced_subsample"]
    }

    random_search = RandomizedSearchCV(
        estimator=base_rf,
        param_distributions=param_distributions,
        n_iter=30,
        scoring="roc_auc",
        cv=5,
        random_state=42,
        n_jobs=-1,
        verbose=1
    )

    print("Starting RandomizedSearchCV (optimizing for ROC-AUC)...")
    random_search.fit(X_train, y_train)

    best_model = random_search.best_estimator_
    print("\nHyperparameter tuning complete!")
    print("Best Parameters:")
    for param, val in random_search.best_params_.items():
        print(f" - {param}: {val}")
    print(f"Best 5-Fold Cross-Validation ROC-AUC: {random_search.best_score_:.4f}")

    # ============================================================
    # 9. EVALUATE ON UNSEEN TEST SET
    # ============================================================
    y_pred = best_model.predict(X_test)
    y_prob = best_model.predict_proba(X_test)[:, 1]

    accuracy = accuracy_score(y_test, y_pred)
    precision = precision_score(y_test, y_pred)
    recall = recall_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)
    roc_auc = roc_auc_score(y_test, y_prob)
    cm = confusion_matrix(y_test, y_pred)

    print("\n============================================================")
    print("             DIABETES RANDOM FOREST EVALUATION              ")
    print("============================================================")
    print(f"Accuracy:         {accuracy:.4f} ({accuracy * 100:.2f}%)")
    print(f"Precision:        {precision:.4f}")
    print(f"Recall:           {recall:.4f}")
    print(f"F1-score:         {f1:.4f}")
    print(f"ROC-AUC:          {roc_auc:.4f}")

    print("\nConfusion Matrix:")
    print(f"[[TN={cm[0,0]}  FP={cm[0,1]}]")
    print(f" [FN={cm[1,0]}  TP={cm[1,1]}]]")

    print("\nDetailed Classification Report:")
    print(classification_report(y_test, y_pred, digits=4))

    # ============================================================
    # 10. FEATURE IMPORTANCE
    # ============================================================
    feature_importance_df = pd.DataFrame({
        "Feature": feature_names,
        "Importance": best_model.feature_importances_
    }).sort_values(by="Importance", ascending=False).reset_index(drop=True)

    print("Feature Importance (Descending Order):")
    print(feature_importance_df.to_string(index=False))

    # ============================================================
    # 11. SAVE MODEL AND FEATURE NAMES
    # ============================================================
    model_output_path = os.path.join(models_dir, "diabetes_random_forest.pkl")
    features_output_path = os.path.join(models_dir, "diabetes_features.pkl")

    joblib.dump(best_model, model_output_path)
    print(f"\nTrained Diabetes Random Forest model saved to: {model_output_path}")

    joblib.dump(feature_names, features_output_path)
    print(f"Feature names saved in exact training order to: {features_output_path}")

    print("\nNote: This model is developed as a project/demo predictive model")
    print("and is not a clinically validated diagnostic system.")
    print("============================================================\n")


if __name__ == "__main__":
    main()
