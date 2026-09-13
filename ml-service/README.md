# MediSphere ML Service

This Flask service loads the trained MediSphere federated Keras models and exposes prediction APIs for CVD and diabetes risk.

## Important preprocessing note

The training code uses `sklearn.preprocessing.StandardScaler` fitted only on the training data. The models were trained on scaled values, and the scaler was not saved with the final `.keras` model files.

Because of this, the ML service does not create a new scaler automatically. If a scaler is missing, the API returns a clear error message explaining that the scaler must be saved from the training code before deployment.

### Example code to save the scaler during training

```python
from sklearn.preprocessing import StandardScaler
import joblib

scaler = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train).astype(np.float32)
joblib.dump(scaler, "models/cvd_scaler.joblib")
```

Then in Flask, the service can load it with:

```python
from joblib import load
scaler = load("models/cvd_scaler.joblib")
scaled = scaler.transform(raw_features.reshape(1, -1))
```

## Files

- `app.py` — Flask app and prediction endpoints
- `utils/preprocessing.py` — helper code for feature metadata and scaler loading
- `models/` — trained Keras model files
- `requirements.txt` — project dependencies

## Run locally

```bash
cd ml-service
python app.py
```

Then visit:

- http://127.0.0.1:5000/health
- http://127.0.0.1:5000/predict/cvd
- http://127.0.0.1:5000/predict/diabetes

## Example test requests

### Health check

```bash
curl http://127.0.0.1:5000/health
```

### CVD prediction

```bash
curl -X POST http://127.0.0.1:5000/predict/cvd \
  -H "Content-Type: application/json" \
  -d '{"features": [55, 2, 1, 0, 5, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0]}'
```

### Diabetes prediction

```bash
curl -X POST http://127.0.0.1:5000/predict/diabetes \
  -H "Content-Type: application/json" \
  -d '{"features": [6, 148, 72, 35, 0, 33.6, 0.627, 50]}'
```

> The example values above are demo inputs only and are not real patient data.
