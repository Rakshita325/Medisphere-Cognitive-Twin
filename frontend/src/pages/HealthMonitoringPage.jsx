import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  HeartPulse, Plus, RefreshCw, AlertCircle, CheckCircle2,
  Trash2, Clock, User, FileText, Stethoscope, Calendar
} from 'lucide-react';
import PatientSelector from '../components/PatientSelector';
import {
  getFhirPatients, getAllPatientData, getPatientName,
  getPatientHealthData, createHealthMeasurement,
  getPatientHealthMeasurements, deleteHealthMeasurement
} from '../services/api';

// ─── Measurement Type Definitions ───
const MEASUREMENT_TYPES = [
  { type: 'Blood Glucose',   unit: 'mg/dL',   isBP: false },
  { type: 'HbA1c',           unit: '%',        isBP: false },
  { type: 'Blood Pressure',  unit: 'mmHg',     isBP: true  },
  { type: 'Heart Rate',      unit: 'BPM',      isBP: false },
  { type: 'SpO2',            unit: '%',        isBP: false },
  { type: 'Weight',          unit: 'kg',       isBP: false },
  { type: 'Cholesterol',     unit: 'mg/dL',    isBP: false },
  { type: 'Hemoglobin',      unit: 'g/dL',     isBP: false },
  { type: 'Creatinine',      unit: 'mg/dL',    isBP: false },
  { type: 'TSH',             unit: 'mIU/L',    isBP: false },
  { type: 'Temperature',     unit: '°C',       isBP: false },
];

const TYPE_COLORS = {
  'Blood Glucose':  { color: '#f59e0b', bg: '#fef3c7' },
  'HbA1c':          { color: '#8b5cf6', bg: '#ede9fe' },
  'Blood Pressure': { color: '#ef4444', bg: '#fee2e2' },
  'Heart Rate':     { color: '#ec4899', bg: '#fce7f3' },
  'SpO2':           { color: '#0ea5e9', bg: '#e0f2fe' },
  'Weight':         { color: '#10b981', bg: '#d1fae5' },
  'Cholesterol':    { color: '#f97316', bg: '#fff7ed' },
  'Hemoglobin':     { color: '#dc2626', bg: '#fee2e2' },
  'Creatinine':     { color: '#6366f1', bg: '#e0e7ff' },
  'TSH':            { color: '#14b8a6', bg: '#ccfbf1' },
  'Temperature':    { color: '#d97706', bg: '#fef3c7' },
};

function getDefaultDateTime() {
  const now = new Date();
  const offset = now.getTimezoneOffset();
  const local = new Date(now.getTime() - offset * 60 * 1000);
  return local.toISOString().slice(0, 16);
}

function formatDateTime(timestamp) {
  if (!timestamp) return '—';
  try {
    const d = new Date(timestamp);
    if (isNaN(d.getTime())) return '—';
    return d.toLocaleString([], {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  } catch {
    return '—';
  }
}

function formatDate(timestamp) {
  if (!timestamp) return '—';
  try {
    const d = new Date(timestamp);
    if (isNaN(d.getTime())) return '—';
    return d.toLocaleDateString([], {
      day: 'numeric', month: 'short', year: 'numeric'
    });
  } catch {
    return '—';
  }
}

// ─── Main Component ───
export default function HealthMonitoringPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const urlPatientId = searchParams.get('patientId');

  // Patient state
  const [patients, setPatients] = useState([]);
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [loadingPatients, setLoadingPatients] = useState(true);
  const [patientHealthData, setPatientHealthData] = useState(null);

  // Measurement records state
  const [measurements, setMeasurements] = useState([]);
  const [loadingMeasurements, setLoadingMeasurements] = useState(false);

  // Form state
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Form fields
  const [formType, setFormType] = useState('');
  const [formValue, setFormValue] = useState('');
  const [formSystolic, setFormSystolic] = useState('');
  const [formDiastolic, setFormDiastolic] = useState('');
  const [formDateTime, setFormDateTime] = useState(getDefaultDateTime());
  const [formNotes, setFormNotes] = useState('');

  // Filter state
  const [filterType, setFilterType] = useState('ALL');

  // Derive current type config
  const currentTypeConfig = MEASUREMENT_TYPES.find(t => t.type === formType);
  const isBP = currentTypeConfig?.isBP || false;
  const currentUnit = currentTypeConfig?.unit || '';

  // ─── Load patient roster (same pattern as CarePlansPage / TreatmentTrackingPage) ───
  const loadPatients = async () => {
    setLoadingPatients(true);
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
            };
          });
        }
      } catch (pdErr) {
        console.warn('Patient-data fetch fallback failed:', pdErr);
      }

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
        console.warn('FHIR fetch notice:', fhirErr);
      }

      const combinedList = [...clinicalList, ...fhirListMapped];
      setPatients(combinedList);

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
      setError('Failed to load patient roster.');
    } finally {
      setLoadingPatients(false);
    }
  };

  useEffect(() => {
    loadPatients();
  }, [urlPatientId]);

  // ─── Patient selection handler ───
  const handleSelectPatient = (patient) => {
    setSelectedPatient(patient);
    setShowForm(false);
    setError(null);
    setSuccessMessage(null);
    if (patient) {
      const pid = patient.sourcePatientId || patient.patientId || patient.id;
      setSearchParams({ patientId: pid });
      localStorage.setItem('medisphere_selected_patient_id', pid);
    }
  };

  // ─── Fetch patient-specific measurements ───
  const fetchMeasurements = async (patientId) => {
    if (!patientId) return;
    setLoadingMeasurements(true);
    try {
      const data = await getPatientHealthMeasurements(patientId);
      setMeasurements(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Error loading health measurements:', err);
      setMeasurements([]);
    } finally {
      setLoadingMeasurements(false);
    }
  };

  // ─── Fetch patient health data (for display name) ───
  const fetchPatientDetails = async (patientId) => {
    try {
      const pd = await getPatientHealthData(patientId);
      setPatientHealthData(pd);
    } catch {
      setPatientHealthData(null);
    }
  };

  // ─── When selected patient changes, load their data ───
  useEffect(() => {
    if (selectedPatient) {
      const pid = selectedPatient.sourcePatientId || selectedPatient.patientId || selectedPatient.id;
      fetchMeasurements(pid);
      fetchPatientDetails(pid);
      setFilterType('ALL');
    } else {
      setMeasurements([]);
      setPatientHealthData(null);
    }
  }, [selectedPatient]);

  // ─── Patient display info ───
  const patientIdVal = selectedPatient
    ? (selectedPatient.sourcePatientId || selectedPatient.patientId || selectedPatient.id)
    : '';
  const patientDisplayName = patientHealthData?.personalDetails
    ? `${patientHealthData.personalDetails.firstName || ''} ${patientHealthData.personalDetails.lastName || ''}`.trim()
    : (selectedPatient?.name || selectedPatient?.displayName || patientIdVal);

  // ─── Reset form ───
  const resetForm = () => {
    setFormType('');
    setFormValue('');
    setFormSystolic('');
    setFormDiastolic('');
    setFormDateTime(getDefaultDateTime());
    setFormNotes('');
  };

  // ─── Save measurement ───
  const handleSave = async () => {
    setError(null);
    setSuccessMessage(null);

    if (!formType) {
      setError('Please select a measurement type.');
      return;
    }

    const typeConfig = MEASUREMENT_TYPES.find(t => t.type === formType);

    if (typeConfig?.isBP) {
      if (!formSystolic || !formDiastolic) {
        setError('Please enter both systolic and diastolic values for Blood Pressure.');
        return;
      }
      const sys = parseFloat(formSystolic);
      const dia = parseFloat(formDiastolic);
      if (isNaN(sys) || isNaN(dia) || sys <= 0 || dia <= 0) {
        setError('Systolic and diastolic must be valid positive numbers.');
        return;
      }
    } else {
      if (!formValue) {
        setError('Please enter a measurement value.');
        return;
      }
      const num = parseFloat(formValue);
      if (isNaN(num)) {
        setError('Value must be a valid number.');
        return;
      }
    }

    if (!formDateTime) {
      setError('Please select a date and time.');
      return;
    }

    setSaving(true);
    try {
      const payload = {
        patientId: patientIdVal,
        measurementType: formType,
        unit: typeConfig?.unit || '',
        recordedAt: new Date(formDateTime).toISOString(),
        source: 'MANUAL',
        notes: formNotes.trim() || null,
      };

      if (typeConfig?.isBP) {
        payload.systolic = parseFloat(formSystolic);
        payload.diastolic = parseFloat(formDiastolic);
      } else {
        payload.value = parseFloat(formValue);
      }

      await createHealthMeasurement(payload);
      setSuccessMessage(`${formType} measurement saved successfully.`);
      resetForm();
      setShowForm(false);

      // Refresh measurements from backend
      await fetchMeasurements(patientIdVal);
    } catch (err) {
      setError(err.message || 'Failed to save measurement. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  // ─── Delete measurement ───
  const handleDelete = async (id) => {
    setDeletingId(id);
    try {
      await deleteHealthMeasurement(id);
      setMeasurements(prev => prev.filter(m => m.id !== id));
      setSuccessMessage('Measurement deleted.');
    } catch (err) {
      setError(err.message || 'Failed to delete measurement.');
    } finally {
      setDeletingId(null);
    }
  };

  // ─── Refresh data ───
  const refreshData = () => {
    setError(null);
    setSuccessMessage(null);
    if (patientIdVal) {
      fetchMeasurements(patientIdVal);
    }
  };

  // ─── Auto-clear messages ───
  useEffect(() => {
    if (successMessage) {
      const t = setTimeout(() => setSuccessMessage(null), 4000);
      return () => clearTimeout(t);
    }
  }, [successMessage]);

  useEffect(() => {
    if (error) {
      const t = setTimeout(() => setError(null), 6000);
      return () => clearTimeout(t);
    }
  }, [error]);

  // ─── Filter measurements ───
  const filteredMeasurements = filterType === 'ALL'
    ? measurements
    : measurements.filter(m => m.measurementType === filterType);

  // ─── Unique types in recorded measurements (for filter dropdown) ───
  const recordedTypes = [...new Set(measurements.map(m => m.measurementType))].sort();

  // ─── Render ───
  return (
    <div className="dashboard-content">
      {/* Page Header */}
      <div className="page-header-row" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px', marginBottom: '20px' }}>
        <div className="page-title-group">
          <h1 style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <HeartPulse className="text-accent" size={28} />
            Health Monitoring
          </h1>
          <p>Milestone 4 Part 3: Record &amp; Track Patient Health Measurements</p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <button
            className="btn-secondary"
            onClick={refreshData}
            disabled={loadingMeasurements}
            title="Refresh Measurements"
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <RefreshCw size={16} className={loadingMeasurements ? 'spin-icon' : ''} />
            Refresh
          </button>
        </div>
      </div>

      {/* Patient Selector */}
      <PatientSelector
        patients={patients}
        selectedPatient={selectedPatient}
        onSelectPatient={handleSelectPatient}
        loading={loadingPatients}
      />

      {/* Error Banner */}
      {error && (
        <div style={{
          backgroundColor: 'var(--status-warning-bg, #fef3c7)',
          color: 'var(--status-warning-text, #92400e)',
          padding: '14px 18px',
          borderRadius: 'var(--radius-md, 8px)',
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

      {/* Success Banner */}
      {successMessage && (
        <div style={{
          backgroundColor: 'var(--status-success-bg, #d1fae5)',
          color: 'var(--status-success-text, #065f46)',
          padding: '14px 18px',
          borderRadius: 'var(--radius-md, 8px)',
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

      {/* Patient Info + Add Measurement */}
      {selectedPatient && (
        <>
          {/* Patient Info Card */}
          <div style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg, 12px)',
            padding: '20px 24px',
            marginBottom: '24px',
            boxShadow: 'var(--shadow-sm)'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                <div style={{
                  background: 'var(--accent-light, #e0e7ff)',
                  color: 'var(--accent-primary, #4f46e5)',
                  padding: '12px',
                  borderRadius: '50%',
                  display: 'flex'
                }}>
                  <Stethoscope size={24} />
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                    {patientDisplayName}
                  </h3>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                    Patient ID: <code style={{ fontFamily: 'monospace', fontWeight: 600 }}>{patientIdVal}</code>
                  </span>
                </div>
              </div>

              <button
                className="btn-primary"
                onClick={() => {
                  resetForm();
                  setShowForm(!showForm);
                  setError(null);
                  setSuccessMessage(null);
                }}
                style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
              >
                <Plus size={16} />
                {showForm ? 'Cancel' : 'Add New Measurement'}
              </button>
            </div>
          </div>

          {/* ── Add Measurement Form ── */}
          {showForm && (
            <div style={{
              background: 'var(--bg-surface)',
              border: '2px solid var(--accent-primary, #4f46e5)',
              borderRadius: 'var(--radius-lg, 12px)',
              padding: '24px',
              marginBottom: '24px',
              boxShadow: 'var(--shadow-md, 0 4px 12px rgba(0,0,0,0.1))'
            }}>
              <h3 style={{ margin: '0 0 20px', fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Plus size={20} style={{ color: 'var(--accent-primary)' }} />
                Record New Measurement — {patientDisplayName}
              </h3>

              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
                gap: '18px'
              }}>
                {/* Measurement Type */}
                <div>
                  <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '6px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                    Measurement Type *
                  </label>
                  <select
                    value={formType}
                    onChange={(e) => {
                      setFormType(e.target.value);
                      setFormValue('');
                      setFormSystolic('');
                      setFormDiastolic('');
                    }}
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: 'var(--radius-md, 8px)',
                      border: '1px solid var(--border-color)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-primary)',
                      fontSize: '0.95rem',
                      outline: 'none'
                    }}
                  >
                    <option value="">— Select Measurement —</option>
                    {MEASUREMENT_TYPES.map(t => (
                      <option key={t.type} value={t.type}>{t.type} ({t.unit})</option>
                    ))}
                  </select>
                </div>

                {/* Value Field(s) */}
                {formType && (
                  isBP ? (
                    <>
                      <div>
                        <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '6px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                          Systolic (mmHg) *
                        </label>
                        <input
                          type="number"
                          value={formSystolic}
                          onChange={(e) => setFormSystolic(e.target.value)}
                          placeholder="e.g. 120"
                          min="0"
                          step="1"
                          style={{
                            width: '100%',
                            padding: '10px 14px',
                            borderRadius: 'var(--radius-md, 8px)',
                            border: '1px solid var(--border-color)',
                            background: 'var(--bg-primary)',
                            color: 'var(--text-primary)',
                            fontSize: '0.95rem',
                            outline: 'none',
                            boxSizing: 'border-box'
                          }}
                        />
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '6px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                          Diastolic (mmHg) *
                        </label>
                        <input
                          type="number"
                          value={formDiastolic}
                          onChange={(e) => setFormDiastolic(e.target.value)}
                          placeholder="e.g. 80"
                          min="0"
                          step="1"
                          style={{
                            width: '100%',
                            padding: '10px 14px',
                            borderRadius: 'var(--radius-md, 8px)',
                            border: '1px solid var(--border-color)',
                            background: 'var(--bg-primary)',
                            color: 'var(--text-primary)',
                            fontSize: '0.95rem',
                            outline: 'none',
                            boxSizing: 'border-box'
                          }}
                        />
                      </div>
                    </>
                  ) : (
                    <div>
                      <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '6px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                        Value ({currentUnit}) *
                      </label>
                      <input
                        type="number"
                        value={formValue}
                        onChange={(e) => setFormValue(e.target.value)}
                        placeholder={`Enter ${formType} value`}
                        step="any"
                        style={{
                          width: '100%',
                          padding: '10px 14px',
                          borderRadius: 'var(--radius-md, 8px)',
                          border: '1px solid var(--border-color)',
                          background: 'var(--bg-primary)',
                          color: 'var(--text-primary)',
                          fontSize: '0.95rem',
                          outline: 'none',
                          boxSizing: 'border-box'
                        }}
                      />
                    </div>
                  )
                )}

                {/* Date/Time */}
                <div>
                  <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '6px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                    Date / Time *
                  </label>
                  <input
                    type="datetime-local"
                    value={formDateTime}
                    onChange={(e) => setFormDateTime(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: 'var(--radius-md, 8px)',
                      border: '1px solid var(--border-color)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-primary)',
                      fontSize: '0.95rem',
                      outline: 'none',
                      boxSizing: 'border-box'
                    }}
                  />
                </div>

                {/* Notes */}
                <div style={{ gridColumn: '1 / -1' }}>
                  <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '6px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                    Notes (Optional)
                  </label>
                  <input
                    type="text"
                    value={formNotes}
                    onChange={(e) => setFormNotes(e.target.value)}
                    placeholder="e.g. Morning reading, Fasting, Post-exercise"
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: 'var(--radius-md, 8px)',
                      border: '1px solid var(--border-color)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-primary)',
                      fontSize: '0.95rem',
                      outline: 'none',
                      boxSizing: 'border-box'
                    }}
                  />
                </div>
              </div>

              {/* Unit display */}
              {formType && (
                <div style={{ marginTop: '12px', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  <FileText size={14} style={{ verticalAlign: 'middle', marginRight: '4px' }} />
                  Unit: <strong>{currentUnit}</strong> &nbsp;|&nbsp; Source: <strong>MANUAL</strong> &nbsp;|&nbsp; Patient: <strong>{patientIdVal}</strong>
                </div>
              )}

              {/* Save Button */}
              <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                <button
                  className="btn-secondary"
                  onClick={() => { setShowForm(false); resetForm(); }}
                  style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                >
                  Cancel
                </button>
                <button
                  className="btn-primary"
                  onClick={handleSave}
                  disabled={saving}
                  style={{ display: 'flex', alignItems: 'center', gap: '6px', minWidth: '160px', justifyContent: 'center' }}
                >
                  {saving ? (
                    <>
                      <RefreshCw size={16} className="spin-icon" />
                      Saving...
                    </>
                  ) : (
                    <>
                      <CheckCircle2 size={16} />
                      Save Measurement
                    </>
                  )}
                </button>
              </div>
            </div>
          )}

          {/* ── Recent Measurements Section ── */}
          <div style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg, 12px)',
            padding: '20px 24px',
            boxShadow: 'var(--shadow-sm)'
          }}>
            {/* Section Header with Filter */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px', marginBottom: '18px' }}>
              <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Calendar size={20} style={{ color: 'var(--accent-primary)' }} />
                Recent Measurements
                {measurements.length > 0 && (
                  <span style={{
                    fontSize: '0.8rem',
                    fontWeight: 600,
                    background: 'var(--accent-light, #e0e7ff)',
                    color: 'var(--accent-primary, #4f46e5)',
                    padding: '2px 10px',
                    borderRadius: '999px',
                    marginLeft: '6px'
                  }}>
                    {filteredMeasurements.length}
                  </span>
                )}
              </h3>

              {recordedTypes.length > 1 && (
                <select
                  value={filterType}
                  onChange={(e) => setFilterType(e.target.value)}
                  style={{
                    padding: '6px 12px',
                    borderRadius: 'var(--radius-md, 8px)',
                    border: '1px solid var(--border-color)',
                    background: 'var(--bg-primary)',
                    color: 'var(--text-primary)',
                    fontSize: '0.85rem'
                  }}
                >
                  <option value="ALL">All Types</option>
                  {recordedTypes.map(t => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
              )}
            </div>

            {/* Loading State */}
            {loadingMeasurements ? (
              <div style={{ padding: '40px 20px', textAlign: 'center', color: 'var(--text-muted)' }}>
                <RefreshCw size={24} className="spin-icon" style={{ marginBottom: '10px' }} />
                <p>Loading measurements...</p>
              </div>
            ) : filteredMeasurements.length === 0 ? (
              /* Empty State */
              <div style={{
                padding: '50px 20px',
                textAlign: 'center',
                color: 'var(--text-muted)'
              }}>
                <HeartPulse size={40} style={{ marginBottom: '12px', opacity: 0.4 }} />
                <p style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '6px', color: 'var(--text-secondary)' }}>
                  No health measurements recorded yet.
                </p>
                <p style={{ fontSize: '0.85rem' }}>
                  Click <strong>"Add New Measurement"</strong> to record the first measurement for {patientDisplayName}.
                </p>
              </div>
            ) : (
              /* Measurement List */
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {filteredMeasurements.map((m) => {
                  const tc = TYPE_COLORS[m.measurementType] || { color: '#64748b', bg: '#f1f5f9' };
                  const isBPRecord = m.measurementType === 'Blood Pressure';
                  const displayValue = isBPRecord
                    ? `${m.systolic ?? '—'}/${m.diastolic ?? '—'}`
                    : (m.value != null ? m.value : '—');

                  return (
                    <div
                      key={m.id}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '14px 18px',
                        background: 'var(--bg-primary)',
                        borderRadius: 'var(--radius-md, 8px)',
                        border: '1px solid var(--border-color)',
                        transition: 'box-shadow 0.15s ease',
                        gap: '16px',
                        flexWrap: 'wrap'
                      }}
                    >
                      {/* Left: Type Badge + Value */}
                      <div style={{ display: 'flex', alignItems: 'center', gap: '14px', flex: '1 1 300px', minWidth: 0 }}>
                        <span style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '6px',
                          padding: '5px 14px',
                          borderRadius: '999px',
                          fontSize: '0.82rem',
                          fontWeight: 700,
                          background: tc.bg,
                          color: tc.color,
                          whiteSpace: 'nowrap',
                          border: `1px solid ${tc.color}22`
                        }}>
                          {m.measurementType}
                        </span>
                        <span style={{
                          fontSize: '1.15rem',
                          fontWeight: 700,
                          color: 'var(--text-primary)',
                          whiteSpace: 'nowrap'
                        }}>
                          {displayValue}
                          <span style={{ fontSize: '0.8rem', fontWeight: 500, color: 'var(--text-muted)', marginLeft: '4px' }}>
                            {m.unit || ''}
                          </span>
                        </span>
                      </div>

                      {/* Middle: Date + Notes */}
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', flex: '1 1 200px', minWidth: 0 }}>
                        <span style={{ fontSize: '0.82rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                          <Clock size={12} />
                          {formatDate(m.recordedAt)}
                        </span>
                        {m.notes && (
                          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontStyle: 'italic', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {m.notes}
                          </span>
                        )}
                      </div>

                      {/* Right: Source + Delete */}
                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexShrink: 0 }}>
                        <span style={{
                          fontSize: '0.75rem',
                          color: 'var(--text-muted)',
                          background: 'var(--bg-surface)',
                          padding: '3px 8px',
                          borderRadius: 'var(--radius-sm, 4px)',
                          border: '1px solid var(--border-color)',
                          textTransform: 'uppercase',
                          fontWeight: 600,
                          letterSpacing: '0.3px'
                        }}>
                          {m.source || 'MANUAL'}
                        </span>
                        <button
                          onClick={() => handleDelete(m.id)}
                          disabled={deletingId === m.id}
                          title="Delete measurement"
                          style={{
                            background: 'none',
                            border: '1px solid var(--border-color)',
                            borderRadius: 'var(--radius-sm, 4px)',
                            padding: '6px',
                            cursor: deletingId === m.id ? 'not-allowed' : 'pointer',
                            color: 'var(--text-muted)',
                            display: 'flex',
                            opacity: deletingId === m.id ? 0.5 : 1,
                            transition: 'color 0.15s ease'
                          }}
                          onMouseEnter={(e) => e.currentTarget.style.color = '#dc2626'}
                          onMouseLeave={(e) => e.currentTarget.style.color = 'var(--text-muted)'}
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
