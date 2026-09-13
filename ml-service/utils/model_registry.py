from __future__ import annotations

import json
from pathlib import Path
from typing import Any

BASE_DIR = Path(__file__).resolve().parents[1]
MODEL_DIR = BASE_DIR / "models"

MODEL_REGISTRY = {
    "cvd": {
        "model_name": "CVD Federated Global Model",
        "display_name": "CVD",
        "version": "CVD-v1.0",
        "model_filename": "cvd_federated_global.keras",
        "metadata_filename": "cvd_model_metadata.json",
        "round_history_filename": "cvd_training_history.json",
        "training_type": "Federated Learning",
        "client_count": 3,
        "target_column": "TenYearCHD",
    },
    "diabetes": {
        "model_name": "Diabetes Federated Global Model",
        "display_name": "Diabetes",
        "version": "DIABETES-v1.0",
        "model_filename": "diabetes_federated_global.keras",
        "metadata_filename": "diabetes_model_metadata.json",
        "round_history_filename": "diabetes_training_history.json",
        "training_type": "Federated Learning",
        "client_count": 3,
        "target_column": "Outcome",
    },
}


def get_model_config(model_name: str) -> dict[str, Any]:
    key = model_name.lower().strip()
    if key not in MODEL_REGISTRY:
        valid = ", ".join(MODEL_REGISTRY.keys())
        raise ValueError(f"Unknown model name '{model_name}'. Expected one of: {valid}")
    return MODEL_REGISTRY[key]


def model_file_path(model_name: str) -> Path:
    config = get_model_config(model_name)
    return MODEL_DIR / config["model_filename"]


def metadata_file_path(model_name: str) -> Path:
    config = get_model_config(model_name)
    return MODEL_DIR / config["metadata_filename"]


def round_history_file_path(model_name: str) -> Path:
    config = get_model_config(model_name)
    return MODEL_DIR / config["round_history_filename"]


def ensure_model_dir() -> Path:
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    return MODEL_DIR


def save_json(path: Path, payload: dict | list) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as file:
        json.dump(payload, file, indent=2)
        file.write("\n")


def load_json(path: Path):
    if not path.exists():
        return None
    with path.open("r", encoding="utf-8") as file:
        return json.load(file)
