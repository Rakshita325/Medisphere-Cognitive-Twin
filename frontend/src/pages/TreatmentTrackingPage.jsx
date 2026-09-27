import React, { useState, useEffect, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  CheckCircle2, Clock, XCircle, AlertCircle, PlayCircle, RefreshCw,
  Plus, Calendar, Activity, Heart, ShieldAlert, FileText, Check,
  ChevronRight, Filter, TrendingUp, Info, AlertTriangle, ArrowRight,
  ClipboardList, Edit3, MessageSquare, History, Sparkles
} from 'lucide-react';
import PatientSelector from '../components/PatientSelector';
import {
  getFhirPatients, getAllPatientData, getPatientName, getLatestPatientCarePlan,
  getPatientTreatmentTracking, updateTreatmentStatus, createTreatmentTracking,
  deleteTreatmentTracking, getPatientAdherence, initializeCarePlanTracking,
  getPatientHealthData
} from '../services/api';

const STATUS_CONFIG = {
  PENDING: {
    label: 'Pending',
    color: '#d97706',
    bg: '#fef3c7',
    border: '#fcd34d',
    badgeClass: 'badge-warning',
    icon: Clock
  },
  IN_PROGRESS: {
    label: 'In Progress',
    color: '#0284c7',
    bg: '#e0f2fe',
    border: '#38bdf8',
    badgeClass: 'badge-info',
    icon: PlayCircle
  },
  COMPLETED: {
    label: 'Completed',
    color: '#059669',
    bg: '#d1fae5',
    border: '#34d399',
    badgeClass: 'badge-success',
    icon: CheckCircle2
  },
  MISSED: {
    label: 'Missed',
    color: '#dc2626',
    bg: '#fee2e2',
    border: '#fca5a5',
    badgeClass: 'badge-danger',
    icon: AlertCircle
  },
  CANCELLED: {
    label: 'Cancelled',
    color: '#64748b',
    bg: '#f1f5f9',
    border: '#cbd5e1',
    badgeClass: 'badge-muted',
    icon: XCircle
  }
};

const TYPE_CONFIG = {
  MEDICATION: { label: 'Medication', color: '#8b5cf6', bg: '#ede9fe' },
  VITAL_MONITORING: { label: 'Vitals', color: '#0ea5e9', bg: '#e0f2fe' },
  GLUCOSE_MONITORING: { label: 'Glucose', color: '#f59e0b', bg: '#fef3c7' },
  DIET: { label: 'Diet / Nutrition', color: '#10b981', bg: '#d1fae5' },
  EXERCISE: { label: 'Exercise', color: '#ec4899', bg: '#fce7f3' },
  LIFESTYLE: { label: 'Lifestyle', color: '#6366f1', bg: '#e0e7ff' },
  GENERAL: { label: 'General Care', color: '#64748b', bg: '#f1f5f9' }
};

export default function TreatmentTrackingPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const urlPatientId = searchParams.get('patientId');

  // Patients state
  const [patients, setPatients] = useState([]);
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [loadingPatients, setLoadingPatients] = useState(true);

  // Patient rich details & Care Plan
  const [patientData, setPatientData] = useState(null);
  const [carePlan, setCarePlan] = useState(null);
  const [loadingCarePlan, setLoadingCarePlan] = useState(false);

  // Treatment Tracking records state
  const [trackingList, setTrackingList] = useState([]);
  const [loadingTracking, setLoadingTracking] = useState(false);
  const [statusUpdatingId, setStatusUpdatingId] = useState(null);
  const [syncingCarePlan, setSyncingCarePlan] = useState(false);

  // Adherence analytics
  const [adherenceData, setAdherenceData] = useState(null);

  // Activity History log
  const [activityHistory, setActivityHistory] = useState([]);

  // UI state
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Add intervention modal state
  const [showAddModal, setShowAddModal] = useState(false);
  const [newIntervention, setNewIntervention] = useState({
    description: '',
    interventionType: 'VITAL_MONITORING',
    frequency: 'Daily',
    dueDate: 'Today',
    status: 'PENDING',
    notes: ''
  });

  // Notes editing state
  const [editingNotesId, setEditingNotesId] = useState(null);
  const [notesInput, setNotesInput] = useState('');

  // 1. Load patient roster (reusing identical robust logic as CarePlansPage)
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

  // When selected patient changes, sync URL & fetch tracking data for that patient ONLY
  const handleSelectPatient = (patient) => {
    setSelectedPatient(patient);
    if (patient) {
      const pid = patient.sourcePatientId || patient.patientId || patient.id;
      setSearchParams({ patientId: pid });
      localStorage.setItem('medisphere_selected_patient_id', pid);
    }
  };

  // 2. Fetch patient-specific Care Plan, Treatment Tracking records & Adherence
  const fetchPatientTreatmentData = async (patientId) => {
    if (!patientId) return;
    setLoadingTracking(true);
    setLoadingCarePlan(true);
    setError(null);

    try {
      // 1. Fetch latest care plan for patient
      let latestPlan = null;
      try {
        latestPlan = await getLatestPatientCarePlan(patientId);
        setCarePlan(latestPlan);
      } catch (cpErr) {
        setCarePlan(null);
      } finally {
        setLoadingCarePlan(false);
      }

      // 2. Fetch patient health data for display name
      try {
        const pd = await getPatientHealthData(patientId);
        setPatientData(pd);
      } catch {
        setPatientData(null);
      }

      // 3. Fetch tracking records from backend
      const records = await getPatientTreatmentTracking(patientId);
      setTrackingList(Array.isArray(records) ? records : []);

      // 4. Fetch adherence analytics from backend
      const adh = await getPatientAdherence(patientId);
      setAdherenceData(adh);

    } catch (err) {
      console.error('Error fetching treatment tracking data:', err);
      setError('Could not load treatment tracking data. Please try again.');
      setTrackingList([]);
      setAdherenceData(null);
    } finally {
      setLoadingTracking(false);
    }
  };

  useEffect(() => {
    if (selectedPatient) {
      const pid = selectedPatient.sourcePatientId || selectedPatient.patientId || selectedPatient.id;
      fetchPatientTreatmentData(pid);
    }
  }, [selectedPatient]);

  // Refresh current patient's data
  const refreshData = () => {
    if (selectedPatient) {
      const pid = selectedPatient.sourcePatientId || selectedPatient.patientId || selectedPatient.id;
      fetchPatientTreatmentData(pid);
    }
  };

  // 3. Handle Status Change
  const handleStatusChange = async (trackingItem, newStatus) => {
    if (!trackingItem || !trackingItem.id || statusUpdatingId) return;
    setStatusUpdatingId(trackingItem.id);
    setError(null);
    setSuccessMessage(null);

    const oldStatus = trackingItem.status;

    try {
      const updated = await updateTreatmentStatus(trackingItem.id, {
        status: newStatus,
        notes: trackingItem.notes || undefined
      });

      // Update state locally
      setTrackingList(prev => prev.map(t => t.id === updated.id ? updated : t));

      // Re-calculate or re-fetch adherence immediately
      if (selectedPatient) {
        const pid = selectedPatient.sourcePatientId || selectedPatient.patientId || selectedPatient.id;
        const adh = await getPatientAdherence(pid);
        setAdherenceData(adh);
      }

      // Log into recent treatment activity history
      const historyEntry = {
        id: Date.now() + Math.random(),
        activity: trackingItem.description,
        type: trackingItem.interventionType,
        oldStatus: oldStatus,
        newStatus: newStatus,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
        notes: trackingItem.notes || ''
      };
      setActivityHistory(prev => [historyEntry, ...prev.slice(0, 19)]);

      setSuccessMessage(`Marked "${trackingItem.description}" as ${newStatus}.`);
      setTimeout(() => setSuccessMessage(null), 4000);
    } catch (err) {
      console.error('Failed to update status:', err);
      setError(`Failed to update status: ${err.message || 'Unknown error'}`);
    } finally {
      setStatusUpdatingId(null);
    }
  };

  // Save updated notes
  const handleSaveNotes = async (trackingItem) => {
    if (!trackingItem || !trackingItem.id) return;
    try {
      const updated = await updateTreatmentStatus(trackingItem.id, {
        status: trackingItem.status,
        notes: notesInput
      });
      setTrackingList(prev => prev.map(t => t.id === updated.id ? updated : t));
      setEditingNotesId(null);
      setSuccessMessage('Intervention notes saved.');
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err) {
      setError(`Failed to save notes: ${err.message}`);
    }
  };

  // Sync / Initialize tracking records from Care Plan
  const handleSyncCarePlan = async () => {
    if (!carePlan || !carePlan.id || syncingCarePlan) return;
    setSyncingCarePlan(true);
    setError(null);
    try {
      const initialized = await initializeCarePlanTracking(carePlan.id);
      if (selectedPatient) {
        const pid = selectedPatient.sourcePatientId || selectedPatient.patientId || selectedPatient.id;
        const records = await getPatientTreatmentTracking(pid);
        setTrackingList(records);
        const adh = await getPatientAdherence(pid);
        setAdherenceData(adh);
      }
      setSuccessMessage(`Successfully initialized ${initialized.length} tracking records from Care Plan.`);
      setTimeout(() => setSuccessMessage(null), 4000);
    } catch (err) {
      console.error('Sync failed:', err);
      setError(`Failed to initialize tracking from care plan: ${err.message}`);
    } finally {
      setSyncingCarePlan(false);
    }
  };

  // Add custom intervention
  const handleAddIntervention = async (e) => {
    e.preventDefault();
    if (!newIntervention.description.trim() || !selectedPatient) return;

    const pid = selectedPatient.sourcePatientId || selectedPatient.patientId || selectedPatient.id;
    const pName = (patientData?.personalDetails
      ? `${patientData.personalDetails.firstName || ''} ${patientData.personalDetails.lastName || ''}`.trim()
      : null) || selectedPatient.name;

    try {
      const payload = {
        patientId: pid,
        patientName: pName,
        carePlanId: carePlan?.id || 'DIRECT_CLINICAL_TRACKING',
        interventionType: newIntervention.interventionType,
        description: newIntervention.description.trim(),
        frequency: newIntervention.frequency,
        dueDate: newIntervention.dueDate || 'Today',
        status: newIntervention.status,
        notes: newIntervention.notes || null
      };

      const created = await createTreatmentTracking(payload);
      setTrackingList(prev => [created, ...prev]);

      // Re-fetch adherence
      const adh = await getPatientAdherence(pid);
      setAdherenceData(adh);

      setShowAddModal(false);
      setNewIntervention({
        description: '',
        interventionType: 'VITAL_MONITORING',
        frequency: 'Daily',
        dueDate: 'Today',
        status: 'PENDING',
        notes: ''
      });

      setSuccessMessage('Custom intervention added successfully.');
      setTimeout(() => setSuccessMessage(null), 4000);
    } catch (err) {
      setError(`Failed to create intervention: ${err.message}`);
    }
  };

  // Delete intervention
  const handleDeleteIntervention = async (id, desc) => {
    if (!window.confirm(`Are you sure you want to remove "${desc}"?`)) return;
    try {
      await deleteTreatmentTracking(id);
      setTrackingList(prev => prev.filter(t => t.id !== id));
      if (selectedPatient) {
        const pid = selectedPatient.sourcePatientId || selectedPatient.patientId || selectedPatient.id;
        const adh = await getPatientAdherence(pid);
        setAdherenceData(adh);
      }
      setSuccessMessage(`Removed "${desc}".`);
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err) {
      setError(`Failed to delete intervention: ${err.message}`);
    }
  };

  // Filtered interventions
  const filteredInterventions = useMemo(() => {
    if (statusFilter === 'ALL') return trackingList;
    return trackingList.filter(t => (t.status || 'PENDING').toUpperCase() === statusFilter);
  }, [trackingList, statusFilter]);

  // Derived metrics / fallback adherence computation (client-side matching backend formula)
  const clientAdherence = useMemo(() => {
    const completed = trackingList.filter(t => t.status === 'COMPLETED').length;
    const pending = trackingList.filter(t => t.status === 'PENDING').length;
    const missed = trackingList.filter(t => t.status === 'MISSED').length;
    const inProgress = trackingList.filter(t => t.status === 'IN_PROGRESS').length;
    const cancelled = trackingList.filter(t => t.status === 'CANCELLED').length;
    const totalDue = completed + pending + missed + inProgress;

    if (totalDue === 0) {
      return { totalDue: 0, completed: 0, pending: 0, missed: 0, inProgress: 0, cancelled: 0, percentage: null, display: 'No data' };
    }

    const pct = Math.round((completed / totalDue) * 100);
    return {
      totalDue,
      completed,
      pending,
      missed,
      inProgress,
      cancelled,
      percentage: pct,
      display: `${pct}%`
    };
  }, [trackingList]);

  // Active metrics to display (prefer server adherence, fall back to calculated)
  const displayAdherence = adherenceData?.adherenceDisplay || clientAdherence.display;
  const completedCount = adherenceData ? adherenceData.completed : clientAdherence.completed;
  const pendingCount = adherenceData ? adherenceData.pending : clientAdherence.pending;
  const missedCount = adherenceData ? adherenceData.missed : clientAdherence.missed;
  const totalDueCount = adherenceData ? adherenceData.totalDue : clientAdherence.totalDue;
  const pctValue = adherenceData?.adherencePercentage != null ? adherenceData.adherencePercentage : clientAdherence.percentage;

  // Progress bar color based on percentage
  const getProgressBarColor = (pct) => {
    if (pct == null) return '#94a3b8';
    if (pct >= 80) return '#10b981';
    if (pct >= 50) return '#f59e0b';
    return '#ef4444';
  };

  const patientDisplayName = (patientData?.personalDetails
    ? `${patientData.personalDetails.firstName || ''} ${patientData.personalDetails.lastName || ''}`.trim()
    : null) || selectedPatient?.name || selectedPatient?.displayName || 'Select Patient';

  const patientIdVal = selectedPatient
    ? (selectedPatient.sourcePatientId || selectedPatient.patientId || selectedPatient.id)
    : 'N/A';

  const carePlanIdVal = carePlan?.id || (trackingList.length > 0 && trackingList[0].carePlanId) || 'None';

  return (
    <div className="dashboard-content">
      {/* Header */}
      <div className="page-header-row" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px', marginBottom: '20px' }}>
        <div className="page-title-group">
          <h1 style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Activity className="text-accent" size={28} />
            Treatment Tracking
          </h1>
          <p>Milestone 4 Part 2: Clinical Intervention Tracking & Adherence Analytics</p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <button
            className="btn-secondary"
            onClick={refreshData}
            disabled={loadingTracking}
            title="Refresh Tracking Records"
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <RefreshCw size={16} className={loadingTracking ? 'spin-icon' : ''} />
            Refresh
          </button>

          <button
            className="btn-primary"
            onClick={() => setShowAddModal(true)}
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <Plus size={16} />
            Add Intervention
          </button>

          {carePlan?.id && (
            <button
              className="btn-secondary"
              onClick={() => navigate(`/care-plans?patientId=${patientIdVal}`)}
              style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
            >
              <ClipboardList size={16} />
              View Care Plan
            </button>
          )}
        </div>
      </div>

      {/* Patient Selector */}
      <PatientSelector
        patients={patients}
        selectedPatient={selectedPatient}
        onSelectPatient={handleSelectPatient}
        loading={loadingPatients}
      />

      {/* Error & Success Alerts */}
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

      {/* Patient & Care Plan Context Banner */}
      {selectedPatient && (
        <div style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-lg)',
          padding: '18px 24px',
          marginBottom: '24px',
          boxShadow: 'var(--shadow-sm)'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
              <div style={{
                background: 'linear-gradient(135deg, var(--accent-light) 0%, rgba(14, 165, 233, 0.15) 100%)',
                color: 'var(--accent-primary)',
                width: '48px',
                height: '48px',
                borderRadius: '12px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <Heart size={24} />
              </div>
              <div>
                <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                  Target Patient Record
                </div>
                <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                  {patientDisplayName}
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '20px', flexWrap: 'wrap' }}>
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Patient ID</div>
                <div style={{ fontSize: '0.95rem', fontWeight: 700, fontFamily: 'monospace', color: 'var(--text-primary)' }}>
                  {patientIdVal}
                </div>
              </div>

              <div style={{ borderLeft: '1px solid var(--border-color)', paddingLeft: '20px' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Care Plan ID</div>
                <div style={{ fontSize: '0.95rem', fontWeight: 700, fontFamily: 'monospace', color: 'var(--accent-primary)' }}>
                  {carePlanIdVal}
                </div>
              </div>

              {carePlan && (
                <div style={{ borderLeft: '1px solid var(--border-color)', paddingLeft: '20px' }}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Plan Status</div>
                  <span style={{
                    display: 'inline-block',
                    padding: '3px 10px',
                    borderRadius: '999px',
                    fontSize: '0.78rem',
                    fontWeight: 700,
                    background: carePlan.status === 'ACTIVE' ? '#d1fae5' : '#e0f2fe',
                    color: carePlan.status === 'ACTIVE' ? '#059669' : '#0284c7'
                  }}>
                    {carePlan.status || 'ACTIVE'}
                  </span>
                </div>
              )}

              {carePlan && trackingList.length === 0 && (
                <button
                  className="btn-primary"
                  onClick={handleSyncCarePlan}
                  disabled={syncingCarePlan}
                  style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem', padding: '6px 14px' }}
                >
                  <Sparkles size={14} className={syncingCarePlan ? 'spin-icon' : ''} />
                  {syncingCarePlan ? 'Initializing...' : 'Initialize from Care Plan'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Adherence Summary & Analytics */}
      <div style={{
        background: 'var(--bg-surface)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-lg)',
        padding: '24px',
        marginBottom: '24px',
        boxShadow: 'var(--shadow-sm)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px', flexWrap: 'wrap', gap: '10px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <TrendingUp size={20} style={{ color: 'var(--accent-primary)' }} />
            <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              Treatment Adherence Analytics
            </h3>
          </div>
          <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
            Formula: <code>completed due activities / total due activities * 100</code>
          </div>
        </div>

        {/* 4-Card Summary Grid */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
          gap: '16px',
          marginBottom: '20px'
        }}>
          {/* Adherence Card */}
          <div style={{
            background: 'var(--bg-primary)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-md)',
            padding: '16px 20px',
            position: 'relative',
            overflow: 'hidden'
          }}>
            <div style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              height: '3px',
              backgroundColor: getProgressBarColor(pctValue)
            }} />
            <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Adherence
            </div>
            <div style={{
              fontSize: '1.9rem',
              fontWeight: 800,
              marginTop: '4px',
              color: pctValue != null ? getProgressBarColor(pctValue) : 'var(--text-muted)'
            }}>
              {displayAdherence}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '4px' }}>
              {pctValue != null ? `${completedCount} of ${totalDueCount} due activities completed` : 'No scheduled due activities'}
            </div>
          </div>

          {/* Completed Card */}
          <div style={{
            background: 'var(--bg-primary)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-md)',
            padding: '16px 20px',
            position: 'relative'
          }}>
            <div style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              height: '3px',
              backgroundColor: '#10b981'
            }} />
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                Completed
              </span>
              <CheckCircle2 size={16} style={{ color: '#10b981' }} />
            </div>
            <div style={{ fontSize: '1.9rem', fontWeight: 800, marginTop: '4px', color: '#10b981' }}>
              {completedCount}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '4px' }}>
              Executed interventions
            </div>
          </div>

          {/* Pending Card */}
          <div style={{
            background: 'var(--bg-primary)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-md)',
            padding: '16px 20px',
            position: 'relative'
          }}>
            <div style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              height: '3px',
              backgroundColor: '#f59e0b'
            }} />
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                Pending
              </span>
              <Clock size={16} style={{ color: '#f59e0b' }} />
            </div>
            <div style={{ fontSize: '1.9rem', fontWeight: 800, marginTop: '4px', color: '#f59e0b' }}>
              {pendingCount}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '4px' }}>
              Awaiting execution
            </div>
          </div>

          {/* Missed Card */}
          <div style={{
            background: 'var(--bg-primary)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-md)',
            padding: '16px 20px',
            position: 'relative'
          }}>
            <div style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              height: '3px',
              backgroundColor: '#ef4444'
            }} />
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                Missed
              </span>
              <AlertCircle size={16} style={{ color: '#ef4444' }} />
            </div>
            <div style={{ fontSize: '1.9rem', fontWeight: 800, marginTop: '4px', color: '#ef4444' }}>
              {missedCount}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '4px' }}>
              Requires provider follow-up
            </div>
          </div>
        </div>

        {/* Progress Bar for Adherence */}
        <div style={{ marginTop: '8px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
              Adherence Progress
            </span>
            <span style={{ fontSize: '0.85rem', fontWeight: 700, color: getProgressBarColor(pctValue) }}>
              {pctValue != null ? `${pctValue}% Completed` : 'No Data Available'}
            </span>
          </div>
          <div style={{
            width: '100%',
            height: '10px',
            backgroundColor: 'var(--bg-primary)',
            borderRadius: '999px',
            overflow: 'hidden',
            border: '1px solid var(--border-color)'
          }}>
            <div style={{
              width: pctValue != null ? `${Math.min(pctValue, 100)}%` : '0%',
              height: '100%',
              backgroundColor: getProgressBarColor(pctValue),
              borderRadius: '999px',
              transition: 'width 0.4s ease'
            }} />
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '6px', fontSize: '0.72rem', color: 'var(--text-muted)' }}>
            <span>0%</span>
            <span>Target Benchmark: 80%</span>
            <span>100%</span>
          </div>
        </div>
      </div>

      {/* Interventions Section */}
      <div style={{
        background: 'var(--bg-surface)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-lg)',
        boxShadow: 'var(--shadow-sm)',
        overflow: 'hidden',
        marginBottom: '24px'
      }}>
        {/* Table Toolbar */}
        <div style={{
          padding: '18px 24px',
          borderBottom: '1px solid var(--border-color)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '12px'
        }}>
          <div>
            <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              Care Plan Interventions ({filteredInterventions.length})
            </h3>
            <span style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              Real-time intervention compliance tracking for {patientDisplayName}
            </span>
          </div>

          {/* Status Filter Tabs */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
            {['ALL', 'PENDING', 'IN_PROGRESS', 'COMPLETED', 'MISSED', 'CANCELLED'].map((st) => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                style={{
                  padding: '6px 12px',
                  borderRadius: 'var(--radius-md)',
                  border: statusFilter === st ? '1px solid var(--accent-primary)' : '1px solid var(--border-color)',
                  background: statusFilter === st ? 'var(--accent-light)' : 'var(--bg-primary)',
                  color: statusFilter === st ? 'var(--accent-primary)' : 'var(--text-secondary)',
                  fontSize: '0.8rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                  transition: 'all 0.15s ease'
                }}
              >
                {st === 'ALL' ? 'All' : STATUS_CONFIG[st]?.label || st}
              </button>
            ))}
          </div>
        </div>

        {/* Interventions Table */}
        {loadingTracking ? (
          <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
            <RefreshCw size={24} className="spin-icon" style={{ margin: '0 auto 10px', color: 'var(--accent-primary)' }} />
            <p style={{ margin: 0 }}>Loading treatment tracking records...</p>
          </div>
        ) : filteredInterventions.length === 0 ? (
          <div style={{ padding: '48px 24px', textAlign: 'center' }}>
            <ClipboardList size={40} style={{ opacity: 0.3, margin: '0 auto 12px', color: 'var(--accent-primary)' }} />
            <h4 style={{ margin: '0 0 6px', color: 'var(--text-primary)' }}>No Interventions Found</h4>
            <p style={{ margin: '0 0 16px', color: 'var(--text-muted)', fontSize: '0.9rem' }}>
              {statusFilter !== 'ALL'
                ? `No interventions with status "${statusFilter}". Try selecting "All".`
                : carePlan
                  ? 'No tracking records generated yet for this Care Plan. Click below to initialize.'
                  : 'No active Care Plan found for this patient. Generate a Care Plan first or add custom interventions.'}
            </p>
            {carePlan && (
              <button
                className="btn-primary"
                onClick={handleSyncCarePlan}
                disabled={syncingCarePlan}
                style={{ marginInline: 'auto' }}
              >
                <Sparkles size={16} />
                Initialize Interventions from Care Plan
              </button>
            )}
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
              <thead>
                <tr style={{ background: 'var(--bg-primary)', borderBottom: '1px solid var(--border-color)' }}>
                  <th style={{ padding: '12px 20px', fontWeight: 600, color: 'var(--text-muted)', fontSize: '0.8rem', textTransform: 'uppercase' }}>
                    Activity
                  </th>
                  <th style={{ padding: '12px 16px', fontWeight: 600, color: 'var(--text-muted)', fontSize: '0.8rem', textTransform: 'uppercase' }}>
                    Frequency
                  </th>
                  <th style={{ padding: '12px 16px', fontWeight: 600, color: 'var(--text-muted)', fontSize: '0.8rem', textTransform: 'uppercase' }}>
                    Due Date
                  </th>
                  <th style={{ padding: '12px 16px', fontWeight: 600, color: 'var(--text-muted)', fontSize: '0.8rem', textTransform: 'uppercase' }}>
                    Status
                  </th>
                  <th style={{ padding: '12px 16px', fontWeight: 600, color: 'var(--text-muted)', fontSize: '0.8rem', textTransform: 'uppercase' }}>
                    Notes
                  </th>
                  <th style={{ padding: '12px 20px', fontWeight: 600, color: 'var(--text-muted)', fontSize: '0.8rem', textTransform: 'uppercase', textAlign: 'right' }}>
                    Action
                  </th>
                </tr>
              </thead>
              <tbody>
                {filteredInterventions.map((item) => {
                  const statusConf = STATUS_CONFIG[item.status] || STATUS_CONFIG.PENDING;
                  const typeConf = TYPE_CONFIG[item.interventionType] || TYPE_CONFIG.GENERAL;
                  const StatusIcon = statusConf.icon;
                  const isUpdating = statusUpdatingId === item.id;
                  const isEditingNotes = editingNotesId === item.id;

                  return (
                    <tr
                      key={item.id}
                      style={{
                        borderBottom: '1px solid var(--border-color)',
                        transition: 'background-color 0.15s ease'
                      }}
                    >
                      {/* Activity */}
                      <td style={{ padding: '16px 20px', verticalAlign: 'middle' }}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                          <span style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.95rem' }}>
                            {item.description}
                          </span>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <span style={{
                              display: 'inline-block',
                              padding: '2px 8px',
                              borderRadius: '999px',
                              fontSize: '0.72rem',
                              fontWeight: 700,
                              background: typeConf.bg,
                              color: typeConf.color
                            }}>
                              {typeConf.label}
                            </span>
                            {item.completedAt && (
                              <span style={{ fontSize: '0.74rem', color: 'var(--text-muted)' }}>
                                Completed at {new Date(item.completedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                              </span>
                            )}
                          </div>
                        </div>
                      </td>

                      {/* Frequency */}
                      <td style={{ padding: '16px 16px', color: 'var(--text-secondary)', verticalAlign: 'middle' }}>
                        {item.frequency || 'Daily'}
                      </td>

                      {/* Due Date */}
                      <td style={{ padding: '16px 16px', color: 'var(--text-secondary)', verticalAlign: 'middle' }}>
                        <span style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '4px',
                          background: 'var(--bg-primary)',
                          padding: '3px 8px',
                          borderRadius: 'var(--radius-sm)',
                          fontSize: '0.82rem',
                          border: '1px solid var(--border-color)'
                        }}>
                          <Calendar size={13} style={{ color: 'var(--text-muted)' }} />
                          {item.dueDate || 'Today'}
                        </span>
                      </td>

                      {/* Status */}
                      <td style={{ padding: '16px 16px', verticalAlign: 'middle' }}>
                        <span style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '6px',
                          padding: '4px 10px',
                          borderRadius: '999px',
                          fontSize: '0.8rem',
                          fontWeight: 700,
                          backgroundColor: statusConf.bg,
                          color: statusConf.color,
                          border: `1px solid ${statusConf.border}`
                        }}>
                          <StatusIcon size={14} />
                          {statusConf.label}
                        </span>
                      </td>

                      {/* Notes */}
                      <td style={{ padding: '16px 16px', verticalAlign: 'middle', maxWidth: '200px' }}>
                        {isEditingNotes ? (
                          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <input
                              type="text"
                              value={notesInput}
                              onChange={(e) => setNotesInput(e.target.value)}
                              placeholder="Add clinical note..."
                              style={{
                                padding: '4px 8px',
                                fontSize: '0.82rem',
                                borderRadius: 'var(--radius-sm)',
                                border: '1px solid var(--accent-primary)',
                                background: 'var(--bg-primary)',
                                color: 'var(--text-primary)',
                                width: '130px'
                              }}
                            />
                            <button
                              className="btn-primary"
                              onClick={() => handleSaveNotes(item)}
                              style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                            >
                              Save
                            </button>
                            <button
                              className="btn-secondary"
                              onClick={() => setEditingNotesId(null)}
                              style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                            >
                              ✕
                            </button>
                          </div>
                        ) : (
                          <div
                            onClick={() => {
                              setEditingNotesId(item.id);
                              setNotesInput(item.notes || '');
                            }}
                            title="Click to edit notes"
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: '6px',
                              cursor: 'pointer',
                              color: item.notes ? 'var(--text-secondary)' : 'var(--text-muted)',
                              fontSize: '0.82rem',
                              fontStyle: item.notes ? 'normal' : 'italic'
                            }}
                          >
                            <span>{item.notes || 'Add note...'}</span>
                            <Edit3 size={12} style={{ opacity: 0.5 }} />
                          </div>
                        )}
                      </td>

                      {/* Action */}
                      <td style={{ padding: '16px 20px', verticalAlign: 'middle', textAlign: 'right' }}>
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '8px' }}>
                          {/* Fast Action Buttons as requested in prompt */}
                          {item.status === 'PENDING' && (
                            <button
                              onClick={() => handleStatusChange(item, 'COMPLETED')}
                              disabled={isUpdating}
                              style={{
                                padding: '5px 12px',
                                borderRadius: 'var(--radius-md)',
                                border: '1px solid #34d399',
                                background: '#d1fae5',
                                color: '#059669',
                                fontSize: '0.8rem',
                                fontWeight: 700,
                                cursor: 'pointer',
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '4px'
                              }}
                            >
                              <Check size={14} />
                              Mark Completed
                            </button>
                          )}

                          {item.status === 'COMPLETED' && (
                            <button
                              onClick={() => handleStatusChange(item, 'MISSED')}
                              disabled={isUpdating}
                              style={{
                                padding: '5px 12px',
                                borderRadius: 'var(--radius-md)',
                                border: '1px solid #fca5a5',
                                background: '#fee2e2',
                                color: '#dc2626',
                                fontSize: '0.8rem',
                                fontWeight: 700,
                                cursor: 'pointer',
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '4px'
                              }}
                            >
                              <AlertCircle size={14} />
                              Mark Missed
                            </button>
                          )}

                          {item.status === 'MISSED' && (
                            <button
                              onClick={() => handleStatusChange(item, 'COMPLETED')}
                              disabled={isUpdating}
                              style={{
                                padding: '5px 12px',
                                borderRadius: 'var(--radius-md)',
                                border: '1px solid #34d399',
                                background: '#d1fae5',
                                color: '#059669',
                                fontSize: '0.8rem',
                                fontWeight: 700,
                                cursor: 'pointer',
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '4px'
                              }}
                            >
                              <Check size={14} />
                              Mark Completed
                            </button>
                          )}

                          {item.status === 'IN_PROGRESS' && (
                            <button
                              onClick={() => handleStatusChange(item, 'COMPLETED')}
                              disabled={isUpdating}
                              style={{
                                padding: '5px 12px',
                                borderRadius: 'var(--radius-md)',
                                border: '1px solid #34d399',
                                background: '#d1fae5',
                                color: '#059669',
                                fontSize: '0.8rem',
                                fontWeight: 700,
                                cursor: 'pointer',
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '4px'
                              }}
                            >
                              <Check size={14} />
                              Mark Completed
                            </button>
                          )}

                          {/* Full Status Selector Dropdown */}
                          <select
                            value={item.status || 'PENDING'}
                            onChange={(e) => handleStatusChange(item, e.target.value)}
                            disabled={isUpdating}
                            style={{
                              padding: '5px 8px',
                              borderRadius: 'var(--radius-md)',
                              border: '1px solid var(--border-color)',
                              background: 'var(--bg-primary)',
                              color: 'var(--text-primary)',
                              fontSize: '0.78rem',
                              fontWeight: 600,
                              cursor: 'pointer'
                            }}
                          >
                            <option value="PENDING">Pending</option>
                            <option value="IN_PROGRESS">In Progress</option>
                            <option value="COMPLETED">Completed</option>
                            <option value="MISSED">Missed</option>
                            <option value="CANCELLED">Cancelled</option>
                          </select>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Recent Treatment Activity / History */}
      <div style={{
        background: 'var(--bg-surface)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-lg)',
        padding: '24px',
        marginBottom: '24px',
        boxShadow: 'var(--shadow-sm)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <History size={20} style={{ color: 'var(--accent-primary)' }} />
            <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              Recent Treatment Activity History
            </h3>
          </div>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            Audit log of clinical status updates
          </span>
        </div>

        {activityHistory.length === 0 ? (
          <div style={{
            padding: '24px',
            textAlign: 'center',
            background: 'var(--bg-primary)',
            borderRadius: 'var(--radius-md)',
            border: '1px dashed var(--border-color)',
            color: 'var(--text-muted)',
            fontSize: '0.88rem'
          }}>
            No status changes logged in this session yet. Changing an intervention's status will record events here.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {activityHistory.map((item) => (
              <div
                key={item.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '12px 16px',
                  background: 'var(--bg-primary)',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '0.88rem'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <span style={{
                    width: '8px',
                    height: '8px',
                    borderRadius: '50%',
                    backgroundColor: STATUS_CONFIG[item.newStatus]?.color || '#0ea5e9'
                  }} />
                  <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                    {item.activity}
                  </span>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                    changed from <code style={{ color: 'var(--text-secondary)' }}>{item.oldStatus}</code> to{' '}
                    <strong style={{ color: STATUS_CONFIG[item.newStatus]?.color }}>{item.newStatus}</strong>
                  </span>
                </div>
                <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                  {item.timestamp}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Add Custom Intervention Modal */}
      {showAddModal && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 9999,
          padding: '20px'
        }}>
          <div style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg)',
            width: '100%',
            maxWidth: '520px',
            boxShadow: 'var(--shadow-lg)',
            overflow: 'hidden'
          }}>
            <div style={{
              padding: '20px 24px',
              borderBottom: '1px solid var(--border-color)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between'
            }}>
              <h3 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                Add Custom Intervention
              </h3>
              <button
                onClick={() => setShowAddModal(false)}
                style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '1.2rem' }}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleAddIntervention} style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                  Activity / Description *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g., Morning Blood Glucose Check"
                  value={newIntervention.description}
                  onChange={(e) => setNewIntervention(prev => ({ ...prev, description: e.target.value }))}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--border-color)',
                    background: 'var(--bg-primary)',
                    color: 'var(--text-primary)',
                    fontSize: '0.9rem'
                  }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                    Intervention Type
                  </label>
                  <select
                    value={newIntervention.interventionType}
                    onChange={(e) => setNewIntervention(prev => ({ ...prev, interventionType: e.target.value }))}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      borderRadius: 'var(--radius-md)',
                      border: '1px solid var(--border-color)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-primary)',
                      fontSize: '0.9rem'
                    }}
                  >
                    <option value="VITAL_MONITORING">Vitals Monitoring</option>
                    <option value="GLUCOSE_MONITORING">Glucose Check</option>
                    <option value="MEDICATION">Medication</option>
                    <option value="DIET">Diet / Nutrition</option>
                    <option value="EXERCISE">Physical Exercise</option>
                    <option value="LIFESTYLE">Lifestyle Modification</option>
                    <option value="GENERAL">General Care</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                    Frequency
                  </label>
                  <select
                    value={newIntervention.frequency}
                    onChange={(e) => setNewIntervention(prev => ({ ...prev, frequency: e.target.value }))}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      borderRadius: 'var(--radius-md)',
                      border: '1px solid var(--border-color)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-primary)',
                      fontSize: '0.9rem'
                    }}
                  >
                    <option value="Daily">Daily</option>
                    <option value="Twice Daily">Twice Daily</option>
                    <option value="Weekly">Weekly</option>
                    <option value="As Directed">As Directed</option>
                  </select>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                    Due Date
                  </label>
                  <input
                    type="text"
                    value={newIntervention.dueDate}
                    onChange={(e) => setNewIntervention(prev => ({ ...prev, dueDate: e.target.value }))}
                    placeholder="Today"
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      borderRadius: 'var(--radius-md)',
                      border: '1px solid var(--border-color)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-primary)',
                      fontSize: '0.9rem'
                    }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                    Initial Status
                  </label>
                  <select
                    value={newIntervention.status}
                    onChange={(e) => setNewIntervention(prev => ({ ...prev, status: e.target.value }))}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      borderRadius: 'var(--radius-md)',
                      border: '1px solid var(--border-color)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-primary)',
                      fontSize: '0.9rem'
                    }}
                  >
                    <option value="PENDING">Pending</option>
                    <option value="IN_PROGRESS">In Progress</option>
                    <option value="COMPLETED">Completed</option>
                  </select>
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                  Clinical Notes (Optional)
                </label>
                <textarea
                  rows={2}
                  value={newIntervention.notes}
                  onChange={(e) => setNewIntervention(prev => ({ ...prev, notes: e.target.value }))}
                  placeholder="Target values, clinical instructions or remarks..."
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--border-color)',
                    background: 'var(--bg-primary)',
                    color: 'var(--text-primary)',
                    fontSize: '0.9rem',
                    resize: 'vertical'
                  }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setShowAddModal(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                >
                  Create Intervention
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
