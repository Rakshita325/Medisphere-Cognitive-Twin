import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { RefreshCw, AlertCircle } from 'lucide-react';
import StatsCards from '../components/StatsCards';
import PatientSelector from '../components/PatientSelector';
import PatientOverviewCard from '../components/PatientOverviewCard';
import PatientTimeline from '../components/PatientTimeline';
import ActionModal from '../components/ActionModal';
import { getFhirPatients, getPatientTwins, getPatient360, savePatientTwin } from '../services/api';

export default function Patient360Dashboard() {
  const [searchParams] = useSearchParams();
  const urlPatientId = searchParams.get('patientId');

  const [patients, setPatients] = useState([]);
  const [twins, setTwins] = useState([]);
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [loading, setLoading] = useState(true);
  const [savingTwin, setSavingTwin] = useState(false);
  const [error, setError] = useState(null);
  const [patient360, setPatient360] = useState(null);

  // Modal dialog state
  const [modalOpen, setModalOpen] = useState(false);
  const [modalData, setModalData] = useState(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [patientsResult, twinsResult] = await Promise.allSettled([
        getFhirPatients(),
        getPatientTwins().catch((twinErr) => {
          console.warn('Could not load existing twins list:', twinErr);
          return [];
        })
      ]);

      const patientsList = patientsResult.status === 'fulfilled' && Array.isArray(patientsResult.value)
        ? patientsResult.value
        : [];

      setPatients(patientsList);
      setTwins(twinsResult.status === 'fulfilled' && Array.isArray(twinsResult.value) ? twinsResult.value : []);

      if (patientsList.length > 0) {
        if (urlPatientId) {
          const match = patientsList.find(p => String(p.id) === String(urlPatientId) || String(p.sourcePatientId) === String(urlPatientId));
          if (match) {
            setSelectedPatient(match);
            return;
          }
        }
        if (!selectedPatient) {
          setSelectedPatient(patientsList[0]);
        }
      }

      if (patientsResult.status === 'rejected') {
        throw patientsResult.reason;
      }
    } catch (err) {
      console.error('Failed to load Patient 360 data:', err);
      setError('Unable to load patient data from FHIR server. Please ensure the Spring Boot backend is running.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    if (!selectedPatient) {
      setPatient360(null);
      return;
    }

    let isActive = true;
    const sourcePatientId = selectedPatient.sourcePatientId || selectedPatient.id;

    getPatient360(sourcePatientId)
      .then((data) => {
        if (isActive) {
          setPatient360(data);
        }
      })
      .catch((err) => {
        if (isActive) {
          setPatient360(null);
          setError(err.message || 'Unable to load Patient 360 data.');
        }
      });

    return () => {
      isActive = false;
    };
  }, [selectedPatient]);

  // Find twin data for selected patient
  const selectedTwin = selectedPatient && twins.length > 0
    ? twins.find(t => String(t.patientId) === String(selectedPatient.id))
    : null;

  // Handle Save / Create Twin action
  const handleSaveTwin = async (patientId) => {
    setSavingTwin(true);
    try {
      const createdTwin = await savePatientTwin(patientId);
      
      // Update twins list
      setTwins(prev => {
        const filtered = prev.filter(t => String(t.patientId) !== String(patientId));
        return [...filtered, createdTwin];
      });

      setModalData({
        title: 'Digital Twin Saved',
        message: `Successfully created and saved Digital Health Twin for patient ID "${patientId}". Data persisted in backend database.`,
        type: 'success'
      });
      setModalOpen(true);
    } catch (err) {
      console.error('Failed to save twin:', err);
      setModalData({
        title: 'Save Twin Failed',
        message: err.message || 'Failed to save digital twin to backend.',
        type: 'warning'
      });
      setModalOpen(true);
    } finally {
      setSavingTwin(false);
    }
  };

  // Handle View Timeline action
  const handleViewTimeline = () => {
    const el = document.getElementById('patient-timeline-section');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <div className="dashboard-content">
      {/* Top Title & Refresh */}
      <div className="page-header-row">
        <div className="page-title-group">
          <h1>Patient 360 Dashboard</h1>
          <p>Real-time FHIR clinical view &amp; Digital Health Twin foundation</p>
        </div>

        <button className="btn-secondary" onClick={loadData} disabled={loading}>
          <RefreshCw size={16} className={loading ? 'spin-icon' : ''} />
          Refresh FHIR Data
        </button>
      </div>

      {/* Error Banner if API fails */}
      {error && (
        <div 
          style={{
            backgroundColor: 'var(--status-warning-bg)',
            color: 'var(--status-warning-text)',
            padding: '16px 20px',
            borderRadius: 'var(--radius-md)',
            marginBottom: '24px',
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            border: '1px solid var(--border-color)'
          }}
        >
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {/* Stats Cards Row */}
      <StatsCards 
        patientCount={patients.length} 
        twinsCount={twins.length} 
        loading={loading} 
      />

      {/* Patient Selection Dropdown */}
      <PatientSelector
        patients={patients}
        selectedPatient={selectedPatient}
        onSelectPatient={setSelectedPatient}
        loading={loading}
      />

      {/* Overview Card — full width, no 3D view */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
        <PatientOverviewCard
          patient={patient360?.patient || selectedPatient}
          patient360={patient360}
          twinData={patient360?.digitalTwin || selectedTwin}
          onSaveTwin={handleSaveTwin}
          onViewTimeline={handleViewTimeline}
          savingTwin={savingTwin}
        />

        <PatientTimeline 
          patient={selectedPatient} 
          twinData={selectedTwin} 
        />
      </div>

      {/* Action Notification Modal */}
      <ActionModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        modalData={modalData}
      />
    </div>
  );
}
