"""Preprocessing helpers for the MediSphere ML service.

This service must match the original federated training pipeline exactly.
The training code used StandardScaler fitted on training data only. The scaler
must therefore be saved and loaded again at prediction time.
"""

import pickle
from pathlib import Path

import joblib
import numpy as np
import pandas as pd

BASE_DIR = Path(__file__).resolve().parents[1]
MODEL_DIR = BASE_DIR / "models"


def load_feature_names(model_name: str):
    """Load the saved feature-name list for a model."""
    feature_path = MODEL_DIR / f"{model_name}_features.pkl"
    if not feature_path.exists():
        raise FileNotFoundError(
            f"Missing feature metadata file: {feature_path}. "
            "The training workflow creates this file for the model.")

    with open(feature_path, "rb") as file:
        return pickle.load(file)


def load_preprocessor(model_name: str):
    """Load the saved preprocessor dictionary for the model.

    The saved object contains the fitted StandardScaler and any training-time
    median values required for diabetes preprocessing.
    """
    preprocessor_path = MODEL_DIR / f"{model_name}_preprocessor.joblib"
    if not preprocessor_path.exists():
        raise FileNotFoundError(
            f"Missing saved preprocessor for {model_name}. "
            "The training scaler must be saved before deployment. Example:\n"
            "from sklearn.preprocessing import StandardScaler\n"
            "import joblib\n"
            "scaler = StandardScaler()\n"
            "X_train_scaled = scaler.fit_transform(X_train).astype(np.float32)\n"
            "joblib.dump({'scaler': scaler, 'feature_names': X.columns.tolist()}, 'models/cvd_preprocessor.joblib')"
        )

    return joblib.load(preprocessor_path)


def prepare_model_input(model_name: str, features):
    """Apply the same preprocessing used during training."""
    feature_names = load_feature_names(model_name)
    preprocessor = load_preprocessor(model_name)
    values = np.asarray(features, dtype=np.float32).reshape(1, -1)

    feature_names = preprocessor.get("feature_names", feature_names)
    dataframe = pd.DataFrame(values, columns=feature_names)

    if model_name == "diabetes":
        invalid_zero_cols = ["Glucose", "BloodPressure", "SkinThickness", "Insulin", "BMI"]
        dataframe = dataframe.copy()
        for col in invalid_zero_cols:
            if col in dataframe.columns:
                dataframe[col] = dataframe[col].replace(0, np.nan)

        medians = preprocessor.get("medians", {})
        for col, median_value in medians.items():
            if col in dataframe.columns:
                dataframe[col] = dataframe[col].fillna(float(median_value))

    scaler = preprocessor["scaler"]
    transformed = scaler.transform(dataframe)
    return transformed.astype(np.float32)
