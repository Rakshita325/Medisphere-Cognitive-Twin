import React from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  FileText, 
  Calendar, 
  Activity, 
  Heart, 
  Pill, 
  Stethoscope, 
  Clock,
  Dna,
  BrainCircuit
} from 'lucide-react';
import { getPatientName, calculateAge } from '../services/api';

export default function PatientOverviewCard({ 
  patient, 
  patient360,
  twinData,
  onSaveTwin, 
  onViewTimeline, 
  savingTwin 
}) {
  const navigate = useNavigate();

  if (!patient) {
    return (
      <div className="overview-card">
        <div className="empty-state-box">
          Please select a patient to view the Patient 360 Overview.
        </div>
      </div>
    );
  }

  const name = getPatientName(patient);
  const age = calculateAge(patient.birthDate);
  const gender = patient.gender ? (patient.gender.charAt(0).toUpperCase() + patient.gender.slice(1)) : 'Unknown';
  const birthDate = patient.birthDate || 'Not specified';
  const id = patient.id || 'N/A';
  const conditions = patient360?.conditions || [];
  const labs = patient360?.labs || [];
  const observations = patient360?.vitals || [];
  const wearable = patient360?.wearableData || [];
  const latest = (code) => observations.find(observation => observation.code === code);
  const heartRate = latest('8867-4');
  const oxygen = latest('59408-5');
  const temperature = latest('8310-5');
  const bloodPressure = observations.find(observation => observation.code === '85354-9');
  const steps = wearable.find(observation => observation.code === '41950-7');
  const sleep = wearable.find(observation => observation.code === '93832-4');
  const lastUpdated = patient.meta && patient.meta.lastUpdated 
    ? new Date(patient.meta.lastUpdated).toLocaleString() 
    : 'Loaded Live from FHIR Server';

  return (
    <div className="overview-card">
      <div className="card-header-bar">
        <div className="patient-twin-title">
          <Dna size={24} style={{ color: 'var(--accent-primary)' }} />
          <div>
            <h2>Digital Health Twin — {name}</h2>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              FHIR Resource ID: {id}
            </div>
          </div>
        </div>

        <div className="fhir-status-pill">
          <FileText size={14} />
          FHIR R4 Resource
        </div>
      </div>

      {/* Demographics & Twin status */}
      <div className="clinical-info-grid">
        <div className="info-box">
          <div className="info-label">Demographics</div>
          <div className="info-value">
            {age !== 'N/A' ? `${age} yrs` : 'Age N/A'} | {gender}
          </div>
          <div className="info-subtext">DOB: {birthDate}</div>
        </div>

        <div className="info-box">
          <div className="info-label">Digital Twin Status</div>
          <div className="info-value" style={{ color: twinData ? 'var(--status-success-text)' : 'var(--text-secondary)' }}>
            {twinData ? 'Active Twin Persisted' : 'Twin Not Created Yet'}
          </div>
          <div className="info-subtext">
            {twinData ? `Diagnosis: ${twinData.diagnosis || 'Recorded'}` : 'Click [Save Twin] below'}
          </div>
        </div>

        <div className="info-box">
          <div className="info-label">Last Resource Sync</div>
          <div className="info-value" style={{ fontSize: '0.85rem' }}>
            {lastUpdated}
          </div>
          <div className="info-subtext">Source: HL7 FHIR Endpoint</div>
        </div>
      </div>

      {/* Vitals Stream Section */}
      <div>
        <div className="info-label" style={{ marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Heart size={16} style={{ color: '#ef4444' }} />
          Vitals Stream
        </div>

        <div className="vitals-grid">
          <div className="vital-card-mini">
            <div className="vital-name">Heart Rate</div>
            <div className="vital-num">{heartRate?.value || '--'}</div>
            <div className="vital-unit">bpm</div>
          </div>
          <div className="vital-card-mini">
            <div className="vital-name">Blood Pressure</div>
            <div className="vital-num">{bloodPressure ? `${bloodPressure.systolic || '--'}/${bloodPressure.diastolic || '--'}` : '--/--'}</div>
            <div className="vital-unit">mmHg</div>
          </div>
          <div className="vital-card-mini">
            <div className="vital-name">SpO₂</div>
            <div className="vital-num">{oxygen?.value ? `${oxygen.value}%` : '--%'}</div>
            <div className="vital-unit">O₂ saturation</div>
          </div>
          <div className="vital-card-mini">
            <div className="vital-name">Glucose</div>
            <div className="vital-num">{labs[0]?.value || '--'}</div>
            <div className="vital-unit">mg/dL</div>
          </div>
        </div>
        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '6px' }}>
          Latest temperature: {temperature?.value ? `${temperature.value} ${temperature.unit || ''}` : 'not recorded'} | Steps: {steps?.value || '--'} | Sleep: {sleep?.value ? `${sleep.value} h` : '--'}
        </div>
      </div>

      {/* Clinical Conditions, Labs, & Medications */}
      <div className="clinical-info-grid">
        <div className="info-box">
          <div className="info-label" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Stethoscope size={14} />
            Conditions
          </div>
          <div className="info-subtext" style={{ fontStyle: 'italic', marginTop: '4px' }}>
            {conditions.length ? conditions.map(condition => condition.name).join(', ') : 'No condition resources recorded on FHIR server.'}
          </div>
        </div>

        <div className="info-box">
          <div className="info-label" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Activity size={14} />
            Lab Results
          </div>
          <div className="info-subtext" style={{ fontStyle: 'italic', marginTop: '4px' }}>
            {labs.length ? labs.slice(0, 3).map(lab => `${lab.type}: ${lab.value || '--'} ${lab.unit || ''}`).join(' | ') : 'No diagnostic lab reports available.'}
          </div>
        </div>

        <div className="info-box">
          <div className="info-label" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Pill size={14} />
            Active Medications
          </div>
          <div className="info-subtext" style={{ fontStyle: 'italic', marginTop: '4px' }}>
            Consent: {patient360 ? (patient360.consentGranted ? 'Granted' : 'Denied') : 'Loading'}
          </div>
        </div>
      </div>

      {/* Action Buttons Toolbar */}
      <div className="action-buttons-bar" style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
        <button 
          className="btn-primary" 
          onClick={() => onSaveTwin(patient.id)}
          disabled={savingTwin}
        >
          <Dna size={16} />
          {savingTwin ? 'Saving Twin...' : (twinData ? 'Update Twin' : 'Save / Create Twin')}
        </button>

        <button 
          className="btn-secondary" 
          onClick={() => navigate(`/risk-predictions?patientId=${encodeURIComponent(patient.id)}`)}
          style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          <BrainCircuit size={16} />
          Generate AI Prediction
        </button>

        <button className="btn-secondary" onClick={onViewTimeline}>
          <Clock size={16} />
          View Timeline
        </button>
      </div>
    </div>
  );
}
