import os
import sys
import pandas as pd
import numpy as np
import joblib
import shap

# Configure matplotlib for headless / non-interactive environment
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt


def main():
    print("============================================================")
    print("   MEDISPHERE COGNITIVE TWIN - DIABETES SHAP EXPLAINABILITY ")
    print("============================================================\n")

    # ============================================================
    # 1. RESOLVE FILE PATHS
    # ============================================================
    # Support execution from either ml-service/ or repository root
    base_dir = "."
    if not os.path.exists("models") and os.path.exists("ml-service/models"):
        base_dir = "ml-service"

    model_path = os.path.join(base_dir, "models", "diabetes_random_forest.pkl")
    features_path = os.path.join(base_dir, "models", "diabetes_features.pkl")
    data_path = os.path.join(base_dir, "data", "diabetes.csv")
    output_dir = os.path.join(base_dir, "explainability")

    os.makedirs(output_dir, exist_ok=True)

    # Check that required model exists
    if not os.path.exists(model_path):
        print(f"Error: Model file not found at {model_path}")
        print("Please train the Diabetes model first using: python prediction/train_diabetes_model.py")
        sys.exit(1)

    if not os.path.exists(data_path):
        print(f"Error: Dataset not found at {data_path}")
        sys.exit(1)

    # ============================================================
    # 2. LOAD MODEL AND FEATURE NAMES
    # ============================================================
    print(f"Loading trained Diabetes model from: {model_path}")
    model = joblib.load(model_path)

    if os.path.exists(features_path):
        feature_names = joblib.load(features_path)
        print(f"Loaded {len(feature_names)} feature names from: {features_path}")
    else:
        # Fallback to model feature names if file not found
        feature_names = list(model.feature_names_in_)
        print(f"Loaded {len(feature_names)} feature names from model attributes.")

    # ============================================================
    # 3. LOAD DATASET AND SELECT PATIENT SAMPLE
    # ============================================================
    print(f"Loading dataset from: {data_path}")
    df = pd.read_csv(data_path)

    # Drop target column if present to get feature matrix X
    target_col = "Outcome"
    if target_col in df.columns:
        X = df.drop(columns=[target_col])[feature_names]
        y = df[target_col]
    else:
        X = df[feature_names]
        y = None

    # Select a patient sample (index 0 by default, or an illustrative sample)
    sample_index = 0
    patient_df = X.iloc[[sample_index]]
    patient_series = X.iloc[sample_index]

    print(f"\nAnalyzing Patient at index: {sample_index}")
    if y is not None:
        print(f"Actual Clinical Outcome (Outcome): {y.iloc[sample_index]}")

    # ============================================================
    # 4. MODEL PREDICTION FOR PATIENT
    # ============================================================
    predicted_class = model.predict(patient_df)[0]
    predicted_probs = model.predict_proba(patient_df)[0]
    diabetes_risk_prob = predicted_probs[1]

    print(f"\nModel Prediction:")
    print(f" - Predicted Class: {predicted_class} ({'High Diabetes Risk' if predicted_class == 1 else 'Low Diabetes Risk'})")
    print(f" - Predicted Risk Probability: {diabetes_risk_prob:.4f} ({diabetes_risk_prob * 100:.2f}%)")

    # ============================================================
    # 5. PRINT PATIENT'S FEATURE VALUES
    # ============================================================
    print("\n------------------------------------------------------------")
    print("PATIENT FEATURE VALUES")
    print("------------------------------------------------------------")
    for feature in feature_names:
        print(f"  {feature:<26}: {patient_series[feature]}")

    # ============================================================
    # 6. INITIALIZE SHAP TREE EXPLAINER
    # ============================================================
    # TreeExplainer calculates exact Shapley values for tree ensembles
    # efficiently using the TreeSHAP algorithm.
    print("\nComputing SHAP values using TreeExplainer...")
    explainer = shap.TreeExplainer(model)
    explanation = explainer(patient_df)

    # In binary classification, SHAP explanations can have 2 output channels
    # (channel 0 for Negative / No Diabetes, channel 1 for Positive / Diabetes).
    # We focus on channel 1 to explain the positive risk probability.
    if len(explanation.shape) == 3 and explanation.shape[2] == 2:
        patient_explanation = explanation[0, :, 1]
        shap_values_class1 = explanation.values[0, :, 1]
        base_value = explanation.base_values[0, 1]
    else:
        patient_explanation = explanation[0]
        shap_values_class1 = explanation.values[0]
        base_value = explanation.base_values[0]

    print(f"Model Base Value (Expected Population Risk): {base_value:.4f}")
    print(f"Patient Predicted Risk Probability:         {diabetes_risk_prob:.4f}")

    # ============================================================
    # 7. FEATURE CONTRIBUTIONS (SHAP VALUES)
    # ============================================================
    shap_summary = pd.DataFrame({
        "Feature": feature_names,
        "Patient Value": [patient_series[f] for f in feature_names],
        "SHAP Contribution": shap_values_class1,
        "Absolute Impact": np.abs(shap_values_class1)
    }).sort_values(by="Absolute Impact", ascending=False).reset_index(drop=True)

    print("\n------------------------------------------------------------")
    print("SHAP FEATURE CONTRIBUTIONS (SORTED BY IMPACT MAGNITUDE)")
    print("------------------------------------------------------------")
    for _, row in shap_summary.iterrows():
        sign = "+" if row["SHAP Contribution"] >= 0 else "-"
        direction = "Increases Risk" if row["SHAP Contribution"] >= 0 else "Decreases Risk"
        print(f"  {row['Feature']:<26} = {row['Patient Value']:<8} | SHAP: {sign}{abs(row['SHAP Contribution']):.4f} ({direction})")

    # ============================================================
    # 8. GROUP BY RISK DIRECTION (INCREASING VS DECREASING)
    # ============================================================
    increasing_risk = shap_summary[shap_summary["SHAP Contribution"] > 0]
    decreasing_risk = shap_summary[shap_summary["SHAP Contribution"] < 0]

    print("\n------------------------------------------------------------")
    print("FACTORS INCREASING DIABETES RISK FOR THIS PATIENT:")
    print("------------------------------------------------------------")
    if len(increasing_risk) == 0:
        print("  None (all measured features contribute favorably)")
    else:
        for _, row in increasing_risk.iterrows():
            print(f"  [+] {row['Feature']:<24} ({row['Patient Value']:<6}): +{row['SHAP Contribution']:.4f} toward higher risk")

    print("\n------------------------------------------------------------")
    print("FACTORS DECREASING DIABETES RISK FOR THIS PATIENT:")
    print("------------------------------------------------------------")
    if len(decreasing_risk) == 0:
        print("  None (no protective factors observed)")
    else:
        for _, row in decreasing_risk.iterrows():
            print(f"  [-] {row['Feature']:<24} ({row['Patient Value']:<6}): {row['SHAP Contribution']:.4f} toward lower risk")

    # ============================================================
    # 9. GENERATE AND SAVE SHAP VISUALIZATIONS
    # ============================================================
    print("\nGenerating SHAP plots...")

    # Plot 1: Patient-level Waterfall Plot
    waterfall_path = os.path.join(output_dir, "diabetes_shap_waterfall.png")
    plt.figure(figsize=(10, 6))
    shap.plots.waterfall(patient_explanation, max_display=8, show=False)
    plt.title(f"SHAP Waterfall: Diabetes Risk for Patient #{sample_index}", fontsize=12, pad=15)
    plt.tight_layout()
    plt.savefig(waterfall_path, dpi=300, bbox_inches="tight")
    plt.close()
    print(f"Saved patient waterfall plot: {waterfall_path}")

    # Plot 2: Patient-level Bar Plot
    bar_path = os.path.join(output_dir, "diabetes_shap_bar.png")
    plt.figure(figsize=(10, 6))
    shap.plots.bar(patient_explanation, max_display=8, show=False)
    plt.title(f"SHAP Feature Attribution: Patient #{sample_index}", fontsize=12, pad=15)
    plt.tight_layout()
    plt.savefig(bar_path, dpi=300, bbox_inches="tight")
    plt.close()
    print(f"Saved patient bar plot:       {bar_path}")

    # Plot 3: Global Cohort Summary Plot (first 100 samples for overview)
    summary_path = os.path.join(output_dir, "diabetes_shap_summary.png")
    cohort_size = min(100, len(X))
    cohort_df = X.iloc[:cohort_size]
    cohort_explanation = explainer(cohort_df)

    if len(cohort_explanation.shape) == 3 and cohort_explanation.shape[2] == 2:
        cohort_explanation_class1 = cohort_explanation[:, :, 1]
    else:
        cohort_explanation_class1 = cohort_explanation

    plt.figure(figsize=(10, 6))
    shap.plots.beeswarm(cohort_explanation_class1, max_display=8, show=False)
    plt.title(f"SHAP Beeswarm Summary (Cohort of {cohort_size} Patients)", fontsize=12, pad=15)
    plt.tight_layout()
    plt.savefig(summary_path, dpi=300, bbox_inches="tight")
    plt.close()
    print(f"Saved cohort summary plot:   {summary_path}")

    print("\n============================================================")
    print("Diabetes SHAP Explainability analysis completed successfully!")
    print("============================================================\n")


if __name__ == "__main__":
    main()
