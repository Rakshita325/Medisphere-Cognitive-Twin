# MediSphere Cognitive Twin

MediSphere is a synthetic Digital Healthcare Twin prototype. It imports patient data into FHIR R4, stores application-level twins in MongoDB, accepts vital events through Kafka, and provides a React Patient 360 dashboard.

## Stack

- Java 25 and Spring Boot 4.1.1
- Maven
- MongoDB
- Apache Kafka
- HAPI FHIR R4 client
- React and Vite

The bundled `src/main/resources/patient-data.json` is synthetic demo data and must not be treated as real clinical data.

## Configuration

Set these environment variables for a local run:

```text
MONGODB_URI=mongodb://localhost:27017/medisphere
MONGODB_DATABASE=medisphere
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
FHIR_BASE_URL=https://r4.quality.hl7.org/fhir
```

Do not commit credentials. The local properties file uses safe localhost defaults; use environment variables for MongoDB Atlas or other hosted services.

## Run

Start the local Kafka broker if it is not already running:

```powershell
docker compose up -d zookeeper kafka
```

Then start the backend:

```powershell
$env:MONGODB_URI = "mongodb://localhost:27017/medisphere"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:SERVER_PORT = "8081"
./mvnw.cmd spring-boot:run
```

You can confirm Kafka is reachable with:

```powershell
Test-NetConnection -ComputerName localhost -Port 9092
```

Run the frontend separately:

```powershell
cd frontend
npm install
npm run dev
```

## Authentication

The API uses HTTP Basic authentication with BCrypt passwords and roles `ADMIN`, `DOCTOR`, and `NURSE`. Development users are initialized by `DataInitializer`; change or remove those credentials for any non-demo deployment.

## Main APIs

- `POST /api/auth/login`, `POST /api/auth/signup`
- `POST /api/fhir/import`
- `GET /api/fhir/patients`
- `GET /api/fhir/patients/{id}/save-twin`
- `GET|POST|PUT|DELETE /api/patient-twins`
- `POST /api/vitals`
- `POST /api/consent`, `PUT /api/consent/revoke/{patientId}`, `GET /api/consent/{patientId}`

## Data flow

`patient-data.json -> Spring Boot -> FHIR Patient/Condition/Observation -> MongoDB PatientTwin -> React Patient 360`

Kafka topic `patient-vitals` carries simulated vital events. Consent events use `consent-events`.
