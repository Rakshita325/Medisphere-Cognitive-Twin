# MediSphere Cognitive Twin

MediSphere is a synthetic Digital Healthcare Twin prototype for demonstrating how healthcare data can be integrated, stored, monitored, and analyzed using FHIR, MongoDB, Kafka, machine learning, real-time alerting, and personalized care management.

The platform follows the flow:

**Collect Data → Create Digital Twin → Predict Risk → Monitor Patient → Alert Doctor → Create Care Plan → Track Treatment → Check Improvement**

The project uses synthetic/demo data and must not be treated as real clinical data.


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

| Model    | Accuracy | ROC-AUC |
| -------- | -------: | ------: |
| CVD      |   84.37% |  0.6975 |
| Diabetes |   80.52% |  0.8683 |

The project reports the measured results honestly and does not artificially increase model accuracy.

#### ML Data Flow

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

### Milestone 3 — Continuous Monitoring & Alerts

The third milestone adds real-time monitoring of simulated wearable data and automated clinical alerting.

#### Wearable Integration

A Python wearable simulator generates synthetic:

* Heart Rate
* SpO2
* Blood Pressure
* Temperature

The generated data is published to Kafka topic:

patient-vitals

#### Kafka Streaming


Wearable Simulator
       ↓
Apache Kafka
       ↓
patient-vitals
       ↓
Spring Boot Kafka Consumer
       ↓
MongoDB vital_records


#### Anomaly Detection

The monitoring service checks vital values against configured thresholds.

| Vital        | Anomaly Condition     |
| ------------ | --------------------- |
| Heart Rate   | `< 50` or `> 120 BPM` |
| SpO2         | `< 92%`               |
| Systolic BP  | `> 140 mmHg`          |
| Diastolic BP | `> 90 mmHg`           |
| Temperature  | `< 35°C` or `> 38°C`  |

#### Clinical Rule Engine

The Clinical Rule Engine determines:

* Alert severity
* Clinical rule triggered
* Recommended action
* Alert routing

#### Real-Time Alert Engine


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


Alerts can be acknowledged from the dashboard.

NEW → ACKNOWLEDGED

The current prototype does not claim SMS, email, or mobile push notification delivery.


## Milestone 4 — Personalized Care Plan & Treatment Management

The fourth milestone focuses on using patient clinical information and risk predictions to create personalized care plans, track treatment, monitor health measurements, and evaluate patient progress.

### Care Plan Management

The system generates patient-specific care plans using:

* Patient clinical information
* Existing medical conditions
* CVD risk prediction
* Diabetes risk prediction
* Clinical goals
* Treatment interventions

The generated care plans are persisted in MongoDB so that they can be retrieved later.

### Risk-Based Care Planning

Milestone 4 integrates the risk predictions from Milestone 2.

Patient Clinical Data
        ↓
M2 Feature Extraction
        ↓
CVD / Diabetes Risk Prediction
        ↓
Patient-Specific Risk
        ↓
M4 Care Plan

Patients without the required M2 features are handled using an `INSUFFICIENT_DATA` state rather than generating an unsupported prediction.

### Treatment Tracking

The system allows doctors to:

* Add treatment interventions
* Track treatment status
* Monitor adherence
* Review treatment progress

Example:


Care Plan
    ↓
Intervention
    ↓
Treatment Tracking
    ↓
Adherence / Status

### Health Monitoring

Doctors can record new health measurements such as:

* Blood pressure
* Glucose
* Heart rate
* SpO2
* Cholesterol
* BMI

Measurements are stored as longitudinal records so that the patient's health history can be reviewed over time.

### Progress Comparison

The system compares baseline and latest health measurements.

Baseline Measurement
        ↓
New Measurement
        ↓
Comparison
        ↓
Progress / Trend

This provides a longitudinal view of changes in the patient's health measurements.

### Care Plan Validation

A rule-based validation layer checks generated care plans for configured:

* Clinical guideline conditions
* Patient safety boundaries
* Contraindications
* Drug interactions
* Care-plan completeness

### Milestone 4 Data Flow

Patient Data
     ↓
M2 Risk Prediction
     ↓
Personalized Care Plan
     ↓
Add Intervention
     ↓
Treatment Tracking
     ↓
Add New Measurement
     ↓
Health Monitoring
     ↓
Progress Comparison
     ↓
Doctor Dashboard

### Milestone 4 Persistence


Care Plan / Treatment / Measurement
              ↓
           MongoDB
              ↓
        Retrieve Later
              ↓
        React Dashboard


## Milestone 4 Main APIs

### Patient Data


GET  /api/patient-data
GET  /api/patient-data/{patientId}


### Risk Prediction

POST /api/ml/predict/cvd
POST /api/ml/predict/diabetes


### Care Plans

POST /api/careplans/generate
GET  /api/careplans/patient/{patientId}


### Treatment Tracking

Treatment tracking APIs support creating and retrieving treatment records, recording adherence, and calculating treatment compliance.

### Health Monitoring

Health monitoring APIs support adding and retrieving longitudinal health measurements.

### Patient Progress

Patient progress APIs provide baseline-versus-latest health comparisons and progress information.


## Milestone 4 Frontend Modules

The React frontend provides separate interfaces for:

* Care Plans
* Treatment Tracking
* Health Monitoring
* Patient Progress
* Risk Predictions

The doctor can navigate between these modules from the application sidebar.


## Complete System Data Flow

### Patient Data

patient-data.json
        ↓
Spring Boot
        ↓
FHIR / Clinical Patient Data
        ↓
MongoDB
        ↓
React Patient Dashboard


### Risk Prediction

Patient Data
     ↓
Spring Boot
     ↓
Flask ML Service
     ↓
Federated Keras Model
     ↓
CVD / Diabetes Risk
     ↓
React Risk Prediction Dashboard
     ↓
M4 Care Plan

### Real-Time Monitoring

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

### Care Management

Patient Data + Risk
        ↓
Care Plan
        ↓
Intervention
        ↓
Treatment Tracking
        ↓
New Health Measurement
        ↓
Progress Comparison
        ↓
Doctor Dashboard


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


## Configuration

Set these environment variables for a local run:

MONGODB_URI=mongodb://localhost:27017/medisphere

MONGODB_DATABASE=medisphere

KAFKA_BOOTSTRAP_SERVERS=localhost:9092

FHIR_BASE_URL=https://r4.quality.hl7.org/fhir

Do not commit credentials.

The local configuration uses safe localhost defaults. Use environment variables for MongoDB Atlas or other hosted services.

## Kafka Configuration

Kafka runs locally on:

localhost:9092


Main topics:

patient-vitals
consent-events


The wearable simulator publishes synthetic vital events to `patient-vitals`.


## Running the Project

### 1. Start MongoDB

Make sure MongoDB is running locally:

mongodb://localhost:27017


### 2. Start Kafka

If using the project's Docker configuration:

powershell
docker compose up -d


Alternatively, start the local Kafka installation according to the project environment.

### 3. Start the Spring Boot Backend
powershell
$env:MONGODB_URI = "mongodb://localhost:27017/medisphere"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:SERVER_PORT = "8081"


Start the backend:

powershell
./mvnw.cmd spring-boot:run

Backend:

http://localhost:8081


### 4. Start the ML Service

The Flask ML service runs on:

http://127.0.0.1:5000

Available endpoints:

GET  /health
POST /predict/cvd
POST /predict/diabetes

### 5. Start the Wearable Simulator

powershell
python wearable_simulator.py


### 6. Start the React Frontend

powershell
cd frontend
npm install
npm run dev

## Authentication

The API uses HTTP Basic authentication with BCrypt passwords and roles:

ADMIN
DOCTOR
NURSE

Development users are initialized by `DataInitializer`.

Change or remove development credentials before using the application outside the demo environment.


## Project Data and Privacy

All patient information included in this repository is synthetic demonstration data.

The project is intended as a prototype for demonstrating:

* Healthcare interoperability
* Digital-twin concepts
* Machine learning
* Federated risk prediction
* Real-time monitoring
* Clinical alerting
* Personalized care planning
* Treatment tracking
* Longitudinal health monitoring

It is not a clinical decision-support system and should not be used to make real patient-care decisions.
