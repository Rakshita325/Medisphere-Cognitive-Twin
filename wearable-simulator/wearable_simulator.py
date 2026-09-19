import json
import random
import time
from datetime import datetime, timezone

from kafka import KafkaProducer


# Kafka configuration
KAFKA_SERVER = "localhost:9092"
KAFKA_TOPIC = "patient-vitals"


# Simulated patients
PATIENTS = [
    {
        "patientId": "P001",
        "patientName": "Sarah M."
    },
    {
        "patientId": "P002",
        "patientName": "John D."
    }
]


def create_producer():
    return KafkaProducer(
        bootstrap_servers=KAFKA_SERVER,
        value_serializer=lambda value: json.dumps(value).encode("utf-8")
    )


def generate_heart_rate():
    return random.randint(68, 85)


def generate_spo2():
    return random.randint(97, 100)


def generate_blood_pressure():
    systolic = random.randint(115, 130)
    diastolic = random.randint(70, 85)

    return systolic, diastolic


def generate_temperature():
    return round(random.uniform(36.5, 37.1), 1)


def create_vital(patient, vital_type):

    timestamp = datetime.now(timezone.utc).isoformat()

    if vital_type == "Heart Rate":

        return {
            "patientId": patient["patientId"],
            "type": "Heart Rate",
            "value": str(generate_heart_rate()),
            "unit": "bpm",
            "recordedAt": timestamp,
            "systolic": None,
            "diastolic": None
        }

    elif vital_type == "SpO2":

        return {
            "patientId": patient["patientId"],
            "type": "SpO2",
            "value": str(generate_spo2()),
            "unit": "%",
            "recordedAt": timestamp,
            "systolic": None,
            "diastolic": None
        }

    elif vital_type == "Blood Pressure":

        systolic, diastolic = generate_blood_pressure()

        return {
            "patientId": patient["patientId"],
            "type": "Blood Pressure",
            "value": None,
            "unit": "mmHg",
            "recordedAt": timestamp,
            "systolic": systolic,
            "diastolic": diastolic
        }

    elif vital_type == "Temperature":

        return {
            "patientId": patient["patientId"],
            "type": "Temperature",
            "value": str(generate_temperature()),
            "unit": "°C",
            "recordedAt": timestamp,
            "systolic": None,
            "diastolic": None
        }


def main():

    print("=" * 60)
    print("MediSphere Wearable Simulator")
    print("=" * 60)

    print(f"Kafka Server : {KAFKA_SERVER}")
    print(f"Kafka Topic  : {KAFKA_TOPIC}")
    print()

    print("Connecting to Kafka...")

    try:
        producer = create_producer()
        producer.bootstrap_connected()

        print("Kafka connection successful!")
        print()
        print("Starting wearable simulation...")
        print("Press Ctrl+C to stop.")
        print()

    except Exception as e:
        print("Could not connect to Kafka.")
        print(f"Error: {e}")
        return

    vital_types = [
        "Heart Rate",
        "SpO2",
        "Blood Pressure",
        "Temperature"
    ]

    try:

        while True:

            for patient in PATIENTS:

                for vital_type in vital_types:

                    vital = create_vital(patient, vital_type)

                    producer.send(
                        KAFKA_TOPIC,
                        key=patient["patientId"].encode("utf-8"),
                        value=vital
                    )

                    producer.flush()

                    print(
                        f"[{vital['recordedAt']}] "
                        f"{patient['patientId']} | "
                        f"{vital['type']} | "
                        f"{vital['value'] if vital['value'] else str(vital['systolic']) + '/' + str(vital['diastolic'])} "
                        f"{vital['unit']}"
                    )

            print("-" * 60)

            time.sleep(5)

    except KeyboardInterrupt:

        print()
        print("Wearable simulator stopped.")

    finally:
        producer.close()


if __name__ == "__main__":
    main()