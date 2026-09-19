# MediSphere Cognitive Twin

MediSphere is a synthetic Digital Healthcare Twin prototype for demonstrating how healthcare data can be integrated, stored, monitored, and analyzed using FHIR, MongoDB, Kafka, machine learning, and real-time alerting.

The platform follows the flow:

**Collect Data → Create Digital Twin → Predict Risk → Monitor Patient → Alert Doctor → Create Care Plan**

The project uses synthetic/demo data and must not be treated as real clinical data.

---

## Project Milestones

### Milestone 1 — FHIR Integration & Digital Twin

The first milestone establishes the healthcare data foundation.

* FHIR R4 integration using HAPI FHIR
* Patient data import
* Patient Twin storage in MongoDB
* Patient 360 dashboard
* Kafka-based vital streaming
* Consent management
* Role-Based Access Control (RBAC)
* SMART on FHIR integration foundation

Supported roles:

* `ADMIN`
* `DOCTOR`
* `NURSE`

---

### Milestone 2 — Federated Risk Prediction & Explainability

The second milestone adds machine-learning-based risk prediction for cardiovascular and diabetes complications.

#### Risk Models

* Cardiovascular Disease (CVD) risk prediction
* Diabetes risk prediction
* Federated model artifacts
* Keras-based global models
* SHAP-based explainability
* Prediction metadata and model information

#### Current Validation Metrics

These are the current measured metrics from the available validation/test evaluation:

| Model    | Accuracy | ROC-AUC |
| -------- | -------: | ------: |
| CVD      |   84.37% |  0.6975 |
| Diabetes |   80.52% |  0.8683 |

The project reports the measured results honestly and does not artificially increase model accuracy.

#### ML Data Flow

```text
Patient Data
     ↓
Spring Boot Backend
     ↓
Flask ML Service
     ↓
Federated Keras Model
     ↓
Risk Prediction
     ↓
SHAP Explainability
     ↓
React Risk Prediction UI
```

---

### Milestone 3 — Continuous Monitoring & Alerts

The third milestone adds real-time monitoring of simulated wearable data and automated clinical alerting.

#### Wearable Integration

A Python wearable simulator generates synthetic:

* Heart Rate
* SpO2
* Blood Pressure
* Temperature

The generated data is published to Kafka topic:

```text
patient-vitals
```

Example:

```text
P001 | Heart Rate | 74 bpm
P001 | SpO2 | 97 %
P001 | Blood Pressure | 124/74 mmHg
P001 | Temperature | 36.7 °C
```

---

#### Kafka Streaming

Kafka receives the simulated vital events and Spring Boot consumes them in real time.

```text
Wearable Simulator
       ↓
Apache Kafka
       ↓
patient-vitals
       ↓
Spring Boot Kafka Consumer
       ↓
MongoDB vital_records
```

---

#### Anomaly Detection

The current monitoring service checks vital values against configured clinical thresholds.

| Vital        | Anomaly Condition     |
| ------------ | --------------------- |
| Heart Rate   | `< 50` or `> 120 BPM` |
| SpO2         | `< 92%`               |
| Systolic BP  | `> 140 mmHg`          |
| Diastolic BP | `> 90 mmHg`           |
| Temperature  | `< 35°C` or `> 38°C`  |

The system distinguishes between normal and abnormal vital events before creating alerts.

---

#### Clinical Rule Engine

The Clinical Rule Engine determines:

* Alert severity
* Clinical rule triggered
* Recommended action
* Alert routing

Examples:

```text
Heart Rate > 120
→ HIGH
→ Review patient vitals
→ Cardiologist
```

```text
SpO2 < 90
→ CRITICAL
→ Immediate clinical review recommended
→ Cardiologist
```

```text
Temperature > 38
→ HIGH
→ Review patient vitals
→ General Physician
```

---

#### Real-Time Alert Engine

When an anomaly is detected:

```text
Vital Event
     ↓
Anomaly Detection
     ↓
Clinical Rule Engine
     ↓
Alert Service
     ↓
Duplicate Alert Prevention
     ↓
WebSocket
     ↓
Doctor Dashboard
```

The dashboard displays information such as:

```text
HIGH ALERT

Patient: Sarah M. (P001)
Vital: Heart Rate
Value: 145 BPM
Assigned To: Cardiologist
Notification: NOTIFIED
Recommended Action: Review patient vitals
Status: NEW
```

Alerts can be acknowledged from the dashboard.

```text
NEW → ACKNOWLEDGED
```

`NOTIFIED` represents notification to the MediSphere doctor dashboard through the real-time WebSocket system. The current prototype does not claim SMS, email, or mobile push notification delivery.

---

## Milestone 3 Validation

A labeled validation dataset containing 120 synthetic samples was created to validate the existing rule-based anomaly detection service.

### Dataset

| Vital          | Samples |
| -------------- | ------: |
| Heart Rate     |      30 |
| SpO2           |      30 |
| Blood Pressure |      30 |
| Temperature    |      30 |
| **Total**      | **120** |

Each vital contains normal and abnormal boundary/threshold cases.

### Results

| Metric           |   Result |
| ---------------- | -------: |
| True Positives   |       60 |
| False Positives  |        0 |
| True Negatives   |       60 |
| False Negatives  |        0 |
| Precision        | **100%** |
| False Alert Rate |   **0%** |
| Accuracy         | **100%** |

The validation results above apply specifically to the **120-sample labeled anomaly-detection dataset and the configured rule-based detection logic**. They should not be interpreted as machine-learning model accuracy.

For this project, false alert rate is calculated as:

```text
False Alert Rate = FP / (TP + FP) × 100
```

The Milestone 3 specification includes a 3.2-minute response-time target. This prototype has not established a measured 3.2-minute average response time, so that value is treated as a project target rather than a measured result.

---

## Stack

* Java 25
* Spring Boot 4.1.1
* Maven
* MongoDB
* Apache Kafka 4.2.1
* HAPI FHIR R4
* React
* Vite
* JavaScript
* Python
* Flask
* TensorFlow / Keras
* SHAP
* WebSocket / STOMP

---

## Configuration

Set these environment variables for a local run:

```text
MONGODB_URI=mongodb://localhost:27017/medisphere

MONGODB_DATABASE=medisphere

KAFKA_BOOTSTRAP_SERVERS=localhost:9092

FHIR_BASE_URL=https://r4.quality.hl7.org/fhir
```

Do not commit credentials.

The local configuration uses safe localhost defaults. Use environment variables for MongoDB Atlas or other hosted services.

---

## Kafka Configuration

Kafka runs locally on:

```text
localhost:9092
```

Main topics:

```text
patient-vitals
consent-events
```

Consumer groups include:

```text
medisphere-vitals
medisphere-consent
```

The wearable simulator publishes synthetic vital events to `patient-vitals`.

---

## Running the Project

### 1. Start MongoDB

Make sure MongoDB is running locally:

```text
mongodb://localhost:27017
```

---

### 2. Start Kafka

If using the project's Docker configuration:

```powershell
docker compose up -d
```

Alternatively, start the local Kafka installation according to the project environment.

Verify that port `9092` is reachable:

```powershell
Test-NetConnection -ComputerName localhost -Port 9092
```

---

### 3. Start the Spring Boot Backend

Set the required environment variables:

```powershell
$env:MONGODB_URI = "mongodb://localhost:27017/medisphere"

$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"

$env:SERVER_PORT = "8081"
```

Start the backend:

```powershell
./mvnw.cmd spring-boot:run
```

Backend:

```text
http://localhost:8081
```

---

### 4. Start the ML Service

The Flask ML service runs on:

```text
http://127.0.0.1:5000
```

Available endpoints include:

```text
GET  /health
POST /predict/cvd
POST /predict/diabetes
```

---

### 5. Start the Wearable Simulator

The simulator publishes synthetic vital events to:

```text
patient-vitals
```

Example command:

```powershell
python wearable_simulator.py
```

The simulator periodically generates:

* Heart Rate
* SpO2
* Blood Pressure
* Temperature

---

### 6. Start the React Frontend

```powershell
cd frontend
npm install
npm run dev
```

---

## Authentication

The API uses HTTP Basic authentication with BCrypt passwords and roles:

```text
ADMIN
DOCTOR
NURSE
```

Development users are initialized by `DataInitializer`.

Change or remove development credentials before using the application outside the demo environment.

---

## Main APIs

### Authentication

```text
POST /api/auth/login
POST /api/auth/signup
```

### FHIR

```text
POST /api/fhir/import
GET  /api/fhir/patients
GET  /api/fhir/patients/{id}/save-twin
```

### Patient Twins

```text
GET|POST|PUT|DELETE /api/patient-twins
```

### Vitals

```text
POST /api/vitals
GET  /api/vital-records/{patientId}/latest
```

### Consent

```text
POST /api/consent
PUT  /api/consent/revoke/{patientId}
GET  /api/consent/{patientId}
```

### Alerts

```text
GET  /api/alerts/active
GET  /api/alerts
GET  /api/alerts/patient/{patientId}
PUT  /api/alerts/{id}/acknowledge
```

### WebSocket

```text
ws://localhost:8081/ws
```

Alert topic:

```text
/topic/alerts
```

---

## Data Flow

### Patient Data

```text
patient-data.json
        ↓
Spring Boot
        ↓
FHIR Patient / Condition / Observation
        ↓
MongoDB PatientTwin
        ↓
React Patient 360 Dashboard
```

### Real-Time Monitoring

```text
Wearable Simulator
        ↓
Kafka patient-vitals
        ↓
Spring Boot Consumer
        ↓
MongoDB vital_records
        ↓
Anomaly Detection
        ↓
Clinical Rule Engine
        ↓
Alert Service
        ↓
WebSocket
        ↓
React Monitoring Dashboard
```

### Risk Prediction

```text
Patient Data
        ↓
Spring Boot
        ↓
Flask ML Service
        ↓
Federated Keras Model
        ↓
CVD / Diabetes Risk Prediction
        ↓
SHAP Explanation
        ↓
React Risk Prediction Dashboard
```

---

## Project Data and Privacy

All patient information included in this repository is synthetic demonstration data.

The project is intended as a prototype for demonstrating healthcare interoperability, digital-twin concepts, machine learning, real-time monitoring, and alerting.

It is not a clinical decision-support system and should not be used to make real patient-care decisions.
