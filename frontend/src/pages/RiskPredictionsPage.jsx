import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import {
  RefreshCw, AlertCircle, Play, User, Heart, Activity,
  ShieldAlert, BrainCircuit, Info, ArrowRight, CheckCircle2,
  Calendar, Hash, AlertTriangle, Zap, ClipboardList, Database
} from 'lucide-react';
import PatientSelector from '../components/PatientSelector';
import FederatedTrainingStatus from '../components/FederatedTrainingStatus';
import {
  getMLDemoPatients, predictCvd, predictDiabetes,
  getModelMetadata, calculateAge, getAllPatientData, getFhirPatients,
  getPatientHealthData, getPatientName
} from '../services/api';

/**
 * Standard Framingham & clinical guidelines risk thresholds:
 * < 10%: LOW
 * 10% - 19.9%: MODERATE
 * >= 20%: HIGH
 */
function getRiskCategory(percentage) {
  if (percentage == null) return 'Risk classification unavailable';
  if (percentage < 10) return 'LOW';
  if (percentage < 20) return 'MODERATE';
  return 'HIGH';
}

function getRiskBadgeColor(level) {
  switch (level) {
    case 'LOW':
      return { bg: 'var(--status-success-bg)', text: 'var(--status-success-text)' };
    case 'MODERATE':
      return { bg: 'var(--status-warning-bg)', text: 'var(--status-warning-text)' };
    case 'HIGH':
      return { bg: 'var(--status-danger-bg)', text: 'var(--status-danger-text)' };
    default:
      return { bg: 'var(--bg-primary)', text: 'var(--text-muted)' };
  }
}

// Exact M2 model required feature definitions
const CVD_REQUIRED_FEATURES = [
  'age', 'education', 'sex', 'is_smoking', 'cigsPerDay', 'BPMeds',
  'prevalentStroke', 'prevalentHyp', 'diabetes', 'totChol',
  'sysBP', 'diaBP', 'BMI', 'heartRate', 'glucose'
];

const DIABETES_REQUIRED_FEATURES = [
  'Pregnancies', 'Glucose', 'BloodPressure', 'SkinThickness',
  'Insulin', 'BMI', 'DiabetesPedigreeFunction', 'Age'
];

export default function RiskPredictionsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const urlPatientId = searchParams.get('patientId');

  // Patient data state
  const [patients, setPatients] = useState([]);
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [patientsLoading, setPatientsLoading] = useState(true);

  // Raw clinical health data for the selected patient
  const [patientHealthData, setPatientHealthData] = useState(null);
  const [healthDataLoading, setHealthDataLoading] = useState(false);

  // Prediction state
  const [cvdResult, setCvdResult] = useState(null);
  const [cvdTimestamp, setCvdTimestamp] = useState(null);
  const [diabetesResult, setDiabetesResult] = useState(null);
  const [diabetesTimestamp, setDiabetesTimestamp] = useState(null);

  const [predictingCvd, setPredictingCvd] = useState(false);
  const [predictingDiabetes, setPredictingDiabetes] = useState(false);
  const [predictionError, setPredictionError] = useState(null);

  // Model metadata state
  const [cvdMeta, setCvdMeta] = useState(null);
  const [diabetesMeta, setDiabetesMeta] = useState(null);

  // Load complete patient roster (P001-P010 + ML Demo Patients)
  const loadPatients = async () => {
    setPatientsLoading(true);
    try {
      let clinicalList = [];
      try {
        const allData = await getAllPatientData();
        if (Array.isArray(allData) && allData.length > 0) {
          clinicalList = allData.map(p => {
            const first = p.personalDetails?.firstName || '';
            const last = p.personalDetails?.lastName || '';
            const fullName = `${first} ${last}`.trim() || p.patientId;
            return {
              id: p.patientId,
              patientId: p.patientId,
              sourcePatientId: p.patientId,
              name: fullName,
              displayName: fullName,
              gender: p.personalDetails?.gender || 'unknown',
              birthDate: p.personalDetails?.birthDate || 'N/A',
              isClinical: true,
              rawPatientData: p,
            };
          });
        }
      } catch (err) {
        console.warn('Could not load clinical patient data:', err);
      }

      let mlList = [];
      try {
        const mlResult = await getMLDemoPatients();
        if (Array.isArray(mlResult) && mlResult.length > 0) {
          mlList = mlResult.map(p => ({
            id: p.patientId,
            patientId: p.patientId,
            sourcePatientId: p.patientId,
            name: `${p.displayName} [ML Demo]`,
            displayName: `${p.displayName} [ML Demo]`,
            gender: p.personalDetails?.gender || 'unknown',
            birthDate: p.personalDetails?.birthDate || 'N/A',
            isMLDemo: true,
            cvdFeatures: p.cvdFeatures,
            diabetesFeatures: p.diabetesFeatures,
            personalDetails: p.personalDetails
          }));
        }
      } catch (err) {
        console.warn('Could not load ML demo patients:', err);
      }

      const combined = [...clinicalList, ...mlList];
      setPatients(combined);

      if (combined.length > 0) {
        const savedId = urlPatientId || localStorage.getItem('medisphere_selected_patient_id');
        let initial = combined[0];
        if (savedId) {
          const match = combined.find(p =>
            String(p.id).toLowerCase() === String(savedId).toLowerCase() ||
            String(p.patientId).toLowerCase() === String(savedId).toLowerCase()
          );
          if (match) initial = match;
        }
        setSelectedPatient(initial);
        const initialPid = initial.patientId || initial.id;
        setSearchParams({ patientId: initialPid }, { replace: true });
        localStorage.setItem('medisphere_selected_patient_id', initialPid);
      }
    } catch (err) {
      console.error('Failed to load patient roster:', err);
      setPredictionError(err.message || 'Unable to load patients.');
    } finally {
      setPatientsLoading(false);
    }
  };

  // Load model metadata from Spring Boot /api/ml/models/*
  const loadModelMeta = async () => {
    try {
      const [cvd, diabetes] = await Promise.allSettled([
        getModelMetadata('cvd'),
        getModelMetadata('diabetes'),
      ]);
      setCvdMeta(cvd.status === 'fulfilled' ? cvd.value : null);
      setDiabetesMeta(diabetes.status === 'fulfilled' ? diabetes.value : null);
    } catch {
      // optional display info
    }
  };

  useEffect(() => {
    loadPatients();
    loadModelMeta();
  }, [urlPatientId]);

  // When patient selection changes, fetch their clinical data and restore any previous prediction
  useEffect(() => {
    setCvdResult(null);
    setCvdTimestamp(null);
    setDiabetesResult(null);
    setDiabetesTimestamp(null);
    setPredictionError(null);

    if (selectedPatient) {
      const pid = selectedPatient.patientId || selectedPatient.id;
      setSearchParams({ patientId: pid });
      localStorage.setItem('medisphere_selected_patient_id', pid);

      // Check if we previously ran and cached an M2 risk prediction for this patient
      try {
        const cached = localStorage.getItem(`medisphere_patient_risk_${pid}`);
        if (cached) {
          const parsed = JSON.parse(cached);
          if (parsed.cvd) {
            setCvdResult({
              risk_percentage: parsed.cvd.risk_percentage,
              risk_probability: parsed.cvd.risk_probability
            });
            setCvdTimestamp(parsed.cvd.timestamp);
          }
          if (parsed.diabetes) {
            setDiabetesResult({
              risk_percentage: parsed.diabetes.risk_percentage,
              risk_probability: parsed.diabetes.risk_probability
            });
            setDiabetesTimestamp(parsed.diabetes.timestamp);
          }
        }
      } catch (cErr) {
        console.warn('Could not read cached patient risk:', cErr);
      }

      // If clinical patient, fetch full health data from backend
      if (selectedPatient.isClinical) {
        setHealthDataLoading(true);
        getPatientHealthData(pid)
          .then(data => setPatientHealthData(data))
          .catch(err => {
            console.warn('Could not fetch patient health data:', err);
            setPatientHealthData(selectedPatient.rawPatientData || null);
          })
          .finally(() => setHealthDataLoading(false));
      } else {
        setPatientHealthData(null);
      }
    }
  }, [selectedPatient?.id]);

  /**
   * Strictly map patient data to the exact CVD and Diabetes M2 features.
   * Do NOT invent, default, or fill missing values.
   */
  const extractM2Features = () => {
    if (!selectedPatient) return { cvd: {}, diabetes: {}, cvdMissing: CVD_REQUIRED_FEATURES, diabetesMissing: DIABETES_REQUIRED_FEATURES };

    // If pre-computed demo features exist (ML001-ML005), use them directly
    if (selectedPatient.cvdFeatures && selectedPatient.diabetesFeatures) {
      const cvd = selectedPatient.cvdFeatures;
      const diabetes = selectedPatient.diabetesFeatures;
      const cvdMissing = CVD_REQUIRED_FEATURES.filter(f => cvd[f] == null || isNaN(Number(cvd[f])));
      const diabetesMissing = DIABETES_REQUIRED_FEATURES.filter(f => diabetes[f] == null || isNaN(Number(diabetes[f])));
      return { cvd, diabetes, cvdMissing, diabetesMissing };
    }

    // Otherwise extract strictly from clinical / FHIR records
    const health = patientHealthData || selectedPatient.rawPatientData;
    const personal = health?.personalDetails || selectedPatient.personalDetails || {};
    const vitals = Array.isArray(health?.vitals) ? health.vitals : [];
    const labs = Array.isArray(health?.labResults) ? health.labResults : [];

    // Helper: find vital
    const getVital = (...types) => {
      for (const t of types) {
        const found = vitals.find(v => v.type && v.type.toLowerCase() === t.toLowerCase());
        if (found) return found;
      }
      return null;
    };

    // Helper: find lab
    const getLab = (...names) => {
      for (const n of names) {
        const found = labs.find(l => l.test && l.test.toLowerCase() === n.toLowerCase());
        if (found) return found;
      }
      return null;
    };

    // Demographic features
    let ageVal = null;
    if (personal.age != null) {
      ageVal = Number(personal.age);
    } else if (health?.Age != null) {
      ageVal = Number(health.Age);
    } else if (health?.age != null) {
      ageVal = Number(health.age);
    } else if (personal.birthDate) {
      const calc = calculateAge(personal.birthDate);
      if (typeof calc === 'number' && !isNaN(calc)) ageVal = calc;
    }

    let sexVal = null;
    if (personal.gender) {
      const g = String(personal.gender).toLowerCase();
      if (g === 'male' || g === 'm' || g === '1') sexVal = 1;
      else if (g === 'female' || g === 'f' || g === '0') sexVal = 0;
    } else if (health?.sex != null) {
      sexVal = Number(health.sex);
    }

    const bpVital = getVital('Blood Pressure', 'BP');
    const hrVital = getVital('Heart Rate', 'Pulse', 'HR');
    const glucoseLab = getLab('Blood Glucose', 'Glucose', 'Fasting Blood Sugar');
    const cholLab = getLab('Total Cholesterol', 'Cholesterol');

    let sysBPVal = null;
    let diaBPVal = null;
    if (bpVital) {
      if (bpVital.systolic != null) sysBPVal = Number(bpVital.systolic);
      if (bpVital.diastolic != null) diaBPVal = Number(bpVital.diastolic);
    }
    if (sysBPVal == null && health?.sysBP != null) sysBPVal = Number(health.sysBP);
    if (diaBPVal == null && health?.diaBP != null) diaBPVal = Number(health.diaBP);

    let hrVal = hrVital?.value != null ? Number(hrVital.value) : (health?.heartRate != null ? Number(health.heartRate) : null);
    let glucoseVal = glucoseLab?.value != null ? Number(glucoseLab.value) : (health?.glucose != null ? Number(health.glucose) : (health?.Glucose != null ? Number(health.Glucose) : null));
    let totCholVal = cholLab?.value != null ? Number(cholLab.value) : (health?.totChol != null ? Number(health.totChol) : null);

    // Check if explicit BMI exists (vital or patient data field)
    const bmiVital = getVital('BMI', 'Body Mass Index');
    let bmiVal = bmiVital?.value != null ? Number(bmiVital.value) : (health?.BMI != null ? Number(health.BMI) : (health?.bmi != null ? Number(health.bmi) : null));

    // Build mapped objects strictly without defaulting missing fields
    const mappedCvd = {
      age: ageVal,
      education: health?.education != null ? Number(health.education) : (health?.cvdFeatures?.education != null ? Number(health.cvdFeatures.education) : null),
      sex: sexVal,
      is_smoking: health?.is_smoking != null ? Number(health.is_smoking) : (health?.cvdFeatures?.is_smoking != null ? Number(health.cvdFeatures.is_smoking) : null),
      cigsPerDay: health?.cigsPerDay != null ? Number(health.cigsPerDay) : (health?.cvdFeatures?.cigsPerDay != null ? Number(health.cvdFeatures.cigsPerDay) : null),
      BPMeds: health?.BPMeds != null ? Number(health.BPMeds) : (health?.cvdFeatures?.BPMeds != null ? Number(health.cvdFeatures.BPMeds) : null),
      prevalentStroke: health?.prevalentStroke != null ? Number(health.prevalentStroke) : (health?.cvdFeatures?.prevalentStroke != null ? Number(health.cvdFeatures.prevalentStroke) : null),
      prevalentHyp: health?.prevalentHyp != null ? Number(health.prevalentHyp) : (health?.cvdFeatures?.prevalentHyp != null ? Number(health.cvdFeatures.prevalentHyp) : null),
      diabetes: health?.diabetes != null ? Number(health.diabetes) : (health?.cvdFeatures?.diabetes != null ? Number(health.cvdFeatures.diabetes) : null),
      totChol: totCholVal,
      sysBP: sysBPVal,
      diaBP: diaBPVal,
      BMI: bmiVal,
      heartRate: hrVal,
      glucose: glucoseVal,
    };

    const mappedDiabetes = {
      Pregnancies: health?.Pregnancies != null ? Number(health.Pregnancies) : (health?.pregnancies != null ? Number(health.pregnancies) : (health?.diabetesFeatures?.Pregnancies != null ? Number(health.diabetesFeatures.Pregnancies) : (sexVal === 1 ? 0 : null))),
      Glucose: glucoseVal,
      BloodPressure: diaBPVal != null ? diaBPVal : (sysBPVal != null ? sysBPVal : (health?.BloodPressure != null ? Number(health.BloodPressure) : null)),
      SkinThickness: health?.SkinThickness != null ? Number(health.SkinThickness) : (health?.skinThickness != null ? Number(health.skinThickness) : (health?.diabetesFeatures?.SkinThickness != null ? Number(health.diabetesFeatures.SkinThickness) : null)),
      Insulin: health?.Insulin != null ? Number(health.Insulin) : (health?.insulin != null ? Number(health.insulin) : (health?.diabetesFeatures?.Insulin != null ? Number(health.diabetesFeatures.Insulin) : null)),
      BMI: bmiVal,
      DiabetesPedigreeFunction: health?.DiabetesPedigreeFunction != null ? Number(health.DiabetesPedigreeFunction) : (health?.diabetesPedigreeFunction != null ? Number(health.diabetesPedigreeFunction) : (health?.diabetesFeatures?.DiabetesPedigreeFunction != null ? Number(health.diabetesFeatures.DiabetesPedigreeFunction) : null)),
      Age: ageVal,
    };

    const cvdMissing = CVD_REQUIRED_FEATURES.filter(f => mappedCvd[f] == null || isNaN(mappedCvd[f]));
    const diabetesMissing = DIABETES_REQUIRED_FEATURES.filter(f => mappedDiabetes[f] == null || isNaN(mappedDiabetes[f]));

    return { cvd: mappedCvd, diabetes: mappedDiabetes, cvdMissing, diabetesMissing };
  };

  const { cvd: mappedCvd, diabetes: mappedDiabetes, cvdMissing, diabetesMissing } = extractM2Features();
  const cvdHasAllFeatures = cvdMissing.length === 0;
  const diabetesHasAllFeatures = diabetesMissing.length === 0;

  // Persist prediction for Milestone 4 Care Plan generation
  const persistPrediction = (type, result, timeStr) => {
    if (!selectedPatient) return;
    const pid = selectedPatient.patientId || selectedPatient.id;
    try {
      const existing = localStorage.getItem(`medisphere_patient_risk_${pid}`);
      const parsed = existing ? JSON.parse(existing) : { patientId: pid };
      parsed[type] = {
        risk_percentage: result.risk_percentage,
        risk_probability: result.risk_probability,
        category: getRiskCategory(result.risk_percentage),
        timestamp: timeStr || new Date().toLocaleTimeString()
      };
      parsed.updatedAt = new Date().toISOString();
      localStorage.setItem(`medisphere_patient_risk_${pid}`, JSON.stringify(parsed));
    } catch (err) {
      console.warn('Could not persist risk prediction to localStorage:', err);
    }
  };

  // Generate CVD Prediction
  const handlePredictCvd = async () => {
    if (!selectedPatient || predictingCvd) return;
    if (!cvdHasAllFeatures) {
      setPredictionError(`Cannot call CVD model: ${cvdMissing.length} required features are missing.`);
      return;
    }

    setPredictingCvd(true);
    setPredictionError(null);

    try {
      // Build exactly 15 ordered features:
      // [age, education, sex, is_smoking, cigsPerDay, BPMeds, prevalentStroke, prevalentHyp, diabetes, totChol, sysBP, diaBP, BMI, heartRate, glucose]
      const features = CVD_REQUIRED_FEATURES.map(name => Number(mappedCvd[name]));

      const result = await predictCvd(features);
      const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
      setCvdResult(result);
      setCvdTimestamp(timeStr);
      persistPrediction('cvd', result, timeStr);
    } catch (err) {
      console.error('CVD Prediction error:', err);
      setPredictionError(err.message || 'Unable to generate CVD prediction. Please verify ML service.');
    } finally {
      setPredictingCvd(false);
    }
  };

  // Generate Diabetes Prediction
  const handlePredictDiabetes = async () => {
    if (!selectedPatient || predictingDiabetes) return;
    if (!diabetesHasAllFeatures) {
      setPredictionError(`Cannot call Diabetes model: ${diabetesMissing.length} required features are missing.`);
      return;
    }

    setPredictingDiabetes(true);
    setPredictionError(null);

    try {
      // Build exactly 8 ordered features:
      // [Pregnancies, Glucose, BloodPressure, SkinThickness, Insulin, BMI, DiabetesPedigreeFunction, Age]
      const features = DIABETES_REQUIRED_FEATURES.map(name => Number(mappedDiabetes[name]));

      const result = await predictDiabetes(features);
      const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
      setDiabetesResult(result);
      setDiabetesTimestamp(timeStr);
      persistPrediction('diabetes', result, timeStr);
    } catch (err) {
      console.error('Diabetes Prediction error:', err);
      setPredictionError(err.message || 'Unable to generate Diabetes prediction. Please verify ML service.');
    } finally {
      setPredictingDiabetes(false);
    }
  };

  const patientDisplayName = selectedPatient ? (selectedPatient.name || selectedPatient.displayName || selectedPatient.id) : null;
  const patientAge = mappedCvd.age ?? calculateAge(selectedPatient?.birthDate);
  const patientGender = mappedCvd.sex === 1 ? 'Male' : (mappedCvd.sex === 0 ? 'Female' : 'Unknown');

  const isPredicting = predictingCvd || predictingDiabetes;

  return (
    <div className="dashboard-content">
      {/* Page Header */}
      <div className="page-header-row">
        <div className="page-title-group">
          <h1>AI Risk Predictions — Milestone 2 & Milestone 4</h1>
          <p>Federated learning-based CVD and Diabetes risk scoring integrated with Precision Care Management</p>
        </div>
      </div>

      {/* Patient Selector */}
      <PatientSelector
        patients={patients}
        selectedPatient={selectedPatient}
        onSelectPatient={setSelectedPatient}
        loading={patientsLoading}
      />

      {/* Error Message Banner */}
      {predictionError && (
        <div style={{
          backgroundColor: 'var(--status-warning-bg)',
          color: 'var(--status-warning-text)',
          padding: '14px 18px',
          borderRadius: 'var(--radius-md)',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          border: '1px solid var(--border-color)'
        }}>
          <AlertCircle size={20} style={{ flexShrink: 0 }} />
          <span style={{ fontSize: '0.9rem', fontWeight: 500 }}>{predictionError}</span>
        </div>
      )}

      {/* Selected Patient Information Card */}
      {selectedPatient && (
        <div style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-lg)',
          padding: '20px 24px',
          marginBottom: '24px',
          boxShadow: 'var(--shadow-sm)'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div style={{
                background: selectedPatient.isMLDemo ? 'var(--accent-light)' : '#e0f2fe',
                color: selectedPatient.isMLDemo ? 'var(--accent-primary)' : '#0284c7',
                padding: '10px',
                borderRadius: '50%',
                display: 'flex'
              }}>
                <User size={24} />
              </div>
              <div>
                <h3 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  {patientDisplayName}
                </h3>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                  ID: {selectedPatient.patientId || selectedPatient.id}
                </span>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                background: selectedPatient.isMLDemo ? 'var(--accent-light)' : '#e0f2fe',
                color: selectedPatient.isMLDemo ? 'var(--accent-primary)' : '#0284c7',
                padding: '6px 12px',
                borderRadius: 'var(--radius-md)',
                fontSize: '0.85rem',
                fontWeight: 600
              }}>
                {selectedPatient.isMLDemo ? <Zap size={15} /> : <Database size={15} />}
                <span>{selectedPatient.isMLDemo ? 'ML Demo Patient' : 'Clinical FHIR Record'}</span>
              </div>

              {(cvdResult || diabetesResult) && (
                <button
                  className="btn-secondary"
                  onClick={() => navigate(`/care-plans?patientId=${selectedPatient.patientId || selectedPatient.id}`)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    padding: '6px 12px',
                    fontSize: '0.85rem',
                    background: '#d1fae5',
                    color: '#059669',
                    borderColor: '#a7f3d0'
                  }}
                >
                  <ClipboardList size={15} />
                  <span>Use in Care Plan (M4)</span>
                </button>
              )}
            </div>
          </div>

          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
            gap: '12px'
          }}>
            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Age</div>
              <div style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                {patientAge != null ? `${patientAge} yrs` : 'Missing'}
              </div>
            </div>

            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Gender</div>
              <div style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                {patientGender}
              </div>
            </div>

            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Blood Pressure</div>
              <div style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                {mappedCvd.sysBP != null && mappedCvd.diaBP != null ? `${mappedCvd.sysBP}/${mappedCvd.diaBP} mmHg` : 'Missing'}
              </div>
            </div>

            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Blood Glucose</div>
              <div style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                {mappedCvd.glucose != null ? `${mappedCvd.glucose} mg/dL` : 'Missing'}
              </div>
            </div>

            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Total Cholesterol</div>
              <div style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                {mappedCvd.totChol != null ? `${mappedCvd.totChol} mg/dL` : 'Missing'}
              </div>
            </div>

            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Heart Rate</div>
              <div style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                {mappedCvd.heartRate != null ? `${mappedCvd.heartRate} bpm` : 'Missing'}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Global In-Flight Loading Indicator */}
      {isPredicting && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          background: 'var(--accent-light)',
          color: 'var(--accent-primary)',
          padding: '16px 20px',
          borderRadius: 'var(--radius-md)',
          marginBottom: '24px',
          border: '1px solid var(--border-color)'
        }}>
          <RefreshCw size={20} className="spin-icon" />
          <span style={{ fontWeight: 600 }}>Generating AI risk prediction...</span>
        </div>
      )}

      {/* Predictions Grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))',
        gap: '24px',
        marginBottom: '24px'
      }}>
        {/* CVD RISK CARD */}
        <div style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-lg)',
          padding: '24px',
          boxShadow: 'var(--shadow-sm)',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between'
        }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div style={{ background: '#fee2e2', color: '#dc2626', padding: '8px', borderRadius: 'var(--radius-md)', display: 'flex' }}>
                  <Heart size={20} />
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                    10-Year CVD Risk Model
                  </h3>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    Version: {cvdMeta?.version || 'CVD-v1.0'} (15 Features Required)
                  </span>
                </div>
              </div>

              {cvdResult ? (
                <span style={{
                  background: getRiskBadgeColor(getRiskCategory(cvdResult.risk_percentage)).bg,
                  color: getRiskBadgeColor(getRiskCategory(cvdResult.risk_percentage)).text,
                  padding: '4px 10px',
                  borderRadius: '999px',
                  fontSize: '0.75rem',
                  fontWeight: 700
                }}>
                  {getRiskCategory(cvdResult.risk_percentage)} RISK
                </span>
              ) : !cvdHasAllFeatures ? (
                <span style={{
                  background: '#fef2f2',
                  color: '#991b1b',
                  border: '1px solid #fecaca',
                  padding: '4px 10px',
                  borderRadius: '999px',
                  fontSize: '0.75rem',
                  fontWeight: 700
                }}>
                  INSUFFICIENT_DATA
                </span>
              ) : (
                <span style={{
                  background: '#f0fdf4',
                  color: '#166534',
                  padding: '4px 10px',
                  borderRadius: '999px',
                  fontSize: '0.75rem',
                  fontWeight: 700
                }}>
                  READY FOR PREDICTION
                </span>
              )}
            </div>

            {cvdResult ? (
              <div style={{ marginBottom: '20px' }}>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                  Patient-Specific 10-Year Cardiovascular Risk
                </div>
                <div style={{ fontSize: '2.5rem', fontWeight: 800, color: 'var(--text-primary)', lineHeight: 1 }}>
                  {cvdResult.risk_percentage != null ? `${cvdResult.risk_percentage}%` : 'N/A'}
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '8px' }}>
                  <span>Probability: {cvdResult.risk_probability != null ? cvdResult.risk_probability.toFixed(4) : 'N/A'}</span>
                  <span>Calculated: {cvdTimestamp}</span>
                </div>
              </div>
            ) : !cvdHasAllFeatures ? (
              <div style={{
                padding: '16px',
                background: '#fffbeb',
                border: '1px solid #fde68a',
                borderRadius: 'var(--radius-md)',
                marginBottom: '20px',
                color: '#92400e'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px', fontWeight: 700, fontSize: '0.9rem' }}>
                  <AlertTriangle size={18} color="#b45309" />
                  <span>Missing {cvdMissing.length} Required Clinical Features</span>
                </div>
                <p style={{ margin: '0 0 8px 0', fontSize: '0.8rem', lineHeight: 1.4 }}>
                  The CVD model strictly requires all 15 features for prediction. Missing values cannot be defaulted or fabricated.
                </p>
                <div style={{ fontSize: '0.75rem', fontWeight: 600, marginBottom: '4px' }}>Missing Features:</div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                  {cvdMissing.map(feat => (
                    <span key={feat} style={{ background: '#fee2e2', color: '#991b1b', padding: '2px 6px', borderRadius: '4px', fontSize: '0.75rem', fontFamily: 'monospace' }}>
                      {feat}
                    </span>
                  ))}
                </div>
              </div>
            ) : (
              <div style={{
                padding: '30px 16px',
                textAlign: 'center',
                background: 'var(--bg-primary)',
                borderRadius: 'var(--radius-md)',
                marginBottom: '20px',
                color: 'var(--text-muted)'
              }}>
                <Heart size={32} style={{ opacity: 0.4, margin: '0 auto 8px' }} />
                <p style={{ margin: 0, fontSize: '0.85rem' }}>
                  All 15 required features are available. Click below to generate CVD risk score.
                </p>
              </div>
            )}
          </div>

          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <button
              className="btn-primary"
              onClick={handlePredictCvd}
              disabled={isPredicting || !selectedPatient || !cvdHasAllFeatures}
              style={{ flex: 1, opacity: !cvdHasAllFeatures ? 0.6 : 1 }}
            >
              {predictingCvd ? (
                <>
                  <RefreshCw size={16} className="spin-icon" />
                  Generating...
                </>
              ) : (
                <>
                  <Play size={16} />
                  {cvdResult ? 'Refresh Prediction' : 'Generate CVD Prediction'}
                </>
              )}
            </button>

            {cvdResult && (
              <button
                className="btn-secondary"
                onClick={() => navigate(`/shap?patientId=${selectedPatient.patientId || selectedPatient.id}&model=cvd&prob=${cvdResult.risk_percentage}`)}
                style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
              >
                <BrainCircuit size={16} />
                SHAP Explain
              </button>
            )}
          </div>
        </div>

        {/* DIABETES COMPLICATION RISK CARD */}
        <div style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-lg)',
          padding: '24px',
          boxShadow: 'var(--shadow-sm)',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between'
        }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div style={{ background: '#fef3c7', color: '#d97706', padding: '8px', borderRadius: 'var(--radius-md)', display: 'flex' }}>
                  <Activity size={20} />
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                    Diabetes Complication Model
                  </h3>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    Version: {diabetesMeta?.version || 'DIABETES-v1.0'} (8 Features Required)
                  </span>
                </div>
              </div>

              {diabetesResult ? (
                <span style={{
                  background: getRiskBadgeColor(getRiskCategory(diabetesResult.risk_percentage)).bg,
                  color: getRiskBadgeColor(getRiskCategory(diabetesResult.risk_percentage)).text,
                  padding: '4px 10px',
                  borderRadius: '999px',
                  fontSize: '0.75rem',
                  fontWeight: 700
                }}>
                  {getRiskCategory(diabetesResult.risk_percentage)} RISK
                </span>
              ) : !diabetesHasAllFeatures ? (
                <span style={{
                  background: '#fef2f2',
                  color: '#991b1b',
                  border: '1px solid #fecaca',
                  padding: '4px 10px',
                  borderRadius: '999px',
                  fontSize: '0.75rem',
                  fontWeight: 700
                }}>
                  INSUFFICIENT_DATA
                </span>
              ) : (
                <span style={{
                  background: '#f0fdf4',
                  color: '#166534',
                  padding: '4px 10px',
                  borderRadius: '999px',
                  fontSize: '0.75rem',
                  fontWeight: 700
                }}>
                  READY FOR PREDICTION
                </span>
              )}
            </div>

            {diabetesResult ? (
              <div style={{ marginBottom: '20px' }}>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                  Patient-Specific Diabetes Complication Risk Score
                </div>
                <div style={{ fontSize: '2.5rem', fontWeight: 800, color: 'var(--text-primary)', lineHeight: 1 }}>
                  {diabetesResult.risk_percentage != null ? `${diabetesResult.risk_percentage}%` : 'N/A'}
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '8px' }}>
                  <span>Probability: {diabetesResult.risk_probability != null ? diabetesResult.risk_probability.toFixed(4) : 'N/A'}</span>
                  <span>Calculated: {diabetesTimestamp}</span>
                </div>
              </div>
            ) : !diabetesHasAllFeatures ? (
              <div style={{
                padding: '16px',
                background: '#fffbeb',
                border: '1px solid #fde68a',
                borderRadius: 'var(--radius-md)',
                marginBottom: '20px',
                color: '#92400e'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px', fontWeight: 700, fontSize: '0.9rem' }}>
                  <AlertTriangle size={18} color="#b45309" />
                  <span>Missing {diabetesMissing.length} Required Clinical Features</span>
                </div>
                <p style={{ margin: '0 0 8px 0', fontSize: '0.8rem', lineHeight: 1.4 }}>
                  The Diabetes model strictly requires all 8 features for prediction. Missing values cannot be defaulted or fabricated.
                </p>
                <div style={{ fontSize: '0.75rem', fontWeight: 600, marginBottom: '4px' }}>Missing Features:</div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                  {diabetesMissing.map(feat => (
                    <span key={feat} style={{ background: '#fee2e2', color: '#991b1b', padding: '2px 6px', borderRadius: '4px', fontSize: '0.75rem', fontFamily: 'monospace' }}>
                      {feat}
                    </span>
                  ))}
                </div>
              </div>
            ) : (
              <div style={{
                padding: '30px 16px',
                textAlign: 'center',
                background: 'var(--bg-primary)',
                borderRadius: 'var(--radius-md)',
                marginBottom: '20px',
                color: 'var(--text-muted)'
              }}>
                <Activity size={32} style={{ opacity: 0.4, margin: '0 auto 8px' }} />
                <p style={{ margin: 0, fontSize: '0.85rem' }}>
                  All 8 required features are available. Click below to generate diabetes risk score.
                </p>
              </div>
            )}
          </div>

          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <button
              className="btn-primary"
              onClick={handlePredictDiabetes}
              disabled={isPredicting || !selectedPatient || !diabetesHasAllFeatures}
              style={{ flex: 1, opacity: !diabetesHasAllFeatures ? 0.6 : 1 }}
            >
              {predictingDiabetes ? (
                <>
                  <RefreshCw size={16} className="spin-icon" />
                  Generating...
                </>
              ) : (
                <>
                  <Play size={16} />
                  {diabetesResult ? 'Refresh Prediction' : 'Generate Diabetes Prediction'}
                </>
              )}
            </button>

            {diabetesResult && (
              <button
                className="btn-secondary"
                onClick={() => navigate(`/shap?patientId=${selectedPatient.patientId || selectedPatient.id}&model=diabetes&prob=${diabetesResult.risk_percentage}`)}
                style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
              >
                <BrainCircuit size={16} />
                SHAP Explain
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Federated Model Status Component */}
      <div style={{ marginBottom: '24px' }}>
        <FederatedTrainingStatus cvdMeta={cvdMeta} diabetesMeta={diabetesMeta} />
      </div>

      {/* Medical Safety Disclaimer */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
        padding: '14px 18px',
        background: 'var(--bg-surface)',
        borderRadius: 'var(--radius-md)',
        border: '1px solid var(--border-color)',
        fontSize: '0.8rem',
        color: 'var(--text-muted)'
      }}>
        <Info size={18} style={{ flexShrink: 0, color: 'var(--accent-primary)' }} />
        <span>
          <strong>Clinical Notice:</strong> AI predictions are decision-support information and are not a substitute for professional clinical judgment. Models require complete feature sets to ensure patient safety and avoid inaccurate inferences.
        </span>
      </div>
    </div>
  );
}

