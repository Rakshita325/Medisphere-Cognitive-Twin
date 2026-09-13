import os
import sys
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import shap
from tensorflow import keras

SCRIPT_DIR = Path(__file__).resolve().parent
ML_SERVICE_DIR = SCRIPT_DIR.parent
if str(ML_SERVICE_DIR) not in sys.path:
    sys.path.insert(0, str(ML_SERVICE_DIR))

from utils.preprocessing import load_feature_names, prepare_model_input


MODEL_DIR = ML_SERVICE_DIR / "models"
OUTPUT_DIR = ML_SERVICE_DIR / "explainability"
OUTPUT_DIR.mkdir(exist_ok=True)


def get_base_value(explainer):
    expected = np.asarray(explainer.expected_value)
    if expected.ndim == 0:
        return float(expected)
    expected = expected.reshape(-1)
    return float(expected[0])


def compute_shap_values(model, model_name, df, feature_names, sample_index=0, cohort_size=100):
    patient_series = df.iloc[sample_index]
    patient_features = patient_series[feature_names].tolist()
    patient_input = prepare_model_input(model_name, patient_features)

    predicted_prob = float(model.predict(patient_input, verbose=0).reshape(-1)[0])
    print(f"\nPatient #{sample_index} raw feature vector loaded from {model_name.upper()} dataset")
    print(f"Model probability (positive class): {predicted_prob:.4f}")

    cohort_df = df[feature_names].head(cohort_size)
    background = np.vstack([
        prepare_model_input(model_name, row.tolist())
        for _, row in cohort_df.iterrows()
    ])

    explainer = shap.DeepExplainer(model, background)
    patient_values = np.asarray(explainer.shap_values(patient_input, check_additivity=False))
    if patient_values.ndim == 3:
        patient_vector = patient_values[0, :, 0]
    else:
        patient_vector = patient_values[0]

    cohort_values = np.asarray(explainer.shap_values(background, check_additivity=False))
    if cohort_values.ndim == 3:
        cohort_vector = cohort_values[:, :, 0]
    else:
        cohort_vector = cohort_values

    patient_exp = shap.Explanation(
        values=patient_vector,
        base_values=get_base_value(explainer),
        data=patient_series[feature_names].to_numpy(dtype=float),
        feature_names=feature_names,
    )

    cohort_exp = shap.Explanation(
        values=cohort_vector,
        base_values=np.full(len(cohort_df), get_base_value(explainer), dtype=float),
        data=cohort_df.to_numpy(dtype=float),
        feature_names=feature_names,
    )

    return patient_exp, cohort_exp, predicted_prob, patient_series


def main():
    print("============================================================")
    print("   MEDISPHERE COGNITIVE TWIN - DIABETES SHAP EXPLAINABILITY ")
    print("============================================================\n")

    model_name = "diabetes"
    model_path = MODEL_DIR / "diabetes_federated_global.keras"
    data_path = ML_SERVICE_DIR / "data" / "diabetes.csv"
    feature_names = load_feature_names(model_name)

    if not model_path.exists():
        raise FileNotFoundError(f"Missing Diabetes model: {model_path}")
    if not data_path.exists():
        raise FileNotFoundError(f"Missing Diabetes dataset: {data_path}")

    print(f"Loading trained Diabetes model from: {model_path}")
    model = keras.models.load_model(str(model_path))
    print(f"Loaded {len(feature_names)} feature names for the active federated model.")

    df = pd.read_csv(data_path)
    if "Outcome" in df.columns:
        df = df.drop(columns=["Outcome"])

    patient_exp, cohort_exp, risk_probability, patient_series = compute_shap_values(
        model=model,
        model_name=model_name,
        df=df,
        feature_names=feature_names,
        sample_index=0,
        cohort_size=min(100, len(df)),
    )

    shap_summary = pd.DataFrame({
        "Feature": feature_names,
        "Patient Value": patient_series[feature_names].to_numpy(dtype=float),
        "SHAP Contribution": patient_exp.values,
        "Absolute Impact": np.abs(patient_exp.values),
    }).sort_values(by="Absolute Impact", ascending=False).reset_index(drop=True)

    print("\n------------------------------------------------------------")
    print("SHAP FEATURE CONTRIBUTIONS (SORTED BY IMPACT MAGNITUDE)")
    print("------------------------------------------------------------")
    for _, row in shap_summary.iterrows():
        sign = "+" if row["SHAP Contribution"] >= 0 else "-"
        direction = "Increases Risk" if row["SHAP Contribution"] >= 0 else "Decreases Risk"
        print(f"  {row['Feature']:<26} = {row['Patient Value']:<8} | SHAP: {sign}{abs(row['SHAP Contribution']):.4f} ({direction})")

    print("\n------------------------------------------------------------")
    print("FACTORS INCREASING DIABETES RISK FOR THIS PATIENT:")
    print("------------------------------------------------------------")
    increasing_risk = shap_summary[shap_summary["SHAP Contribution"] > 0]
    if increasing_risk.empty:
        print("  None (all measured features contribute favorably)")
    else:
        for _, row in increasing_risk.iterrows():
            print(f"  [+] {row['Feature']:<24} ({row['Patient Value']:<6}): +{row['SHAP Contribution']:.4f} toward higher risk")

    print("\n------------------------------------------------------------")
    print("FACTORS DECREASING DIABETES RISK FOR THIS PATIENT:")
    print("------------------------------------------------------------")
    decreasing_risk = shap_summary[shap_summary["SHAP Contribution"] < 0]
    if decreasing_risk.empty:
        print("  None (no protective factors observed)")
    else:
        for _, row in decreasing_risk.iterrows():
            print(f"  [-] {row['Feature']:<24} ({row['Patient Value']:<6}): {row['SHAP Contribution']:.4f} toward lower risk")

    print("\nGenerating SHAP plots...")

    waterfall_path = OUTPUT_DIR / "diabetes_shap_waterfall.png"
    plt.figure(figsize=(10, 6))
    shap.plots.waterfall(patient_exp, max_display=8, show=False)
    plt.title("SHAP Waterfall: Diabetes Risk for Patient #0", fontsize=12, pad=15)
    plt.tight_layout()
    plt.savefig(waterfall_path, dpi=300, bbox_inches="tight")
    plt.close()
    print(f"Saved patient waterfall plot: {waterfall_path}")

    bar_path = OUTPUT_DIR / "diabetes_shap_bar.png"
    plt.figure(figsize=(10, 6))
    shap.plots.bar(patient_exp, max_display=8, show=False)
    plt.title("SHAP Feature Attribution: Patient #0", fontsize=12, pad=15)
    plt.tight_layout()
    plt.savefig(bar_path, dpi=300, bbox_inches="tight")
    plt.close()
    print(f"Saved patient bar plot:       {bar_path}")

    summary_path = OUTPUT_DIR / "diabetes_shap_summary.png"
    plt.figure(figsize=(10, 6))
    shap.plots.beeswarm(cohort_exp, max_display=8, show=False)
    plt.title(f"SHAP Beeswarm Summary (Cohort of {len(cohort_exp.data)} Patients)", fontsize=12, pad=15)
    plt.tight_layout()
    plt.savefig(summary_path, dpi=300, bbox_inches="tight")
    plt.close()
    print(f"Saved cohort summary plot:   {summary_path}")

    print(f"\nPatient positive-class probability: {risk_probability:.4f}")
    print("\n============================================================")
    print("Diabetes SHAP Explainability analysis completed successfully!")
    print("============================================================\n")


if __name__ == "__main__":
    main()
