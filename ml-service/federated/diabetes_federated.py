import os
import sys
from datetime import datetime, timezone
from pathlib import Path

import numpy as np
import pandas as pd

ROOT_DIR = Path(__file__).resolve().parents[1]
if str(ROOT_DIR) not in sys.path:
    sys.path.insert(0, str(ROOT_DIR))
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    roc_auc_score,
    confusion_matrix,
    classification_report
)

from utils.model_registry import (
    get_model_config,
    metadata_file_path,
    model_file_path,
    round_history_file_path,
    save_json,
)

# Silence oneDNN & TensorFlow verbose info logs
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"

import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers


def create_keras_model(input_dim):
    """
    Creates a Multi-Layer Perceptron (MLP) for binary diabetes risk classification.
    Every simulated hospital node uses this identical architecture.
    """
    model = keras.Sequential([
        layers.Input(shape=(input_dim,)),
        layers.Dense(32, activation="relu", name="dense_1"),
        layers.Dropout(0.2, name="dropout_1"),
        layers.Dense(16, activation="relu", name="dense_2"),
        layers.Dropout(0.1, name="dropout_2"),
        layers.Dense(1, activation="sigmoid", name="output")
    ])
    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=0.005),
        loss=keras.losses.BinaryCrossentropy(),
        metrics=["accuracy", keras.metrics.AUC(name="auc")]
    )
    return model


def federated_averaging(hospital_weights, hospital_sample_counts):
    """
    Implements standard FedAvg (Federated Averaging):
    W_global = sum_{k=1}^K (n_k / N_total) * W_k
    where n_k is the number of local patient samples at Hospital k.
    """
    total_samples = sum(hospital_sample_counts)
    num_layers = len(hospital_weights[0])
    aggregated_weights = []

    for layer_idx in range(num_layers):
        layer_sum = np.zeros_like(hospital_weights[0][layer_idx])
        for k in range(len(hospital_weights)):
            weight_factor = hospital_sample_counts[k] / total_samples
            layer_sum += weight_factor * hospital_weights[k][layer_idx]
        aggregated_weights.append(layer_sum)

    return aggregated_weights


def determine_convergence(round_history):
    if len(round_history) < 2:
        return {
            "status": "insufficient_data",
            "message": "At least two rounds are required to evaluate convergence."
        }

    losses = [entry["training_loss"] for entry in round_history]
    accuracies = [entry["validation_accuracy"] for entry in round_history]

    loss_is_decreasing = losses[-1] < losses[0]
    acc_is_improving = accuracies[-1] >= accuracies[0]
    stable_accuracy = accuracies[-1] >= max(accuracies[:-1]) - 0.01

    if loss_is_decreasing and (acc_is_improving or stable_accuracy):
        status = "converging"
        message = "Loss is decreasing and accuracy is improving or stabilizing."
    else:
        status = "mixed"
        message = "Training completed, but the recorded metrics do not yet clearly demonstrate convergence."

    return {
        "status": status,
        "message": message,
        "loss_trend": "decreasing" if loss_is_decreasing else "not_decreasing",
        "accuracy_trend": "improving_or_stable" if (acc_is_improving or stable_accuracy) else "not_improving",
        "first_round_loss": losses[0],
        "latest_round_loss": losses[-1],
        "first_round_accuracy": accuracies[0],
        "latest_round_accuracy": accuracies[-1],
    }


def main():
    config = get_model_config("diabetes")
    print("====================================================================")
    print(" MEDISPHERE COGNITIVE TWIN - DIABETES FEDERATED LEARNING SIMULATION ")
    print("====================================================================\n")

    # ============================================================
    # 0. FEDERATED LEARNING ENVIRONMENT DIAGNOSTIC
    # ============================================================
    print("--- Environment Diagnostic ---")
    print(f"Python Version:     {sys.version.split()[0]}")
    print(f"TensorFlow Version: {tf.__version__}")
    print("Federated Learning Backend: Custom TensorFlow FedAvg")
    print("No TensorFlow Federated dependency is required for this workflow.")
    print("------------------------------\n")

    # ============================================================
    # 1. RESOLVE DATASET PATH
    # ============================================================
    data_path = "data/diabetes.csv"
    if not os.path.exists(data_path) and os.path.exists(os.path.join("ml-service", "data", "diabetes.csv")):
        data_path = os.path.join("ml-service", "data", "diabetes.csv")

    if not os.path.exists(data_path):
        print(f"Error: Dataset not found at {data_path}")
        sys.exit(1)

    print(f"Loading Diabetes dataset from: {data_path}...")
    df = pd.read_csv(data_path)
    print(f"Total dataset size: {df.shape[0]} patient records, {df.shape[1]} columns")

    # Separate Features and Target
    target_col = "Outcome"
    X = df.drop(columns=[target_col]).copy()
    y = df[target_col].values.astype(np.float32)
    feature_names = list(X.columns)

    # Handle physiologically invalid zero values without leakage
    invalid_zero_cols = ["Glucose", "BloodPressure", "SkinThickness", "Insulin", "BMI"]
    for col in invalid_zero_cols:
        X[col] = X[col].replace(0, np.nan)

    # ============================================================
    # 2. TRAIN / TEST SPLIT (HELD-OUT GLOBAL EVALUATION SET)
    # ============================================================
    X_train_full, X_test, y_train_full, y_test = train_test_split(
        X, y, test_size=0.20, random_state=42, stratify=y
    )

    # Impute missing values using training set medians
    train_medians = X_train_full.median()
    X_train_imputed = X_train_full.fillna(train_medians)
    X_test_imputed = X_test.fillna(train_medians)

    # Standardize numerical features for neural network convergence
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train_imputed).astype(np.float32)
    X_test_scaled = scaler.transform(X_test_imputed).astype(np.float32)

    print(f"Training pool: {X_train_scaled.shape[0]} patients")
    print(f"Global test evaluation set: {X_test_scaled.shape[0]} patients\n")

    # ============================================================
    # 3. PARTITION DATA INTO 3 SIMULATED HOSPITALS
    # ============================================================
    # Simulating 3 distinct healthcare centers:
    # Hospital 1: Central Endocrinology Institute (~40% of patients)
    # Hospital 2: Northside Health Network (~35% of patients)
    # Hospital 3: Sunrise Regional Hospital (~25% of patients)
    np.random.seed(42)
    total_train = len(X_train_scaled)
    indices = np.random.permutation(total_train)

    split_1 = int(0.40 * total_train)
    split_2 = int(0.75 * total_train)

    hospitals_data = {
        "Hospital 1 (Central Endocrinology)": {
            "X": X_train_scaled[indices[:split_1]],
            "y": y_train_full[indices[:split_1]]
        },
        "Hospital 2 (Northside Health Network)": {
            "X": X_train_scaled[indices[split_1:split_2]],
            "y": y_train_full[indices[split_1:split_2]]
        },
        "Hospital 3 (Sunrise Regional Hospital)": {
            "X": X_train_scaled[indices[split_2:]],
            "y": y_train_full[indices[split_2:]]
        }
    }

    print("====================================================================")
    print("       SIMULATED HOSPITAL DATA PARTITIONS (LOCAL DATA SILOS)        ")
    print("====================================================================")
    for name, data in hospitals_data.items():
        pos_rate = np.mean(data["y"]) * 100
        print(f" * {name}: {len(data['X'])} patients (Positive Diabetes rate: {pos_rate:.1f}%)")
    print("Patient records remain strictly confined inside each hospital silo.")
    print("Only model parameters (weights) are transmitted to the coordinator.\n")

    # ============================================================
    # 4. INITIALIZE GLOBAL MODEL & FEDERATED HYPERPARAMETERS
    # ============================================================
    input_dim = len(feature_names)
    global_model = create_keras_model(input_dim)
    global_weights = global_model.get_weights()

    num_federated_rounds = 5
    local_epochs = 3
    batch_size = 32
    round_history = []

    print("====================================================================")
    print(f" STARTING FEDERATED TRAINING: {num_federated_rounds} ROUNDS (FedAvg) ")
    print("====================================================================")

    # Initial baseline evaluation before training
    initial_preds = global_model.predict(X_test_scaled, verbose=0).flatten()
    initial_auc = roc_auc_score(y_test, initial_preds)
    initial_acc = accuracy_score(y_test, (initial_preds >= 0.5).astype(int))
    print(f"Round 0 (Initial Global Weights): Test Acc={initial_acc:.4f}, Test ROC-AUC={initial_auc:.4f}\n")

    # ============================================================
    # 5. FEDERATED TRAINING ROUNDS
    # ============================================================
    for round_num in range(1, num_federated_rounds + 1):
        print(f">>> FEDERATED ROUND {round_num}/{num_federated_rounds}")
        round_hospital_weights = []
        round_sample_counts = []
        round_local_losses = []
        round_local_accs = []

        # Each hospital trains locally on its private silo
        for hospital_name, data in hospitals_data.items():
            # 1. Instantiate local hospital model with current global weights
            local_model = create_keras_model(input_dim)
            local_model.set_weights(global_weights)

            # 2. Local training
            history = local_model.fit(
                data["X"], data["y"],
                epochs=local_epochs,
                batch_size=batch_size,
                verbose=0,
                shuffle=True
            )

            local_loss = history.history["loss"][-1]
            local_acc = history.history["accuracy"][-1]
            round_local_losses.append(float(local_loss))
            round_local_accs.append(float(local_acc))
            print(f"  - [{hospital_name}] Local training ({local_epochs} epochs): Loss={local_loss:.4f}, Acc={local_acc:.4f}")

            # 3. Collect updated model weights (NO raw patient data transferred)
            round_hospital_weights.append(local_model.get_weights())
            round_sample_counts.append(len(data["X"]))

        # 4. Central Coordinator performs Federated Averaging (FedAvg)
        global_weights = federated_averaging(round_hospital_weights, round_sample_counts)
        global_model.set_weights(global_weights)

        # 5. Evaluate updated global model on held-out test cohort
        y_prob = global_model.predict(X_test_scaled, verbose=0).flatten()
        y_pred = (y_prob >= 0.5).astype(int)
        round_acc = accuracy_score(y_test, y_pred)
        round_auc = roc_auc_score(y_test, y_prob)
        round_loss = float(np.mean(tf.keras.losses.binary_crossentropy(y_test, y_prob).numpy()))

        round_record = {
            "round": round_num,
            "training_loss": float(np.mean(round_local_losses)),
            "training_accuracy": float(np.mean(round_local_accs)),
            "validation_loss": round_loss,
            "validation_accuracy": float(round_acc),
            "validation_auc": float(round_auc),
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }
        round_history.append(round_record)

        print(f"  => Round {round_num} Global Evaluation: Test Acc={round_acc:.4f} ({round_acc*100:.2f}%), Test ROC-AUC={round_auc:.4f}\n")

    # ============================================================
    # 6. FINAL GLOBAL MODEL EVALUATION
    # ============================================================
    final_probs = global_model.predict(X_test_scaled, verbose=0).flatten()
    final_preds = (final_probs >= 0.5).astype(int)

    acc = accuracy_score(y_test, final_preds)
    prec = precision_score(y_test, final_preds, zero_division=0)
    rec = recall_score(y_test, final_preds, zero_division=0)
    f1 = f1_score(y_test, final_preds, zero_division=0)
    auc = roc_auc_score(y_test, final_probs)
    cm = confusion_matrix(y_test, final_preds)

    print("====================================================================")
    print("        FINAL GLOBAL FEDERATED DIABETES MODEL EVALUATION            ")
    print("====================================================================")
    print(f"Accuracy:         {acc:.4f} ({acc * 100:.2f}%)")
    print(f"Precision:        {prec:.4f}")
    print(f"Recall:           {rec:.4f}")
    print(f"F1-score:         {f1:.4f}")
    print(f"ROC-AUC:          {auc:.4f}")

    print("\nConfusion Matrix:")
    print(f"[[TN={cm[0,0]}  FP={cm[0,1]}]")
    print(f" [FN={cm[1,0]}  TP={cm[1,1]}]]")

    print("\nDetailed Classification Report:")
    print(classification_report(y_test, final_preds, digits=4, zero_division=0))

    # Save final global model
    models_dir = "models"
    if not os.path.exists(models_dir) and os.path.exists(os.path.join("ml-service", "models")):
        models_dir = os.path.join("ml-service", "models")
    os.makedirs(models_dir, exist_ok=True)
    save_path = str(model_file_path("diabetes"))
    global_model.save(save_path)
    print(f"Global Federated Diabetes model saved to: {save_path}")

    history_path = round_history_file_path("diabetes")
    save_json(history_path, round_history)
    print(f"Diabetes training history saved to: {history_path}")

    metadata = {
        "model_name": config["model_name"],
        "model_version": config["version"],
        "training_type": config["training_type"],
        "client_count": config["client_count"],
        "federated_rounds": num_federated_rounds,
        "final_accuracy": float(round_history[-1]["validation_accuracy"]),
        "final_loss": float(round_history[-1]["validation_loss"]),
        "final_auc": float(round_history[-1]["validation_auc"]),
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "convergence": determine_convergence(round_history),
    }
    metadata_path = metadata_file_path("diabetes")
    save_json(metadata_path, metadata)
    print(f"Diabetes model metadata saved to: {metadata_path}")

    print("\nNote: The 3 hospitals are an educational simulation for the MediSphere")
    print("internship project, using public research data (Pima Indians Diabetes Dataset).")
    print("This is not a clinically validated diagnostic system.")
    print("====================================================================\n")


if __name__ == "__main__":
    main()
