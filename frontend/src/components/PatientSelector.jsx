import React from 'react';
import { UserCheck, Search } from 'lucide-react';
import { getPatientName } from '../services/api';

export default function PatientSelector({ patients, selectedPatient, onSelectPatient, loading }) {
  if (loading) {
    return (
      <div className="patient-selector-card">
        <span className="selector-label">
          <UserCheck size={20} className="stat-icon-wrapper" />
          Loading Patients from FHIR Server...
        </span>
      </div>
    );
  }

  if (!patients || patients.length === 0) {
    return (
      <div className="patient-selector-card">
        <span className="selector-label" style={{ color: 'var(--text-muted)' }}>
          <UserCheck size={20} />
          No patients found on FHIR Server
        </span>
      </div>
    );
  }

  const selectedId = selectedPatient ? (selectedPatient.id || '') : '';

  return (
    <div className="patient-selector-card">
      <span className="selector-label">
        <UserCheck size={20} style={{ color: 'var(--accent-primary)' }} />
        Select Patient:
      </span>

      <select
        className="patient-select-dropdown"
        value={selectedId}
        onChange={(e) => {
          const found = patients.find(p => String(p.id) === e.target.value);
          if (found) onSelectPatient(found);
        }}
      >
        {patients.map((p) => {
          const name = getPatientName(p);
          const gender = p.gender ? p.gender.toUpperCase() : 'N/A';
          const dob = p.birthDate ? p.birthDate : 'N/A';
          const id = p.id || 'N/A';
          return (
            <option key={id} value={id}>
              {name} (ID: {id} | {gender} | DOB: {dob})
            </option>
          );
        })}
      </select>
    </div>
  );
}
