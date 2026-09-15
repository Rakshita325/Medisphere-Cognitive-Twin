import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import {
  RefreshCw, AlertCircle, Play, User, Heart, Activity,
  ShieldAlert, BrainCircuit, Info, ArrowRight, CheckCircle2,
  Calendar, Hash, AlertTriangle, Zap
} from 'lucide-react';
import PatientSelector from '../components/PatientSelector';
import FederatedTrainingStatus from '../components/FederatedTrainingStatus';
import {
  getMLDemoPatients, predictCvd, predictDiabetes,
  getModelMetadata, calculateAge
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

export default function RiskPredictionsPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const urlPatientId = searchParams.get('patientId');

  // Patient data state
  const [patients, setPatients] = useState([]);
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [patientsLoading, setPatientsLoading] = useState(true);

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

  // Load ML demo patients (Milestone 2 synthetic patients for risk prediction)
  const loadPatients = async () => {
    setPatientsLoading(true);
    try {
      const result = await getMLDemoPatients();
      const list = Array.isArray(result) ? result : [];
      
      // Transform ML patient data to match expected format
      const transformedList = list.map(p => ({
        id: p.patientId,
        patientId: p.patientId,
        patientSource: p.patientSource,
        displayName: p.displayName,
        name: p.displayName,
        isMLDemo: true,
        // Store feature data for easy access during prediction
        cvdFeatures: p.cvdFeatures,
        diabetesFeatures: p.diabetesFeatures,
        personalDetails: p.personalDetails
      }));
      
      setPatients(transformedList);

      if (transformedList.length > 0) {
        if (urlPatientId) {
          const match = transformedList.find(p => String(p.id) === String(urlPatientId));
          if (match) {
            setSelectedPatient(match);
            return;
          }
        }
        if (!selectedPatient) {
          setSelectedPatient(transformedList[0]);
        }
      }
    } catch (err) {
      console.error('Failed to load ML demo patients:', err);
      setPredictionError(err.message || 'Unable to load ML demo patients for risk prediction.');
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

  // Clear predictions when patient selection changes
  useEffect(() => {
    setCvdResult(null);
    setCvdTimestamp(null);
    setDiabetesResult(null);
    setDiabetesTimestamp(null);
    setPredictionError(null);
  }, [selectedPatient]);

  /**
   * For ML demo patients, return pre-computed clinical values from the JSON.
   * No extraction needed - values are already validated and ml-ready.
   */
  const getClinicalValues = () => {
    if (!selectedPatient || !selectedPatient.cvdFeatures) return null;
    
    const cvd = selectedPatient.cvdFeatures;
    const diabetes = selectedPatient.diabetesFeatures;
    
    return {
      age: cvd.age ?? null,
      gender: cvd.sex ?? null,
      sysBP: cvd.sysBP ?? null,
      diaBP: cvd.diaBP ?? null,
      heartRate: cvd.heartRate ?? null,
      totChol: cvd.totChol ?? null,
      glucose: cvd.glucose ?? null,
      bmi: cvd.BMI ?? null,
      // Additional diabetes-specific values
      pregnancies: diabetes.Pregnancies ?? 0,
      skinThickness: diabetes.SkinThickness ?? null,
      insulin: diabetes.Insulin ?? null,
      dpf: diabetes.DiabetesPedigreeFunction ?? null
    };
  };

  // Generate CVD Prediction using pre-computed ML demo patient features
  const handlePredictCvd = async () => {
    if (!selectedPatient || predictingCvd) return;
    setPredictingCvd(true);
    setPredictionError(null);

    try {
      if (!selectedPatient.cvdFeatures) {
        throw new Error('CVD features not available for this patient.');
      }

      const cvd = selectedPatient.cvdFeatures;

      // Prepare exactly 15 features in order matching the model:
      // [age, education, sex, is_smoking, cigsPerDay, BPMeds, prevalentStroke, prevalentHyp, diabetes, totChol, sysBP, diaBP, BMI, heartRate, glucose]
      const features = [
        cvd.age,
        cvd.education,
        cvd.sex,
        cvd.is_smoking,
        cvd.cigsPerDay,
        cvd.BPMeds,
        cvd.prevalentStroke,
        cvd.prevalentHyp,
        cvd.diabetes,
        cvd.totChol,
        cvd.sysBP,
        cvd.diaBP,
        cvd.BMI,
        cvd.heartRate,
        cvd.glucose
      ];

      const result = await predictCvd(features);
      setCvdResult(result);
      setCvdTimestamp(new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }));
    } catch (err) {
      console.error('CVD Prediction error:', err);
      setPredictionError(err.message || 'Unable to generate prediction. Please try again.');
    } finally {
      setPredictingCvd(false);
    }
  };

  // Generate Diabetes Prediction using pre-computed ML demo patient features
  const handlePredictDiabetes = async () => {
    if (!selectedPatient || predictingDiabetes) return;
    setPredictingDiabetes(true);
    setPredictionError(null);

    try {
      if (!selectedPatient.diabetesFeatures) {
        throw new Error('Diabetes features not available for this patient.');
      }

      const diabetes = selectedPatient.diabetesFeatures;

      // Prepare exactly 8 features in order matching the model:
      // [Pregnancies, Glucose, BloodPressure, SkinThickness, Insulin, BMI, DiabetesPedigreeFunction, Age]
      const features = [
        diabetes.Pregnancies,
        diabetes.Glucose,
        diabetes.BloodPressure,
        diabetes.SkinThickness,
        diabetes.Insulin,
        diabetes.BMI,
        diabetes.DiabetesPedigreeFunction,
        diabetes.Age
      ];

      const result = await predictDiabetes(features);
      setDiabetesResult(result);
      setDiabetesTimestamp(new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }));
    } catch (err) {
      console.error('Diabetes Prediction error:', err);
      setPredictionError(err.message || 'Unable to generate prediction. Please try again.');
    } finally {
      setPredictingDiabetes(false);
    }
  };

  const patientName = selectedPatient ? `${selectedPatient.displayName} [ML Demo]` : null;
  const patientAge = selectedPatient?.personalDetails?.age ?? calculateAge(selectedPatient?.personalDetails?.birthDate);
  const patientGender = selectedPatient?.personalDetails?.gender
    ? selectedPatient.personalDetails.gender.charAt(0).toUpperCase() + selectedPatient.personalDetails.gender.slice(1)
    : 'Unknown';

  const isPredicting = predictingCvd || predictingDiabetes;
  const clinicalVals = getClinicalValues();

  return (
    <div className="dashboard-content">
      {/* Page Header */}
      <div className="page-header-row">
        <div className="page-title-group">
          <h1>AI Risk Predictions — Milestone 2 ML Demo</h1>
          <p>Federated learning-based CVD and Diabetes risk scoring on synthetic demonstration patients</p>
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
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div style={{
                background: 'var(--accent-light)',
                color: 'var(--accent-primary)',
                padding: '10px',
                borderRadius: '50%',
                display: 'flex'
              }}>
                <User size={24} />
              </div>
              <div>
                <h3 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  {patientName}
                </h3>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                  ID: {selectedPatient.id}
                </span>
              </div>
            </div>

            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              background: 'var(--accent-light)',
              color: 'var(--accent-primary)',
              padding: '8px 12px',
              borderRadius: 'var(--radius-md)',
              fontSize: '0.85rem',
              fontWeight: 500
            }}>
              <Zap size={16} />
              <span>Synthetic ML Demo Patient</span>
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
                {patientAge !== 'N/A' ? `${patientAge} years` : 'Unavailable'}
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
                {clinicalVals?.sysBP && clinicalVals?.diaBP ? `${clinicalVals.sysBP}/${clinicalVals.diaBP} mmHg` : 'Unavailable'}
              </div>
            </div>

            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Blood Glucose</div>
              <div style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                {clinicalVals?.glucose ? `${clinicalVals.glucose} mg/dL` : 'Unavailable'}
              </div>
            </div>

            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Total Cholesterol</div>
              <div style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                {clinicalVals?.totChol ? `${clinicalVals.totChol} mg/dL` : 'Unavailable'}
              </div>
            </div>

            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Heart Rate</div>
              <div style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                {clinicalVals?.heartRate ? `${clinicalVals.heartRate} bpm` : 'Unavailable'}
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
        gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
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
                    CVD Risk Model
                  </h3>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    Version: {cvdMeta?.version || 'CVD-v1.0'}
                  </span>
                </div>
              </div>

              {cvdResult && (
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
              )}
            </div>

            {cvdResult ? (
              <div style={{ marginBottom: '20px' }}>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                  10-Year Cardiovascular Risk
                </div>
                <div style={{ fontSize: '2.5rem', fontWeight: 800, color: 'var(--text-primary)', lineHeight: 1 }}>
                  {cvdResult.risk_percentage != null ? `${cvdResult.risk_percentage}%` : 'N/A'}
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '8px' }}>
                  <span>Probability: {cvdResult.risk_probability != null ? cvdResult.risk_probability.toFixed(4) : 'N/A'}</span>
                  <span>Timestamp: {cvdTimestamp}</span>
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
                  Click below to generate 10-year CVD risk score for this patient.
                </p>
              </div>
            )}
          </div>

          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <button
              className="btn-primary"
              onClick={handlePredictCvd}
              disabled={isPredicting || !selectedPatient}
              style={{ flex: 1 }}
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
                onClick={() => navigate(`/shap?patientId=${selectedPatient.id}&model=cvd&prob=${cvdResult.risk_percentage}`)}
                style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
              >
                <BrainCircuit size={16} />
                View Explanation
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
                    Version: {diabetesMeta?.version || 'DIABETES-v1.0'}
                  </span>
                </div>
              </div>

              {diabetesResult && (
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
              )}
            </div>

            {diabetesResult ? (
              <div style={{ marginBottom: '20px' }}>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                  Diabetes Complication Risk Score
                </div>
                <div style={{ fontSize: '2.5rem', fontWeight: 800, color: 'var(--text-primary)', lineHeight: 1 }}>
                  {diabetesResult.risk_percentage != null ? `${diabetesResult.risk_percentage}%` : 'N/A'}
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '8px' }}>
                  <span>Probability: {diabetesResult.risk_probability != null ? diabetesResult.risk_probability.toFixed(4) : 'N/A'}</span>
                  <span>Timestamp: {diabetesTimestamp}</span>
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
                  Click below to generate diabetes risk score for this patient.
                </p>
              </div>
            )}
          </div>

          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <button
              className="btn-primary"
              onClick={handlePredictDiabetes}
              disabled={isPredicting || !selectedPatient}
              style={{ flex: 1 }}
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
                onClick={() => navigate(`/shap?patientId=${selectedPatient.id}&model=diabetes&prob=${diabetesResult.risk_percentage}`)}
                style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
              >
                <BrainCircuit size={16} />
                View Explanation
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
          <strong>Clinical Notice:</strong> AI predictions are decision-support information and are not a substitute for professional clinical judgment. Do not interpret model predictions as definitive diagnoses or use them for automated treatment recommendations.
        </span>
      </div>
    </div>
  );
}
