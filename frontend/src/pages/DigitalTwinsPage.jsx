import React, { useState, useEffect } from 'react';
import {
  Dna, RefreshCw, AlertCircle, Users, ChevronDown,
  User, Hash, Calendar, Stethoscope
} from 'lucide-react';
import { getPatientTwins, getPatientTwinById } from '../services/api';

export default function DigitalTwinsPage() {
  const [twins, setTwins] = useState([]);
  const [selectedTwinId, setSelectedTwinId] = useState(null);
  const [selectedTwin, setSelectedTwin] = useState(null);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState(null);

  const loadTwins = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getPatientTwins();
      const twinsList = Array.isArray(data) ? data : [];
      setTwins(twinsList);
      if (twinsList.length > 0 && !selectedTwinId) {
        selectTwin(twinsList[0]);
      }
    } catch (err) {
      console.error('Failed to load twins:', err);
      setError('Unable to load Digital Twins. Please ensure the backend is running.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTwins();
  }, []);

  const selectTwin = async (twin) => {
    setSelectedTwinId(twin.id);
    setSelectedTwin(twin);

    // Optionally fetch full detail
    if (twin.id) {
      setDetailLoading(true);
      try {
        const detail = await getPatientTwinById(twin.id);
        setSelectedTwin(detail);
      } catch {
        // Fall back to list data
        setSelectedTwin(twin);
      } finally {
        setDetailLoading(false);
      }
    }
  };

  return (
    <div className="dashboard-content">
      <div className="page-header-row">
        <div className="page-title-group">
          <h1>Digital Twins</h1>
          <p>Patient Digital Health Twins from MongoDB</p>
        </div>
        <button className="btn-secondary" onClick={loadTwins} disabled={loading}>
          <RefreshCw size={16} className={loading ? 'spin-icon' : ''} />
          Refresh
        </button>
      </div>

      {error && (
        <div className="patients-error-banner">
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="patients-loading">
          <div className="patients-loading-spinner" />
          <p>Loading Digital Twins from MongoDB...</p>
        </div>
      ) : twins.length === 0 ? (
        <div className="patients-empty">
          <Dna size={48} />
          <h3>No Digital Twins</h3>
          <p>No Digital Health Twins have been created yet. Go to the Patients page to create twins from FHIR records.</p>
        </div>
      ) : (
        <div className="twins-layout">
          {/* Twin Selector */}
          <div className="twins-selector-panel">
            <div className="twins-selector-header">
              <Dna size={18} />
              <span>Select Digital Twin</span>
              <span className="twins-count-badge">{twins.length}</span>
            </div>
            <div className="twins-list">
              {twins.map((twin) => {
                const isActive = selectedTwinId === twin.id;
                const gender = twin.gender
                  ? twin.gender.charAt(0).toUpperCase() + twin.gender.slice(1)
                  : 'N/A';
                return (
                  <button
                    key={twin.id}
                    className={`twins-list-item ${isActive ? 'active' : ''}`}
                    onClick={() => selectTwin(twin)}
                  >
                    <div className="twins-list-item-avatar">
                      <User size={18} />
                    </div>
                    <div className="twins-list-item-info">
                      <div className="twins-list-item-name">{twin.name || 'Unknown'}</div>
                      <div className="twins-list-item-meta">
                        {gender} · {twin.age ? `${twin.age} yrs` : 'N/A'}
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Twin Detail Panel */}
          <div className="twins-detail-panel">
            {selectedTwin ? (
              <>
                <div className="twins-detail-header">
                  <div className="twins-detail-avatar">
                    <Dna size={28} />
                  </div>
                  <div>
                    <h2>{selectedTwin.name || 'Unknown Patient'}</h2>
                    <span className="twins-detail-id">
                      Twin ID: {selectedTwin.id || 'N/A'}
                    </span>
                  </div>
                  <div className="twins-detail-status-pill">Active</div>
                </div>

                {detailLoading ? (
                  <div className="patients-loading" style={{ padding: '32px 0' }}>
                    <div className="patients-loading-spinner" />
                    <p>Loading twin details...</p>
                  </div>
                ) : (
                  <>
                    {/* Patient Info */}
                    <div className="twins-detail-section">
                      <h3>
                        <User size={18} />
                        Patient Information
                      </h3>
                      <div className="clinical-info-grid">
                        <div className="info-box">
                          <div className="info-label">Name</div>
                          <div className="info-value">{selectedTwin.name || 'N/A'}</div>
                        </div>
                        <div className="info-box">
                          <div className="info-label">Patient ID</div>
                          <div className="info-value" style={{ fontFamily: 'monospace' }}>
                            {selectedTwin.patientId || 'N/A'}
                          </div>
                        </div>
                        <div className="info-box">
                          <div className="info-label">Age</div>
                          <div className="info-value">
                            {selectedTwin.age ? `${selectedTwin.age} years` : 'N/A'}
                          </div>
                        </div>
                        <div className="info-box">
                          <div className="info-label">Gender</div>
                          <div className="info-value">
                            {selectedTwin.gender
                              ? selectedTwin.gender.charAt(0).toUpperCase() + selectedTwin.gender.slice(1)
                              : 'N/A'}
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Twin Info */}
                    <div className="twins-detail-section">
                      <h3>
                        <Dna size={18} />
                        Digital Twin Details
                      </h3>
                      <div className="clinical-info-grid">
                        <div className="info-box">
                          <div className="info-label">Twin Status</div>
                          <div className="info-value" style={{ color: 'var(--status-success-text)' }}>
                            Active — Persisted in MongoDB
                          </div>
                        </div>
                        <div className="info-box">
                          <div className="info-label">Diagnosis</div>
                          <div className="info-value">{selectedTwin.diagnosis || 'Recorded'}</div>
                        </div>
                        <div className="info-box">
                          <div className="info-label">Twin Record ID</div>
                          <div className="info-value" style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                            {selectedTwin.id || 'N/A'}
                          </div>
                        </div>
                      </div>
                    </div>
                  </>
                )}
              </>
            ) : (
              <div className="patients-empty" style={{ padding: '48px 24px' }}>
                <Dna size={36} />
                <h3>Select a Twin</h3>
                <p>Choose a Digital Twin from the list to view its details.</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
