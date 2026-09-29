import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  ClipboardList, RefreshCw, AlertCircle, Play, CheckCircle2,
  Clock, ShieldAlert, Heart, Activity, User, ChevronRight,
  FileText, ArrowRight, Zap, Check, AlertTriangle, Edit3,
  ShieldCheck, Pill, CheckSquare, XCircle, FileCheck, ThumbsUp
} from 'lucide-react';
import PatientSelector from '../components/PatientSelector';
import {
  getFhirPatients, getPatientTwins,
  generateCarePlan, getPatientCarePlans, updateCarePlanStatus,
  getCarePlanValidation,
  getPatientName, calculateAge, getPatientHealthData, getAllPatientData
} from '../services/api';

const STATUS_CONFIG = {
  DRAFT: { label: 'Draft', color: 'var(--text-muted)', bg: 'var(--bg-primary)', border: 'var(--border-color)' },
  PENDING_APPROVAL: { label: 'Pending Provider Approval', color: '#d97706', bg: '#fef3c7', border: '#fcd34d' },
  APPROVED: { label: 'Approved', color: '#0284c7', bg: '#e0f2fe', border: '#38bdf8' },
  ACTIVE: { label: 'Active', color: '#059669', bg: '#d1fae5', border: '#34d399' },
  COMPLETED: { label: 'Completed', color: '#4f46e5', bg: '#e0e7ff', border: '#818cf8' },
};

export default function CarePlansPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const urlPatientId = searchParams.get('patientId');

  const [patients, setPatients] = useState([]);
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [loadingPatients, setLoadingPatients] = useState(true);

  // Patient health data from patient-data.json
  const [patientHealthData, setPatientHealthData] = useState(null);
  const [loadingHealthData, setLoadingHealthData] = useState(false);

  // Care Plan states
  const [currentCarePlan, setCurrentCarePlan] = useState(null);
  const [carePlanHistory, setCarePlanHistory] = useState([]);
  const [loadingCarePlan, setLoadingCarePlan] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [statusUpdating, setStatusUpdating] = useState(false);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Validation state (Guideline compliance, Safety checks, Drug interaction)
  const [validationData, setValidationData] = useState(null);
  const [loadingValidation, setLoadingValidation] = useState(false);

  // Load patient roster prioritizing rich clinical patients (P001-P010) and FHIR records
  const loadPatients = async () => {
    setLoadingPatients(true);
    try {
      // 1. Always load rich clinical patient records (P001 - P010)
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
            };
          });
        }
      } catch (pdErr) {
        console.warn('Patient-data fetch fallback failed:', pdErr);
      }

      // 2. Also load FHIR patients if available, omitting duplicates
      let fhirListMapped = [];
      try {
        const fhirList = await getFhirPatients();
        if (Array.isArray(fhirList) && fhirList.length > 0) {
          fhirListMapped = fhirList
            .filter(p => !clinicalList.some(c =>
              String(c.id).toLowerCase() === String(p.id).toLowerCase() ||
              String(c.patientId).toLowerCase() === String(p.sourcePatientId || p.id).toLowerCase()
            ))
            .map(p => ({
              id: p.id,
              patientId: p.sourcePatientId || p.id,
              sourcePatientId: p.sourcePatientId || p.id,
              name: getPatientName(p),
              displayName: getPatientName(p),
              gender: p.gender || 'unknown',
              birthDate: p.birthDate || 'N/A',
            }));
        }
      } catch (fhirErr) {
        console.warn('FHIR patient list fetch notice:', fhirErr);
      }

      const combinedList = [...clinicalList, ...fhirListMapped];
      setPatients(combinedList);

      // Determine initial patient: URL param -> localStorage -> first in list
      const savedPatientId = urlPatientId || localStorage.getItem('medisphere_selected_patient_id');
      if (combinedList.length > 0) {
        let initial = combinedList[0];
        if (savedPatientId) {
          const match = combinedList.find(p =>
            String(p.id).toLowerCase() === String(savedPatientId).toLowerCase() ||
            String(p.patientId).toLowerCase() === String(savedPatientId).toLowerCase() ||
            String(p.sourcePatientId).toLowerCase() === String(savedPatientId).toLowerCase()
          );
          if (match) initial = match;
        }
        setSelectedPatient(initial);
        const initialPid = initial.sourcePatientId || initial.patientId || initial.id;
        setSearchParams({ patientId: initialPid }, { replace: true });
        localStorage.setItem('medisphere_selected_patient_id', initialPid);
      }
    } catch (err) {
      console.error('Failed loading patient roster:', err);
      setError('Failed to load patients for care plan management.');
    } finally {
      setLoadingPatients(false);
    }
  };

  useEffect(() => {
    loadPatients();
  }, [urlPatientId]);

  // Handle explicit patient selection with full persistence across refresh
  const handleSelectPatient = (patient) => {
    setSelectedPatient(patient);
    if (patient) {
      const pid = patient.sourcePatientId || patient.patientId || patient.id;
      setSearchParams({ patientId: pid });
      localStorage.setItem('medisphere_selected_patient_id', pid);
    }
  };

  // Load existing care plans whenever selected patient changes
  const fetchPatientCarePlans = async (patientId) => {
    if (!patientId) return;
    setLoadingCarePlan(true);
    setError(null);
    try {
      const plans = await getPatientCarePlans(patientId);
      if (Array.isArray(plans) && plans.length > 0) {
        // Display active/approved plan, or default to the most recent one (index 0)
        const activeOrLatest = plans.find(p => p.status === 'ACTIVE' || p.status === 'APPROVED') || plans[0];
        setCurrentCarePlan(activeOrLatest);
        setCarePlanHistory(plans);
      } else {
        setCurrentCarePlan(null);
        setCarePlanHistory([]);
      }
    } catch (err) {
      console.error('Error fetching care plan:', err);
      setError('Unable to load care plan. Please try again.');
      setCurrentCarePlan(null);
      setCarePlanHistory([]);
    } finally {
      setLoadingCarePlan(false);
    }
  };

  // Fetch validation for current care plan
  const fetchCarePlanValidation = async (carePlanId) => {
    if (!carePlanId) {
      setValidationData(null);
      return;
    }
    setLoadingValidation(true);
    try {
      const val = await getCarePlanValidation(carePlanId);
      setValidationData(val);
    } catch (err) {
      console.warn('Could not load care plan validation:', err.message);
      setValidationData(null);
    } finally {
      setLoadingValidation(false);
    }
  };

  useEffect(() => {
    if (selectedPatient) {
      const patientId = selectedPatient.sourcePatientId || selectedPatient.patientId || selectedPatient.id;
      fetchPatientCarePlans(patientId);
      fetchPatientHealthData(selectedPatient);
    }
  }, [selectedPatient]);

  useEffect(() => {
    if (currentCarePlan?.id) {
      fetchCarePlanValidation(currentCarePlan.id);
    } else {
      setValidationData(null);
    }
  }, [currentCarePlan?.id]);

  // Fetch the rich health record for the selected patient from patient-data.json
  const fetchPatientHealthData = async (patient) => {
    if (!patient) return;
    setLoadingHealthData(true);
    setPatientHealthData(null);
    try {
      // Determine the patientId to look up in patient-data.json (P001-P010)
      const lookupId = patient.sourcePatientId || patient.patientId || patient.id;
      const data = await getPatientHealthData(lookupId);
      setPatientHealthData(data);
    } catch (err) {
      console.warn('Could not load patient health data:', err.message);
      setPatientHealthData(null);
    } finally {
      setLoadingHealthData(false);
    }
  };

  // Helper: extract a vital sign by matching type case-insensitively
  const findVital = (...types) => {
    if (!patientHealthData?.vitals) return null;
    for (const t of types) {
      const match = patientHealthData.vitals.find(v => v.type && v.type.toLowerCase() === t.toLowerCase());
      if (match) return match;
    }
    return null;
  };

  // Helper: extract a lab result by matching test name case-insensitively
  const findLab = (...testNames) => {
    if (!patientHealthData?.labResults) return null;
    for (const name of testNames) {
      const match = patientHealthData.labResults.find(l => l.test && l.test.toLowerCase() === name.toLowerCase());
      if (match) return match;
    }
    return null;
  };

  // Build the health snapshot strictly from actual patient data (no static hardcoding)
  const getPatientHealthSnapshot = () => {
    if (!patientHealthData) {
      return null;
    }

    const bpVital = findVital('Blood Pressure', 'BP');
    const hrVital = findVital('Heart Rate', 'Pulse', 'HR');
    const spo2Vital = findVital('SpO2', 'Oxygen Saturation');
    const tempVital = findVital('Temperature', 'Temp');
    const glucoseLab = findLab('Blood Glucose', 'Glucose', 'Fasting Blood Glucose');
    const hba1cLab = findLab('HbA1c', 'Hemoglobin A1c', 'A1c');
    const cholesterolLab = findLab('Total Cholesterol', 'Cholesterol');
    const ldlLab = findLab('LDL Cholesterol', 'LDL');

    let bpStr = null;
    if (bpVital) {
      if (bpVital.systolic != null && bpVital.diastolic != null) {
        bpStr = `${Math.round(bpVital.systolic)}/${Math.round(bpVital.diastolic)} ${bpVital.unit || 'mmHg'}`;
      } else if (bpVital.value) {
        bpStr = `${bpVital.value} ${bpVital.unit || 'mmHg'}`;
      }
    }

    return {
      bp: bpStr,
      heartRate: hrVital?.value != null ? `${hrVital.value} ${hrVital.unit || 'bpm'}` : null,
      spo2: spo2Vital?.value != null ? `${spo2Vital.value}${spo2Vital.unit || '%'}` : null,
      temperature: tempVital?.value != null ? `${tempVital.value}°${tempVital.unit || 'F'}` : null,
      sugar: glucoseLab?.value != null ? `${glucoseLab.value} ${glucoseLab.unit || 'mg/dL'}` : null,
      hba1c: hba1cLab?.value != null ? `${hba1cLab.value}${hba1cLab.unit || '%'}` : null,
      cholesterol: cholesterolLab?.value != null ? `${cholesterolLab.value} ${cholesterolLab.unit || 'mg/dL'}` : null,
      ldl: ldlLab?.value != null ? `${ldlLab.value} ${ldlLab.unit || 'mg/dL'}` : null,
      steps: patientHealthData.wearableData?.steps ?? null,
      sleepHours: patientHealthData.wearableData?.sleepHours ?? null,
      conditions: Array.isArray(patientHealthData.conditions) ? patientHealthData.conditions : [],
      riskData: patientHealthData.riskProfile || patientHealthData.riskInformation || null,
    };
  };

  // Generate Care Plan
  const handleGenerateCarePlan = async () => {
    if (!selectedPatient || generating) return;
    setGenerating(true);
    setError(null);
    setSuccessMessage(null);

    const snapshot = getPatientHealthSnapshot();
    const pid = selectedPatient.sourcePatientId || selectedPatient.patientId || selectedPatient.id;
    const pName = (patientHealthData?.personalDetails
      ? `${patientHealthData.personalDetails.firstName || ''} ${patientHealthData.personalDetails.lastName || ''}`.trim()
      : null) || selectedPatient.name || selectedPatient.displayName;

    const requestData = {
      patientId: pid,
      patientName: pName,
      riskInformation: {
        bloodPressure: snapshot?.bp || null,
        bloodSugar: snapshot?.sugar || null,
        hba1c: snapshot?.hba1c || null,
        heartRate: snapshot?.heartRate || null,
        spo2: snapshot?.spo2 || null,
        temperature: snapshot?.temperature || null,
        cholesterol: snapshot?.cholesterol || null,
        ldl: snapshot?.ldl || null,
        steps: snapshot?.steps ?? null,
        sleepHours: snapshot?.sleepHours ?? null,
        conditions: snapshot?.conditions?.map(c => c.name).join(', ') || null,
        existingRisk: snapshot?.riskData || null,
      }
    };

    try {
      const generated = await generateCarePlan(requestData);
      setCurrentCarePlan(generated);
      setCarePlanHistory(prev => [generated, ...prev]);
      setSuccessMessage(`Care plan successfully generated for ${pName}.`);
    } catch (err) {
      console.error('Generate care plan error:', err);
      setError(err.message || 'Failed to generate care plan. Please verify backend service.');
    } finally {
      setGenerating(false);
    }
  };

  // Change Care Plan Status
  const handleStatusChange = async (newStatus) => {
    if (!currentCarePlan || !currentCarePlan.id || statusUpdating) return;
    setStatusUpdating(true);
    setError(null);
    try {
      const updated = await updateCarePlanStatus(currentCarePlan.id, newStatus);
      setCurrentCarePlan(updated);
      setCarePlanHistory(prev => prev.map(p => p.id === updated.id ? updated : p));
      setSuccessMessage(`Care plan status updated to: ${newStatus}`);
    } catch (err) {
      console.error('Status change error:', err);
      setError(err.message || 'Failed to update care plan status.');
    } finally {
      setStatusUpdating(false);
    }
  };

  const healthSnapshot = getPatientHealthSnapshot();
  const currentStatus = currentCarePlan?.status || 'PENDING_APPROVAL';
  const statusInfo = STATUS_CONFIG[currentStatus] || STATUS_CONFIG.PENDING_APPROVAL;

  return (
    <div className="dashboard-content">
      {/* Page Header */}
      <div className="page-header-row">
        <div className="page-title-group">
          <h1>Precision Care Management</h1>
          <p>AI-Generated Personalized Care Plans & Clinical Pathway Tracking</p>
        </div>
      </div>

      {/* Patient Selector */}
      <PatientSelector
        patients={patients}
        selectedPatient={selectedPatient}
        onSelectPatient={handleSelectPatient}
        loading={loadingPatients}
      />

      {/* Error & Success Messages */}
      {error && (
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
          <span style={{ fontSize: '0.9rem', fontWeight: 500 }}>{error}</span>
        </div>
      )}

      {successMessage && (
        <div style={{
          backgroundColor: 'var(--status-success-bg)',
          color: 'var(--status-success-text)',
          padding: '14px 18px',
          borderRadius: 'var(--radius-md)',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          border: '1px solid var(--border-color)'
        }}>
          <CheckCircle2 size={20} style={{ flexShrink: 0 }} />
          <span style={{ fontSize: '0.9rem', fontWeight: 500 }}>{successMessage}</span>
        </div>
      )}

      {/* Patient Health Information Card */}
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
                <h3 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  Patient: {patientHealthData?.personalDetails
                    ? `${patientHealthData.personalDetails.firstName || ''} ${patientHealthData.personalDetails.lastName || ''}`.trim()
                    : selectedPatient.name}
                </h3>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  Patient ID: <code style={{ fontFamily: 'monospace' }}>{selectedPatient.sourcePatientId || selectedPatient.patientId || selectedPatient.id}</code>
                </span>
              </div>
            </div>

            <button
              className="btn-primary"
              onClick={handleGenerateCarePlan}
              disabled={generating || loadingHealthData}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '10px 20px',
                fontSize: '0.95rem',
                fontWeight: 600
              }}
            >
              {generating ? (
                <>
                  <RefreshCw size={18} className="spin-icon" />
                  Generating Care Plan...
                </>
              ) : (
                <>
                  <Zap size={18} />
                  Generate Care Plan
                </>
              )}
            </button>
          </div>

          {/* Conditions */}
          <div style={{ marginBottom: '14px' }}>
            <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '6px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Active Conditions
            </div>
            {healthSnapshot?.conditions && healthSnapshot.conditions.length > 0 ? (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                {healthSnapshot.conditions.map((c, idx) => (
                  <span key={idx} style={{
                    background: '#fef3c7',
                    color: '#92400e',
                    padding: '4px 12px',
                    borderRadius: '999px',
                    fontSize: '0.82rem',
                    fontWeight: 600,
                    border: '1px solid #fcd34d'
                  }}>
                    {c.name || c}
                  </span>
                ))}
              </div>
            ) : (
              <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>No active conditions diagnosed</span>
            )}
          </div>

          <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '10px' }}>
            Health Information & Vitals:
          </div>

          {loadingHealthData ? (
            <div style={{ padding: '16px', textAlign: 'center', color: 'var(--text-muted)' }}>
              <RefreshCw size={18} className="spin-icon" style={{ marginRight: '8px' }} />
              Loading patient health data...
            </div>
          ) : (
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(155px, 1fr))',
              gap: '12px'
            }}>
              {/* Blood Pressure */}
              <div style={{ padding: '12px 16px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Blood Pressure</div>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '4px' }}>
                  {healthSnapshot?.bp || <span style={{ color: 'var(--text-muted)', fontWeight: 400, fontSize: '0.9rem' }}>Not available</span>}
                </div>
              </div>

              {/* Heart Rate */}
              <div style={{ padding: '12px 16px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Heart Rate</div>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '4px' }}>
                  {healthSnapshot?.heartRate || <span style={{ color: 'var(--text-muted)', fontWeight: 400, fontSize: '0.9rem' }}>Not available</span>}
                </div>
              </div>

              {/* SpO2 */}
              <div style={{ padding: '12px 16px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>SpO2</div>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '4px' }}>
                  {healthSnapshot?.spo2 || <span style={{ color: 'var(--text-muted)', fontWeight: 400, fontSize: '0.9rem' }}>Not available</span>}
                </div>
              </div>

              {/* Temperature */}
              <div style={{ padding: '12px 16px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Temperature</div>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '4px' }}>
                  {healthSnapshot?.temperature || <span style={{ color: 'var(--text-muted)', fontWeight: 400, fontSize: '0.9rem' }}>Not available</span>}
                </div>
              </div>

              {/* Blood Glucose */}
              <div style={{ padding: '12px 16px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Blood Glucose</div>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '4px' }}>
                  {healthSnapshot?.sugar || <span style={{ color: 'var(--text-muted)', fontWeight: 400, fontSize: '0.9rem' }}>Not available</span>}
                </div>
              </div>

              {/* HbA1c */}
              <div style={{ padding: '12px 16px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>HbA1c</div>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '4px' }}>
                  {healthSnapshot?.hba1c || <span style={{ color: 'var(--text-muted)', fontWeight: 400, fontSize: '0.9rem' }}>Not available</span>}
                </div>
              </div>

              {/* Total Cholesterol */}
              <div style={{ padding: '12px 16px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Cholesterol</div>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '4px' }}>
                  {healthSnapshot?.cholesterol || <span style={{ color: 'var(--text-muted)', fontWeight: 400, fontSize: '0.9rem' }}>Not available</span>}
                </div>
              </div>

              {/* LDL */}
              <div style={{ padding: '12px 16px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>LDL</div>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '4px' }}>
                  {healthSnapshot?.ldl || <span style={{ color: 'var(--text-muted)', fontWeight: 400, fontSize: '0.9rem' }}>Not available</span>}
                </div>
              </div>

              {/* Wearable Steps */}
              <div style={{ padding: '12px 16px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Steps</div>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '4px' }}>
                  {healthSnapshot?.steps != null ? healthSnapshot.steps.toLocaleString() : <span style={{ color: 'var(--text-muted)', fontWeight: 400, fontSize: '0.9rem' }}>Not available</span>}
                </div>
              </div>

              {/* Sleep Hours */}
              <div style={{ padding: '12px 16px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Sleep</div>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '4px' }}>
                  {healthSnapshot?.sleepHours != null ? `${healthSnapshot.sleepHours} hrs` : <span style={{ color: 'var(--text-muted)', fontWeight: 400, fontSize: '0.9rem' }}>Not available</span>}
                </div>
              </div>

              {/* Existing Risk Data */}
              <div style={{ padding: '12px 16px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Existing Risk</div>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '4px' }}>
                  {healthSnapshot?.riskData || <span style={{ color: 'var(--text-muted)', fontWeight: 400, fontSize: '0.9rem' }}>Not available</span>}
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Main Care Plan Display Section */}
      {loadingCarePlan ? (
        <div style={{
          padding: '40px',
          textAlign: 'center',
          background: 'var(--bg-surface)',
          borderRadius: 'var(--radius-lg)',
          border: '1px solid var(--border-color)'
        }}>
          <RefreshCw size={28} className="spin-icon" style={{ margin: '0 auto 12px', color: 'var(--accent-primary)' }} />
          <p style={{ margin: 0, color: 'var(--text-secondary)' }}>Loading care plan...</p>
        </div>
      ) : !currentCarePlan ? (
        <div style={{
          padding: '50px 24px',
          textAlign: 'center',
          background: 'var(--bg-surface)',
          borderRadius: 'var(--radius-lg)',
          border: '1px solid var(--border-color)'
        }}>
          <ClipboardList size={48} style={{ opacity: 0.3, margin: '0 auto 12px', color: 'var(--accent-primary)' }} />
          <h3 style={{ margin: '0 0 8px', color: 'var(--text-primary)' }}>No care plan generated yet.</h3>
          <p style={{ margin: '0 0 20px', color: 'var(--text-muted)', maxWidth: '480px', marginInline: 'auto' }}>
            No care plan has been generated for this patient yet. Click the <strong>[Generate Care Plan]</strong> button above to construct a personalized, AI-driven care plan based on patient health indicators and risk models.
          </p>
          <button className="btn-primary" onClick={handleGenerateCarePlan} disabled={generating}>
            <Zap size={16} />
            Generate Care Plan Now
          </button>
        </div>
      ) : (
        <div style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-md)',
          overflow: 'hidden',
          marginBottom: '24px'
        }}>
          {/* Care Plan Header */}
          <div style={{
            padding: '24px',
            background: 'linear-gradient(135deg, var(--bg-surface) 0%, var(--bg-primary) 100%)',
            borderBottom: '1px solid var(--border-color)'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px' }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
                  <span style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                    Patient: {currentCarePlan.patientName || selectedPatient?.name}
                  </span>
                  <span style={{
                    background: 'var(--accent-light)',
                    color: 'var(--accent-primary)',
                    padding: '4px 10px',
                    borderRadius: '999px',
                    fontSize: '0.8rem',
                    fontWeight: 700
                  }}>
                    Care Plan: AI-Generated
                  </span>
                </div>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                  Created: {currentCarePlan.createdAt ? new Date(currentCarePlan.createdAt).toLocaleString() : 'Just now'} | Plan ID: {currentCarePlan.id || 'N/A'}
                </div>
              </div>

              {/* Status Badge & Controls */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <span style={{
                  backgroundColor: statusInfo.bg,
                  color: statusInfo.color,
                  border: `1px solid ${statusInfo.border}`,
                  padding: '6px 14px',
                  borderRadius: '999px',
                  fontSize: '0.85rem',
                  fontWeight: 700,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px'
                }}>
                  <Clock size={14} />
                  Status: {statusInfo.label}
                </span>

                <select
                  value={currentStatus}
                  onChange={(e) => handleStatusChange(e.target.value)}
                  disabled={statusUpdating}
                  style={{
                    padding: '6px 12px',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--border-color)',
                    background: 'var(--bg-surface)',
                    color: 'var(--text-primary)',
                    fontSize: '0.85rem',
                    fontWeight: 600,
                    cursor: 'pointer'
                  }}
                >
                  <option value="DRAFT">Set: Draft</option>
                  <option value="PENDING_APPROVAL">Set: Pending Approval</option>
                  <option value="APPROVED">Set: Approved</option>
                  <option value="ACTIVE">Set: Active</option>
                  <option value="COMPLETED">Set: Completed</option>
                </select>
              </div>
            </div>
          </div>

          {/* Student/Demo Disclaimer Notice */}
          <div style={{
            margin: '20px 24px 0',
            padding: '14px 18px',
            background: 'var(--bg-primary)',
            borderRadius: 'var(--radius-md)',
            border: '1px dashed var(--accent-primary)',
            fontSize: '0.82rem',
            color: 'var(--text-secondary)',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '12px'
          }}>
            <ShieldAlert size={20} style={{ flexShrink: 0, color: 'var(--accent-primary)', marginTop: '2px' }} />
            <div>
              <strong style={{ color: 'var(--text-primary)' }}>Student Project / Demo Disclaimer:</strong>
              <p style={{ margin: '4px 0 0', lineHeight: 1.4 }}>
                {currentCarePlan.disclaimer || 'AI-generated recommendations are provider-reviewed/demo recommendations. Medication changes must not be autonomously prescribed by the system.'}
              </p>
            </div>
          </div>

          <div style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '24px' }}>
            {/* Clinical Validation, Safety Checks & Provider Verification Section */}
            <div style={{
              background: 'var(--bg-surface)',
              border: '1px solid var(--border-color)',
              borderRadius: 'var(--radius-lg)',
              padding: '20px 22px',
              boxShadow: 'var(--shadow-sm)'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px', flexWrap: 'wrap', gap: '10px' }}>
                <h4 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 800, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <ShieldCheck size={20} style={{ color: 'var(--accent-primary)' }} />
                  Care Plan Clinical Validation & Provider Verification
                </h4>
                {loadingValidation && (
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <RefreshCw size={13} className="spin-icon" /> Validating rules...
                  </span>
                )}
              </div>

              {/* 3 Validation Cards Grid */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
                gap: '14px',
                marginBottom: '16px'
              }}>
                {/* 1. Guideline Compliance Card */}
                <div style={{
                  background: 'var(--bg-primary)',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-md)',
                  padding: '14px 16px',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between'
                }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                      <span style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                        1. Guideline Compliance
                      </span>
                      <span style={{
                        background: validationData?.guidelineCompliance?.status === 'COMPLIANT' ? '#d1fae5' : '#fef3c7',
                        color: validationData?.guidelineCompliance?.status === 'COMPLIANT' ? '#059669' : '#d97706',
                        border: `1px solid ${validationData?.guidelineCompliance?.status === 'COMPLIANT' ? '#34d399' : '#fcd34d'}`,
                        padding: '2px 8px',
                        borderRadius: '999px',
                        fontSize: '0.72rem',
                        fontWeight: 700
                      }}>
                        {validationData?.guidelineCompliance?.displayStatus || 'Compliant'}
                      </span>
                    </div>
                    <p style={{ margin: '0 0 8px', fontSize: '0.85rem', color: 'var(--text-primary)', lineHeight: 1.4, fontWeight: 500 }}>
                      {validationData?.guidelineCompliance?.summary || 'Care plan evaluated against configured clinical guideline rules.'}
                    </p>
                    {validationData?.guidelineCompliance?.guidelinesChecked?.length > 0 && (
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginBottom: '6px' }}>
                        <strong>Checked:</strong> {validationData.guidelineCompliance.guidelinesChecked.join(', ')}
                      </div>
                    )}
                  </div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', borderTop: '1px solid var(--border-color)', paddingTop: '6px', marginTop: '6px' }}>
                    Configured demonstration check. Not an official medical certification.
                  </div>
                </div>

                {/* 2. Safety Checks Card */}
                <div style={{
                  background: 'var(--bg-primary)',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-md)',
                  padding: '14px 16px',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between'
                }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                      <span style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                        2. Safety Validation
                      </span>
                      <span style={{
                        background: validationData?.safetyCheck?.passed ? '#d1fae5' : '#fee2e2',
                        color: validationData?.safetyCheck?.passed ? '#059669' : '#dc2626',
                        border: `1px solid ${validationData?.safetyCheck?.passed ? '#34d399' : '#fca5a5'}`,
                        padding: '2px 8px',
                        borderRadius: '999px',
                        fontSize: '0.72rem',
                        fontWeight: 700
                      }}>
                        {validationData?.safetyCheck?.displayStatus || (validationData?.safetyCheck?.passed ? 'Safety Check Passed' : 'Safety Check Passed')}
                      </span>
                    </div>
                    <p style={{ margin: '0 0 6px', fontSize: '0.85rem', color: 'var(--text-primary)', lineHeight: 1.4, fontWeight: 500 }}>
                      {validationData?.safetyCheck?.summary || 'Pre-approval safety checks evaluated.'}
                    </p>
                    {validationData?.safetyCheck?.checksPassed?.length > 0 && (
                      <ul style={{ margin: '0 0 4px', paddingLeft: '14px', fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                        {validationData.safetyCheck.checksPassed.slice(0, 3).map((cp, idx) => (
                          <li key={idx}>{cp}</li>
                        ))}
                      </ul>
                    )}
                  </div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', borderTop: '1px solid var(--border-color)', paddingTop: '6px', marginTop: '6px' }}>
                    Verified before provider authorization.
                  </div>
                </div>

                {/* 3. Drug Interaction Card */}
                <div style={{
                  background: 'var(--bg-primary)',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-md)',
                  padding: '14px 16px',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between'
                }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                      <span style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                        3. Drug Interaction Check
                      </span>
                      <span style={{
                        background: validationData?.drugInteraction?.hasInteraction ? '#fee2e2' : '#d1fae5',
                        color: validationData?.drugInteraction?.hasInteraction ? '#dc2626' : '#059669',
                        border: `1px solid ${validationData?.drugInteraction?.hasInteraction ? '#fca5a5' : '#34d399'}`,
                        padding: '2px 8px',
                        borderRadius: '999px',
                        fontSize: '0.72rem',
                        fontWeight: 700
                      }}>
                        {validationData?.drugInteraction?.displayStatus || 'No configured interaction detected'}
                      </span>
                    </div>
                    <p style={{ margin: '0 0 6px', fontSize: '0.85rem', color: 'var(--text-primary)', lineHeight: 1.4, fontWeight: 500 }}>
                      {validationData?.drugInteraction?.summary || 'No configured interaction detected.'}
                    </p>
                    {validationData?.drugInteraction?.detectedInteractions?.map((di, idx) => (
                      <div key={idx} style={{
                        background: '#fee2e2',
                        color: '#991b1b',
                        border: '1px solid #fca5a5',
                        padding: '6px 8px',
                        borderRadius: '6px',
                        fontSize: '0.75rem',
                        marginBottom: '4px'
                      }}>
                        <strong>{di.drugA} + {di.drugB} ({di.severity}):</strong> {di.description}
                      </div>
                    ))}
                  </div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', borderTop: '1px solid var(--border-color)', paddingTop: '6px', marginTop: '6px' }}>
                    Configured demonstration check. Review before approval.
                  </div>
                </div>
              </div>

              {/* Provider Approval Lifecycle Action Bar */}
              <div style={{
                background: 'var(--bg-primary)',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-md)',
                padding: '14px 18px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: '12px'
              }}>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                    Provider Approval Workflow
                  </div>
                  <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '2px' }}>
                    Current Status: <span style={{ color: statusInfo.color }}>{statusInfo.label}</span>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                  {currentStatus !== 'APPROVED' && (
                    <button
                      className="btn-primary"
                      onClick={() => handleStatusChange('APPROVED')}
                      disabled={statusUpdating}
                      style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '7px 14px', fontSize: '0.85rem', background: '#0284c7', borderColor: '#0284c7' }}
                    >
                      <ThumbsUp size={14} />
                      Approve Care Plan
                    </button>
                  )}

                  {currentStatus !== 'ACTIVE' && (
                    <button
                      className="btn-primary"
                      onClick={() => handleStatusChange('ACTIVE')}
                      disabled={statusUpdating}
                      style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '7px 14px', fontSize: '0.85rem', background: '#059669', borderColor: '#059669' }}
                    >
                      <Play size={14} />
                      Activate Plan
                    </button>
                  )}

                  {currentStatus !== 'PENDING_APPROVAL' && currentStatus !== 'DRAFT' && (
                    <button
                      className="btn-secondary"
                      onClick={() => handleStatusChange('PENDING_APPROVAL')}
                      disabled={statusUpdating}
                      style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '7px 12px', fontSize: '0.85rem' }}
                    >
                      <Edit3 size={14} />
                      Request Revision
                    </button>
                  )}

                  {currentStatus !== 'COMPLETED' && (
                    <button
                      className="btn-secondary"
                      onClick={() => handleStatusChange('COMPLETED')}
                      disabled={statusUpdating}
                      style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '7px 12px', fontSize: '0.85rem' }}
                    >
                      <CheckCircle2 size={14} />
                      Mark Completed
                    </button>
                  )}
                </div>
              </div>
            </div>

            {/* Care Goals Section */}
            <div>
              <h4 style={{ margin: '0 0 14px', fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <CheckCircle2 size={18} style={{ color: 'var(--accent-primary)' }} />
                Care Goals
              </h4>

              {currentCarePlan.goals && currentCarePlan.goals.length > 0 ? (
                <div style={{ display: 'grid', gap: '12px' }}>
                  {currentCarePlan.goals.map((goal, idx) => (
                    <div key={idx} style={{
                      background: 'var(--bg-primary)',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      padding: '16px 20px'
                    }}>
                      <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--accent-primary)', marginBottom: '4px' }}>
                        Goal {idx + 1}: {goal.title}
                      </div>
                      {goal.description && (
                        <p style={{ margin: '0 0 8px', fontSize: '0.88rem', color: 'var(--text-secondary)' }}>
                          {goal.description}
                        </p>
                      )}
                      {goal.targetMetric && (
                        <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                          <strong>Target Metric:</strong> {goal.targetMetric}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <div style={{
                  background: 'var(--bg-primary)',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-md)',
                  padding: '16px 20px'
                }}>
                  <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--accent-primary)', marginBottom: '4px' }}>
                    Goal 1: Improve BP and blood sugar control
                  </div>
                  <p style={{ margin: 0, fontSize: '0.88rem', color: 'var(--text-secondary)' }}>
                    Reduce blood pressure toward target and stabilize glycemic control.
                  </p>
                </div>
              )}
            </div>

            {/* Interventions Section */}
            <div>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px', flexWrap: 'wrap', gap: '8px' }}>
                <h4 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Activity size={18} style={{ color: 'var(--accent-primary)' }} />
                  Recommended Interventions ({currentCarePlan.interventions?.length || 0})
                </h4>

                <button
                  className="btn-primary"
                  onClick={() => {
                    const pid = selectedPatient?.sourcePatientId || selectedPatient?.patientId || selectedPatient?.id;
                    navigate(`/treatment-tracking?patientId=${encodeURIComponent(pid)}`);
                  }}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    padding: '6px 14px',
                    fontSize: '0.85rem'
                  }}
                >
                  Track Treatments & Adherence
                  <ArrowRight size={14} />
                </button>
              </div>

              <div style={{
                background: 'var(--bg-primary)',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-md)',
                padding: '18px 22px'
              }}>
                <ul style={{ margin: 0, paddingLeft: '20px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {currentCarePlan.interventions && currentCarePlan.interventions.length > 0 ? (
                    currentCarePlan.interventions.map((item, idx) => (
                      <li key={idx} style={{ fontSize: '0.92rem', color: 'var(--text-primary)', lineHeight: 1.5 }}>
                        {item}
                      </li>
                    ))
                  ) : (
                    <>
                      <li style={{ fontSize: '0.92rem', color: 'var(--text-primary)' }}>Take prescribed treatment as directed</li>
                      <li style={{ fontSize: '0.92rem', color: 'var(--text-primary)' }}>Monitor BP</li>
                      <li style={{ fontSize: '0.92rem', color: 'var(--text-primary)' }}>Monitor blood sugar</li>
                      <li style={{ fontSize: '0.92rem', color: 'var(--text-primary)' }}>Exercise regularly</li>
                      <li style={{ fontSize: '0.92rem', color: 'var(--text-primary)' }}>Follow a healthy diet</li>
                    </>
                  )}
                </ul>
              </div>
            </div>

            {/* Monitoring Instructions Section */}
            <div>
              <h4 style={{ margin: '0 0 12px', fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Clock size={18} style={{ color: 'var(--accent-primary)' }} />
                Monitoring Instructions
              </h4>

              <div style={{
                background: 'var(--bg-primary)',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-md)',
                padding: '18px 22px'
              }}>
                <ul style={{ margin: 0, paddingLeft: '20px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {currentCarePlan.monitoringInstructions && currentCarePlan.monitoringInstructions.length > 0 ? (
                    currentCarePlan.monitoringInstructions.map((item, idx) => (
                      <li key={idx} style={{ fontSize: '0.92rem', color: 'var(--text-primary)', lineHeight: 1.5 }}>
                        {item}
                      </li>
                    ))
                  ) : (
                    <>
                      <li style={{ fontSize: '0.92rem', color: 'var(--text-primary)' }}>Regular BP readings</li>
                      <li style={{ fontSize: '0.92rem', color: 'var(--text-primary)' }}>Regular glucose readings</li>
                      <li style={{ fontSize: '0.92rem', color: 'var(--text-primary)' }}>Follow-up review</li>
                    </>
                  )}
                </ul>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Care Plan History Drawer */}
      {carePlanHistory.length > 1 && (
        <div style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-lg)',
          padding: '20px 24px'
        }}>
          <h4 style={{ margin: '0 0 14px', fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
            Care Plan History for {selectedPatient?.name} ({carePlanHistory.length})
          </h4>
          <div style={{ display: 'grid', gap: '10px' }}>
            {carePlanHistory.map((plan, idx) => (
              <div
                key={plan.id || idx}
                onClick={() => setCurrentCarePlan(plan)}
                style={{
                  padding: '12px 16px',
                  background: currentCarePlan?.id === plan.id ? 'var(--accent-light)' : 'var(--bg-primary)',
                  border: `1px solid ${currentCarePlan?.id === plan.id ? 'var(--accent-primary)' : 'var(--border-color)'}`,
                  borderRadius: 'var(--radius-md)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  cursor: 'pointer'
                }}
              >
                <div>
                  <span style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                    Care Plan #{plan.id ? plan.id.substring(0, 8) : idx + 1}
                  </span>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginLeft: '12px' }}>
                    {plan.createdAt ? new Date(plan.createdAt).toLocaleString() : ''}
                  </span>
                </div>
                <span style={{
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  padding: '3px 8px',
                  borderRadius: '999px',
                  background: (STATUS_CONFIG[plan.status] || STATUS_CONFIG.PENDING_APPROVAL).bg,
                  color: (STATUS_CONFIG[plan.status] || STATUS_CONFIG.PENDING_APPROVAL).color
                }}>
                  {plan.status}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
