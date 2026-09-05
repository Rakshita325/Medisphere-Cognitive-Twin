import React from 'react';
import { Clock, CheckCircle2, FileCode, Dna, UserPlus } from 'lucide-react';
import { getPatientName } from '../services/api';

export default function PatientTimeline({ patient, twinData }) {
  if (!patient) return null;

  const patientName = getPatientName(patient);

  const events = [];

  // 1. FHIR Patient Resource creation / last updated event
  if (patient.meta && patient.meta.lastUpdated) {
    events.push({
      id: 'fhir-sync',
      title: 'FHIR Patient Resource Synchronized',
      timestamp: new Date(patient.meta.lastUpdated).toLocaleString(),
      description: `Resource ID: ${patient.id || 'N/A'} fetched from HL7 FHIR R4 server`,
      icon: FileCode
    });
  } else {
    events.push({
      id: 'fhir-sync-now',
      title: 'FHIR Patient Resource Loaded',
      timestamp: 'Current Session',
      description: `Patient resource ${patient.id || ''} loaded into MediSphere Twin Engine`,
      icon: FileCode
    });
  }

  // 2. Patient registered event (birthDate / record entry)
  if (patient.birthDate) {
    events.push({
      id: 'dob-record',
      title: 'Patient Date of Birth Recorded',
      timestamp: patient.birthDate,
      description: `Date of birth ${patient.birthDate} registered in EHR records`,
      icon: UserPlus
    });
  }

  // 3. Twin persisted event if twin exists
  if (twinData) {
    events.push({
      id: 'twin-created',
      title: 'Digital Health Twin Created & Saved',
      timestamp: 'Persisted in MongoDB',
      description: `Twin Record ID: ${twinData.id || twinData.patientId} created for ${twinData.name}`,
      icon: Dna
    });
  }

  return (
    <div className="timeline-card" id="patient-timeline-section">
      <div className="timeline-title">
        <Clock size={20} style={{ color: 'var(--accent-primary)' }} />
        Patient Clinical Timeline — {patientName}
      </div>

      {events.length === 0 ? (
        <div className="empty-state-box">
          No historical timeline events found for this patient on the FHIR server.
        </div>
      ) : (
        <div className="timeline-list">
          {events.map((evt) => {
            const Icon = evt.icon || CheckCircle2;
            return (
              <div key={evt.id} className="timeline-item">
                <div className="timeline-dot"></div>
                <div className="timeline-time">{evt.timestamp}</div>
                <div className="timeline-event-name">{evt.title}</div>
                <div className="timeline-event-desc">{evt.description}</div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
