import os
from pathlib import Path

import numpy as np
from flask import Flask, jsonify, request
from flask_cors import CORS
from tensorflow import keras

from utils.model_registry import get_model_config, load_json, metadata_file_path
from utils.preprocessing import prepare_model_input


# ============================================================
# CONFIGURATION
# ============================================================

BASE_DIR = Path(__file__).resolve().parent
MODEL_DIR = BASE_DIR / "models"

app = Flask(__name__)
CORS(app)


# ============================================================
# MODEL VARIABLES
# ============================================================

cvd_model = None
diabetes_model = None


# ============================================================
# MODEL LOADING
# ============================================================

def load_model_with_clear_error(model_name, model_path):
    """
    Load a Keras model and display a clear error if
    the model file is missing or cannot be loaded.
    """

    if not os.path.exists(model_path):
        print()
        print(f"ERROR: {model_name} model file not found:")
        print(model_path)
        return None

    try:
        model = keras.models.load_model(model_path)

        print()
        print(f"SUCCESS: {model_name} model loaded")
        print(f"Path: {model_path}")

        return model

    except Exception as exc:
        print()
        print(f"ERROR: Failed to load {model_name} model")
        print(f"Reason: {exc}")

        return None


# ============================================================
# LOAD CVD MODEL
# ============================================================

cvd_model_path = MODEL_DIR / "cvd_federated_global.keras"

cvd_model = load_model_with_clear_error(
    "CVD",
    cvd_model_path
)


# ============================================================
# LOAD DIABETES MODEL
# ============================================================

diabetes_model_path = MODEL_DIR / "diabetes_federated_global.keras"

diabetes_model = load_model_with_clear_error(
    "Diabetes",
    diabetes_model_path
)


# ============================================================
# HEALTH CHECK
# ============================================================


def load_model_metadata(model_name):
    config = get_model_config(model_name)
    metadata = load_json(metadata_file_path(model_name))
    if metadata is None:
        return {
            "status": "missing",
            "version": config["version"],
            "model_name": config["model_name"],
            "federated_round": None,
            "accuracy": None,
        }

    return {
        "status": "loaded" if globals()[f"{model_name}_model"] is not None else "missing",
        "version": metadata.get("model_version", config["version"]),
        "model_name": metadata.get("model_name", config["model_name"]),
        "federated_round": metadata.get("federated_rounds"),
        "accuracy": metadata.get("final_accuracy"),
        "loss": metadata.get("final_loss"),
        "trained_at": metadata.get("trained_at"),
        "convergence": metadata.get("convergence", {}).get("status"),
    }


@app.get("/health")
def health():

    cvd_status = (
        "loaded"
        if cvd_model is not None
        else "missing"
    )

    diabetes_status = (
        "loaded"
        if diabetes_model is not None
        else "missing"
    )

    cvd_metadata = load_model_metadata("cvd")
    diabetes_metadata = load_model_metadata("diabetes")

    return jsonify({
        "status": "healthy",
        "service": "MediSphere ML Service",
        "models": {
            "cvd": cvd_metadata,
            "diabetes": diabetes_metadata,
        },
        "cvd_model_status": cvd_status,
        "diabetes_model_status": diabetes_status,
    }), 200


# ============================================================
# HOME PAGE
# ============================================================

@app.get("/")
def home():

    return jsonify({
        "message": "MediSphere ML Service is running",
        "status": "online",
        "endpoints": {
            "health": "GET /health",
            "cvd_prediction": "POST /predict/cvd",
            "diabetes_prediction": "POST /predict/diabetes",
            "model_metadata": "GET /models/<cvd|diabetes>"
        }
    }), 200


@app.get("/models/<model_name>")
def model_metadata(model_name):
    allowed = {"cvd", "diabetes"}
    if model_name not in allowed:
        return jsonify({
            "error": "Unsupported model",
            "available_models": ["cvd", "diabetes"]
        }), 404

    metadata = load_model_metadata(model_name)
    return jsonify({
        "model": metadata.get("model_name"),
        "version": metadata.get("version"),
        "federated_round": metadata.get("federated_round"),
        "accuracy": metadata.get("accuracy"),
        "loss": metadata.get("loss"),
        "status": metadata.get("status"),
        "convergence": metadata.get("convergence"),
        "trained_at": metadata.get("trained_at"),
    }), 200


# ============================================================
# FEATURE VALIDATION
# ============================================================

def validate_features(payload, expected_count, model_name):

    # --------------------------------------------------------
    # Check request body
    # --------------------------------------------------------

    if payload is None:

        return None, {
            "error": f"{model_name} request body is required"
        }, 400


    # --------------------------------------------------------
    # Check JSON object
    # --------------------------------------------------------

    if not isinstance(payload, dict):

        return None, {
            "error": f"{model_name} request must be a JSON object"
        }, 400


    # --------------------------------------------------------
    # Check features field
    # --------------------------------------------------------

    if "features" not in payload:

        return None, {
            "error": f"{model_name} requires a 'features' field"
        }, 400


    features = payload["features"]


    # --------------------------------------------------------
    # Check features list
    # --------------------------------------------------------

    if not isinstance(features, list):

        return None, {
            "error": f"{model_name} features must be a list"
        }, 400


    # --------------------------------------------------------
    # Check number of features
    # --------------------------------------------------------

    if len(features) != expected_count:

        return None, {
            "error": (
                f"{model_name} requires exactly "
                f"{expected_count} features"
            ),
            "received": len(features),
            "expected": expected_count
        }, 400


    # --------------------------------------------------------
    # Validate every feature
    # --------------------------------------------------------

    for index, value in enumerate(features):

        # Null check
        if value is None:

            return None, {
                "error": (
                    f"Feature at index {index} "
                    f"cannot be null"
                )
            }, 400


        # Boolean check
        if isinstance(value, bool):

            return None, {
                "error": (
                    f"Feature at index {index} "
                    f"must be numeric"
                )
            }, 400


        # Numeric check
        if not isinstance(value, (int, float)):

            return None, {
                "error": (
                    f"Feature at index {index} "
                    f"must be numeric"
                )
            }, 400


        # NaN / Infinity check
        if not np.isfinite(value):

            return None, {
                "error": (
                    f"Feature at index {index} "
                    f"must be a finite number"
                )
            }, 400


    return features, None, None


# ============================================================
# MODEL PREDICTION
# ============================================================

def predict_with_probability(
    model,
    model_name,
    prepared_features
):

    try:

        prediction = model.predict(
            prepared_features,
            verbose=0
        )


        # Get probability
        probability = float(
            prediction[0][0]
        )


        # Make binary prediction
        predicted_class = int(
            probability >= 0.5
        )


        result = {

            "model": model_name,

            "prediction": predicted_class,

            "risk_probability": round(
                probability,
                6
            ),

            "risk_percentage": round(
                probability * 100,
                2
            )

        }


        return result, 200


    except Exception as exc:

        return {

            "error": (
                f"{model_name} prediction failed"
            ),

            "details": str(exc)

        }, 500


# ============================================================
# CVD PREDICTION
# ============================================================

@app.post("/predict/cvd")
def predict_cvd():

    # --------------------------------------------------------
    # Check CVD model
    # --------------------------------------------------------

    if cvd_model is None:

        return jsonify({

            "error": "CVD model is not loaded",

            "model_path": str(
                cvd_model_path
            )

        }), 500


    # --------------------------------------------------------
    # Read JSON request
    # --------------------------------------------------------

    payload = request.get_json(
        silent=True
    )


    # --------------------------------------------------------
    # Validate features
    # CVD model requires 15 features
    # --------------------------------------------------------

    features, error, status_code = validate_features(

        payload,

        15,

        "CVD model"

    )


    if error is not None:

        return jsonify(error), status_code


    # --------------------------------------------------------
    # Preprocessing
    # --------------------------------------------------------

    try:

        prepared_features = prepare_model_input(

            "cvd",

            features

        )


    except FileNotFoundError as exc:

        return jsonify({

            "error": "CVD preprocessing file not found",

            "details": str(exc)

        }), 500


    except Exception as exc:

        return jsonify({

            "error": "CVD preprocessing failed",

            "details": str(exc)

        }), 500


    # --------------------------------------------------------
    # Prediction
    # --------------------------------------------------------

    result, status_code = predict_with_probability(

        cvd_model,

        "CVD Federated Global Model",

        prepared_features

    )


    return jsonify(result), status_code


# ============================================================
# DIABETES PREDICTION
# ============================================================

@app.post("/predict/diabetes")
def predict_diabetes():

    # --------------------------------------------------------
    # Check Diabetes model
    # --------------------------------------------------------

    if diabetes_model is None:

        return jsonify({

            "error": "Diabetes model is not loaded",

            "model_path": str(
                diabetes_model_path
            )

        }), 500


    # --------------------------------------------------------
    # Read JSON request
    # --------------------------------------------------------

    payload = request.get_json(
        silent=True
    )


    # --------------------------------------------------------
    # Validate features
    # Diabetes model requires 8 features
    # --------------------------------------------------------

    features, error, status_code = validate_features(

        payload,

        8,

        "Diabetes model"

    )


    if error is not None:

        return jsonify(error), status_code


    # --------------------------------------------------------
    # Preprocessing
    # --------------------------------------------------------

    try:

        prepared_features = prepare_model_input(

            "diabetes",

            features

        )


    except FileNotFoundError as exc:

        return jsonify({

            "error": "Diabetes preprocessing file not found",

            "details": str(exc)

        }), 500


    except Exception as exc:

        return jsonify({

            "error": "Diabetes preprocessing failed",

            "details": str(exc)

        }), 500


    # --------------------------------------------------------
    # Prediction
    # --------------------------------------------------------

    result, status_code = predict_with_probability(

        diabetes_model,

        "Diabetes Federated Global Model",

        prepared_features

    )


    return jsonify(result), status_code


# ============================================================
# ERROR HANDLERS
# ============================================================

@app.errorhandler(404)
def not_found(error):

    return jsonify({

        "error": "Endpoint not found",

        "available_endpoints": [

            "GET /",

            "GET /health",

            "POST /predict/cvd",

            "POST /predict/diabetes"

        ]

    }), 404


@app.errorhandler(405)
def method_not_allowed(error):

    return jsonify({

        "error": "Method not allowed",

        "message": (
            "Use GET for /health and POST "
            "for prediction endpoints."
        ),

        "available_endpoints": {

            "health": "GET /health",

            "cvd": "POST /predict/cvd",

            "diabetes": "POST /predict/diabetes"

        }

    }), 405


@app.errorhandler(500)
def internal_server_error(error):

    return jsonify({

        "error": "Internal server error"

    }), 500


# ============================================================
# START FLASK SERVER
# ============================================================

if __name__ == "__main__":

    print()
    print("================================================")
    print("           MediSphere ML Service")
    print("================================================")
    print()
    print("Home:")
    print("http://127.0.0.1:5000/")
    print()
    print("Health:")
    print("http://127.0.0.1:5000/health")
    print()
    print("CVD Prediction:")
    print("POST http://127.0.0.1:5000/predict/cvd")
    print()
    print("Diabetes Prediction:")
    print("POST http://127.0.0.1:5000/predict/diabetes")
    print()
    print("================================================")
    print()

    app.run(
        host="127.0.0.1",
        port=5000,
        debug=True
    )