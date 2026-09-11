import pandas as pd

# Load dataset
file_path = "data/framingham.csv"
df = pd.read_csv(file_path)

print("Original dataset shape:", df.shape)

# Remove unnecessary ID column
df = df.drop(columns=["id"])

# Convert categorical columns to numeric
df["sex"] = df["sex"].map({"F": 0, "M": 1})
df["is_smoking"] = df["is_smoking"].map({"NO": 0, "YES": 1})

# Fill missing numerical values with median
numeric_columns = [
    "education",
    "cigsPerDay",
    "BPMeds",
    "totChol",
    "BMI",
    "heartRate",
    "glucose"
]

for column in numeric_columns:
    df[column] = df[column].fillna(df[column].median())

# Check missing values after cleaning
print("\nMissing values after cleaning:")
print(df.isnull().sum())

# Save cleaned dataset
output_file = "data/framingham_cleaned.csv"
df.to_csv(output_file, index=False)

print("\nCleaned dataset saved successfully!")
print("Final shape:", df.shape)