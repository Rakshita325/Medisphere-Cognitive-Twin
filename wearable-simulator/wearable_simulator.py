import json
import random
import time
from datetime import datetime, timezone

from kafka import KafkaProducer


# ============================================================
# Kafka Configuration
# ============================================================

KAFKA_SERVER = "localhost:9092"
KAFKA_TOPIC = "patient-vitals"


# ============================================================
# Simulated Patients
# ============================================================

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


# ============================================================
# Abnormal Episode Configuration
# ============================================================

# How long one abnormal episode lasts
ABNORMAL_DURATION = 45

# Minimum gap before another abnormal episode can start
NORMAL_DURATION = 30


# ============================================================
# Create Kafka Producer
# ============================================================

def create_producer():
    return KafkaProducer(
        bootstrap_servers=KAFKA_SERVER,
        value_serializer=lambda value: json.dumps(value).encode("utf-8")
    )


# ============================================================
# Normal Vital Generators
# ============================================================

def generate_normal_heart_rate():
    return random.randint(68, 85)


def generate_normal_spo2():
    return random.randint(97, 100)


def generate_normal_blood_pressure():
    systolic = random.randint(115, 130)
    diastolic = random.randint(70, 85)

    return systolic, diastolic


def generate_normal_temperature():
    return round(random.uniform(36.5, 37.1), 1)


# ============================================================
# Abnormal Vital Generators
# ============================================================

def generate_abnormal_heart_rate():
    return random.randint(120, 135)


def generate_abnormal_spo2():
    return random.randint(85, 91)


def generate_abnormal_blood_pressure():
    systolic = random.randint(160, 175)
    diastolic = random.randint(100, 110)

    return systolic, diastolic


def generate_abnormal_temperature():
    return round(random.uniform(38.5, 39.5), 1)


# ============================================================
# Create Vital
# ============================================================

def create_vital(patient, vital_type, abnormal_vital=None):

    timestamp = datetime.now(timezone.utc).isoformat()

    is_abnormal = vital_type == abnormal_vital

    # --------------------------------------------------------
    # Heart Rate
    # --------------------------------------------------------

    if vital_type == "Heart Rate":

        value = (
            generate_abnormal_heart_rate()
            if is_abnormal
            else generate_normal_heart_rate()
        )

        return {
            "patientId": patient["patientId"],
            "type": "Heart Rate",
            "value": str(value),
            "unit": "bpm",
            "recordedAt": timestamp,
            "systolic": None,
            "diastolic": None
        }

    # --------------------------------------------------------
    # SpO2
    # --------------------------------------------------------

    elif vital_type == "SpO2":

        value = (
            generate_abnormal_spo2()
            if is_abnormal
            else generate_normal_spo2()
        )

        return {
            "patientId": patient["patientId"],
            "type": "SpO2",
            "value": str(value),
            "unit": "%",
            "recordedAt": timestamp,
            "systolic": None,
            "diastolic": None
        }

    # --------------------------------------------------------
    # Blood Pressure
    # --------------------------------------------------------

    elif vital_type == "Blood Pressure":

        if is_abnormal:
            systolic, diastolic = generate_abnormal_blood_pressure()
        else:
            systolic, diastolic = generate_normal_blood_pressure()

        return {
            "patientId": patient["patientId"],
            "type": "Blood Pressure",
            "value": None,
            "unit": "mmHg",
            "recordedAt": timestamp,
            "systolic": systolic,
            "diastolic": diastolic
        }

    # --------------------------------------------------------
    # Temperature
    # --------------------------------------------------------

    elif vital_type == "Temperature":

        value = (
            generate_abnormal_temperature()
            if is_abnormal
            else generate_normal_temperature()
        )

        return {
            "patientId": patient["patientId"],
            "type": "Temperature",
            "value": str(value),
            "unit": "°C",
            "recordedAt": timestamp,
            "systolic": None,
            "diastolic": None
        }


# ============================================================
# Main
# ============================================================

def main():

    print("=" * 70)
    print("MediSphere Wearable Simulator")
    print("=" * 70)

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
        print("Abnormal episodes will last approximately 45 seconds.")
        print("Only ONE abnormal vital will be active at a time.")
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


    # --------------------------------------------------------
    # Episode State
    # --------------------------------------------------------

    abnormal_vital = None
    episode_start_time = None

    last_episode_end_time = time.time()

    # Change this to control how frequently abnormal episodes
    # can start.
    next_episode_after = NORMAL_DURATION


    try:

        while True:

            current_time = time.time()


            # =================================================
            # Start a New Abnormal Episode
            # =================================================

            if (
                abnormal_vital is None
                and current_time - last_episode_end_time >= next_episode_after
            ):

                abnormal_vital = random.choice(vital_types)

                episode_start_time = current_time

                print()
                print("!" * 70)
                print(
                    f"⚠ ABNORMAL EPISODE STARTED | "
                    f"Vital: {abnormal_vital}"
                )
                print("!" * 70)
                print()


            # =================================================
            # End Abnormal Episode
            # =================================================

            if (
                abnormal_vital is not None
                and current_time - episode_start_time >= ABNORMAL_DURATION
            ):

                print()
                print("-" * 70)
                print(
                    f"✓ ABNORMAL EPISODE ENDED | "
                    f"{abnormal_vital} returned to normal"
                )
                print("-" * 70)
                print()

                abnormal_vital = None
                episode_start_time = None

                last_episode_end_time = current_time

                next_episode_after = NORMAL_DURATION


            # =================================================
            # Generate Patient Vitals
            # =================================================

            for patient in PATIENTS:

                for vital_type in vital_types:

                    vital = create_vital(
                        patient,
                        vital_type,
                        abnormal_vital
                    )


                    # ------------------------------------------------
                    # Send to Kafka
                    # ------------------------------------------------

                    producer.send(
                        KAFKA_TOPIC,
                        key=patient["patientId"].encode("utf-8"),
                        value=vital
                    )

                    producer.flush()


                    # ------------------------------------------------
                    # Display in Simulator
                    # ------------------------------------------------

                    display_value = (
                        vital["value"]
                        if vital["value"] is not None
                        else f"{vital['systolic']}/{vital['diastolic']}"
                    )


                    status = (
                        "⚠ ABNORMAL"
                        if vital_type == abnormal_vital
                        else "NORMAL"
                    )


                    print(
                        f"{patient['patientId']} | "
                        f"{vital_type} | "
                        f"{display_value} {vital['unit']} | "
                        f"{status}"
                    )


            print("-" * 70)

            # Wait 5 seconds before next batch
            time.sleep(5)


    except KeyboardInterrupt:

        print()
        print("Wearable simulator stopped.")


    finally:

        producer.close()


# ============================================================
# Program Entry
# ============================================================

if __name__ == "__main__":
    main()