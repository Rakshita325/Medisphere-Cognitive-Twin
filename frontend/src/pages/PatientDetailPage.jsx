import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import {
  ArrowLeft, Dna, Calendar, User, Hash, RefreshCw,
  AlertCircle, CheckCircle, FileText
} from 'lucide-react';
import { getFhirPatients, getPatientTwins, savePatientTwin, getPatientName, calculateAge } from '../services/api';
import ActionModal from '../components/ActionModal';

export default function PatientDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [patient, setPatient] = useState(null);
  const [twin, setTwin] = useState(null);
  const [loading, setLoading] = useState(true);
  const [savingTwin, setSavingTwin] = useState(false);
  const [error, setError] = useState(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [modalData, setModalData] = useState(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [patientsData, twinsData] = await Promise.all([
        getFhirPatients(),
        getPatientTwins().catch(() => [])
      ]);
      const patients = Array.isArray(patientsData) ? patientsData : [];
      const twins = Array.isArray(twinsData) ? twinsData : [];

      const found = patients.find(p => String(p.id) === String(id));
      if (!found) {
        setError(`Patient with ID "${id}" not found.`);
        return;
      }
      setPatient(found);

      const existingTwin = twins.find(t => String(t.patientId) === String(id));
      setTwin(existingTwin || null);
    } catch (err) {
      console.error('Failed to load patient details:', err);
      setError('Unable to load patient data. Please ensure the backend is running.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [id]);

  const handleCreateTwin = async () => {
    if (!patient) return;
    setSavingTwin(true);
    try {
      const createdTwin = await savePatientTwin(patient.id);
      setTwin(createdTwin);
      setModalData({
        title: 'Digital Twin Created',
        message: `Successfully created Digital Health Twin for ${getPatientName(patient)}.`,
        type: 'success'
      });
      setModalOpen(true);
    } catch (err) {
      setModalData({
        title: 'Failed to Create Twin',
        message: err.message || 'Failed to save digital twin to backend.',
        type: 'warning'
      });
      setModalOpen(true);
    } finally {
      setSavingTwin(false);
    }
  };

  if (loading) {
    return (
      <div className="dashboard-content">
        <div className="patients-loading">
          <div className="patients-loading-spinner" />
          <p>Loading patient details...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard-content">
        <div className="page-header-row">
          <div className="page-title-group">
            <h1>Patient Details</h1>
          </div>
          <Link to="/patients" className="btn-secondary">
            <ArrowLeft size={16} />
            Back to Patients
          </Link>
        </div>
        <div className="patients-error-banner">
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      </div>
    );
  }

  const name = getPatientName(patient);
  const age = calculateAge(patient.birthDate);
  const gender = patient.gender
    ? patient.gender.charAt(0).toUpperCase() + patient.gender.slice(1)
    : 'Unknown';

  return (
    <div className="dashboard-content">
      {/* Header */}
      <div className="page-header-row">
        <div className="page-title-group">
          <h1>Patient Details</h1>
          <p>{name}</p>
        </div>
        <Link to="/patients" className="btn-secondary">
          <ArrowLeft size={16} />
          Back to Patients
        </Link>
      </div>

      {/* Patient Info Card */}
      <div className="patient-detail-card">
        <div className="patient-detail-header">
          <div className="patient-detail-avatar">
            <User size={32} />
          </div>
          <div>
            <h2>{name}</h2>
            <span className="patient-detail-id">
              <Hash size={14} />
              {patient.id}
            </span>
          </div>
          <div className="patient-detail-fhir-pill">
            <FileText size={14} />
            FHIR R4 Resource
          </div>
        </div>

        <div className="clinical-info-grid">
          <div className="info-box">
            <div className="info-label">Full Name</div>
            <div className="info-value">{name}</div>
          </div>
          <div className="info-box">
            <div className="info-label">Patient ID</div>
            <div className="info-value" style={{ fontFamily: 'monospace' }}>{patient.id}</div>
          </div>
          <div className="info-box">
            <div className="info-label">Age</div>
            <div className="info-value">{age !== 'N/A' ? `${age} years` : 'N/A'}</div>
          </div>
          <div className="info-box">
            <div className="info-label">Gender</div>
            <div className="info-value">{gender}</div>
          </div>
          <div className="info-box">
            <div className="info-label">
              <Calendar size={14} style={{ display: 'inline', marginRight: '4px', verticalAlign: 'middle' }} />
              Date of Birth
            </div>
            <div className="info-value">{patient.birthDate || 'Not specified'}</div>
          </div>
        </div>
      </div>

      {/* Digital Twin Section */}
      <div className="patient-detail-twin-section">
        <h3>
          <Dna size={20} style={{ color: 'var(--accent-primary)' }} />
          Digital Health Twin
        </h3>

        {twin ? (
          <div className="patient-detail-twin-info">
            <div className="patient-detail-twin-status active">
              <CheckCircle size={18} />
              <span>Digital Twin Active</span>
            </div>
            <div className="clinical-info-grid">
              <div className="info-box">
                <div className="info-label">Twin ID</div>
                <div className="info-value" style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                  {twin.id || 'N/A'}
                </div>
              </div>
              <div className="info-box">
                <div className="info-label">Name</div>
                <div className="info-value">{twin.name || name}</div>
              </div>
              <div className="info-box">
                <div className="info-label">Age</div>
                <div className="info-value">{twin.age || age} years</div>
              </div>
              <div className="info-box">
                <div className="info-label">Gender</div>
                <div className="info-value">
                  {twin.gender ? twin.gender.charAt(0).toUpperCase() + twin.gender.slice(1) : gender}
                </div>
              </div>
              <div className="info-box">
                <div className="info-label">Diagnosis</div>
                <div className="info-value">{twin.diagnosis || 'Recorded'}</div>
              </div>
            </div>
            <button
              className="btn-primary"
              onClick={handleCreateTwin}
              disabled={savingTwin}
              style={{ marginTop: '16px' }}
            >
              <RefreshCw size={16} className={savingTwin ? 'spin-icon' : ''} />
              {savingTwin ? 'Updating...' : 'Update Twin'}
            </button>
          </div>
        ) : (
          <div className="patient-detail-twin-empty">
            <Dna size={36} style={{ color: 'var(--text-muted)', opacity: 0.5 }} />
            <p>No Digital Twin has been created for this patient yet.</p>
            <button
              className="btn-primary"
              onClick={handleCreateTwin}
              disabled={savingTwin}
            >
              <Dna size={16} />
              {savingTwin ? 'Creating Twin...' : 'Create Digital Twin'}
            </button>
          </div>
        )}
      </div>

      <ActionModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        modalData={modalData}
      />
    </div>
  );
}
