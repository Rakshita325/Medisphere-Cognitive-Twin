import React from 'react';
import { Users, Server, Dna } from 'lucide-react';

export default function StatsCards({ patientCount, twinsCount, loading }) {
  return (
    <div className="stats-cards-grid">
      {/* Card 1: Patients Onboarded */}
      <div className="stat-card">
        <div className="stat-header">
          <span className="stat-title">Patients Onboarded</span>
          <div className="stat-icon-wrapper">
            <Users size={20} />
          </div>
        </div>
        <div className="stat-value">
          {loading ? '...' : (patientCount !== null ? patientCount : 'N/A')}
        </div>
        <div className="stat-footer">
          <span className="stat-badge success">Live FHIR</span>
          <span>Loaded from FHIR Server</span>
        </div>
      </div>

      {/* Card 2: FHIR Resources */}
      <div className="stat-card">
        <div className="stat-header">
          <span className="stat-title">FHIR Resources</span>
          <div className="stat-icon-wrapper">
            <Server size={20} />
          </div>
        </div>
        <div className="stat-value" style={{ fontSize: '1.6rem' }}>
          FHIR R4
        </div>
        <div className="stat-footer">
          <span className="stat-badge info">Active</span>
          <span>Synced with HL7 FHIR Endpoint</span>
        </div>
      </div>

      {/* Card 3: Twins Created */}
      <div className="stat-card">
        <div className="stat-header">
          <span className="stat-title">Twins Created</span>
          <div className="stat-icon-wrapper">
            <Dna size={20} />
          </div>
        </div>
        <div className="stat-value">
          {loading ? '...' : (twinsCount !== null ? twinsCount : '0')}
        </div>
        <div className="stat-footer">
          <span className="stat-badge success">MongoDB</span>
          <span>Persisted Digital Health Twins</span>
        </div>
      </div>
    </div>
  );
}
