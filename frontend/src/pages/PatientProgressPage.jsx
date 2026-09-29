import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  TrendingUp, TrendingDown, Minus, RefreshCw, AlertCircle,
  User, Stethoscope, ArrowUp, ArrowDown, Activity, AlertTriangle,
  CheckCircle2, Clock
} from 'lucide-react';
import PatientSelector from '../components/PatientSelector';
import {
  getFhirPatients, getAllPatientData, getPatientName,
  getPatientHealthData, getPatientProgress
} from '../services/api';

// ─── Status config ───
const STATUS_CONFIG = {
  IMPROVED: {
    label: 'Improved',
    color: '#059669',
    bg: '#d1fae5',
    border: '#34d399',
    icon: TrendingDown,
    iconStyle: { color: '#059669' }
  },
  WORSENED: {
    label: 'Worsened',
    color: '#dc2626',
    bg: '#fee2e2',
    border: '#fca5a5',
    icon: TrendingUp,
    iconStyle: { color: '#dc2626' }
  },
  STABLE: {
    label: 'Stable',
    color: '#0284c7',
    bg: '#e0f2fe',
    border: '#38bdf8',
    icon: Minus,
    iconStyle: { color: '#0284c7' }
  }
};

const METRIC_COLORS = {
  'Blood Pressure':  { color: '#ef4444', bg: 'rgba(239,68,68,0.08)' },
  'Blood Glucose':   { color: '#f59e0b', bg: 'rgba(245,158,11,0.08)' },
  'HbA1c':           { color: '#8b5cf6', bg: 'rgba(139,92,246,0.08)' },
  'Heart Rate':      { color: '#ec4899', bg: 'rgba(236,72,153,0.08)' },
  'SpO2':            { color: '#0ea5e9', bg: 'rgba(14,165,233,0.08)' },
  'Weight':          { color: '#10b981', bg: 'rgba(16,185,129,0.08)' },
  'Cholesterol':     { color: '#f97316', bg: 'rgba(249,115,22,0.08)' },
  'Hemoglobin':      { color: '#dc2626', bg: 'rgba(220,38,38,0.08)' },
  'Creatinine':      { color: '#6366f1', bg: 'rgba(99,102,241,0.08)' },
  'TSH':             { color: '#14b8a6', bg: 'rgba(20,184,166,0.08)' },
  'Temperature':     { color: '#d97706', bg: 'rgba(217,119,6,0.08)' },
};

function formatTimestamp(ts) {
  if (!ts) return '—';
  try {
    const d = new Date(ts);
    if (isNaN(d.getTime())) return ts;
    return d.toLocaleDateString([], { day: 'numeric', month: 'short', year: 'numeric' });
  } catch {
    return ts;
  }
}

function formatChange(change, isBP) {
  if (change == null || change === undefined) return '—';
  if (isBP) {
    const str = String(change);
    if (!str.includes('/')) return change;
    const parts = str.split('/');
    return parts.map(p => {
      const n = Number(p);
      if (isNaN(n)) return p;
      return n > 0 ? `+${n}` : String(n);
    }).join('/');
  }
  const n = Number(change);
  if (isNaN(n)) return String(change);
  if (n === 0) return '0';
  return n > 0 ? `+${n}` : String(n);
}

function getChangeColor(status) {
  if (status === 'IMPROVED') return '#059669';
  if (status === 'WORSENED') return '#dc2626';
  if (status === 'STABLE') return '#0284c7';
  return 'var(--text-muted)';
}

export default function PatientProgressPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const urlPatientId = searchParams.get('patientId');

  // Patient state
  const [patients, setPatients] = useState([]);
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [loadingPatients, setLoadingPatients] = useState(true);

  // Progress state
  const [progress, setProgress] = useState(null);
  const [loadingProgress, setLoadingProgress] = useState(false);
  const [progressError, setProgressError] = useState(null);

  // Patient display info
  const [patientHealthData, setPatientHealthData] = useState(null);

  // ─── Load patient roster ───
  const loadPatients = useCallback(async () => {
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
      } catch {
        // Fallback to FHIR below
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
      } catch {
        // silent
      }

      const combinedList = [...clinicalList, ...fhirListMapped];
      setPatients(combinedList);

      const savedPid = urlPatientId || localStorage.getItem('medisphere_selected_patient_id');
      if (combinedList.length > 0) {
        let initial = combinedList[0];
        if (savedPid) {
          const match = combinedList.find(p =>
            String(p.id).toLowerCase() === String(savedPid).toLowerCase() ||
            String(p.patientId).toLowerCase() === String(savedPid).toLowerCase() ||
            String(p.sourcePatientId).toLowerCase() === String(savedPid).toLowerCase()
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
    } finally {
      setLoadingPatients(false);
    }
  }, [urlPatientId]);

  useEffect(() => { loadPatients(); }, []);

  // ─── Patient selection handler ───
  const handleSelectPatient = (patient) => {
    setSelectedPatient(patient);
    setProgress(null);
    setProgressError(null);
    if (patient) {
      const pid = patient.sourcePatientId || patient.patientId || patient.id;
      setSearchParams({ patientId: pid });
      localStorage.setItem('medisphere_selected_patient_id', pid);
    }
  };

  // ─── Resolve canonical patient ID ───
  const patientIdVal = selectedPatient
    ? (selectedPatient.sourcePatientId || selectedPatient.patientId || selectedPatient.id)
    : '';

  // ─── Fetch progress from backend ───
  const fetchProgress = useCallback(async (pid) => {
    if (!pid) return;
    setLoadingProgress(true);
    setProgressError(null);
    try {
      const data = await getPatientProgress(pid);
      setProgress(data);
    } catch (err) {
      console.error('Progress fetch error:', err);
      setProgressError('Unable to load progress. Please try again.');
      setProgress(null);
    } finally {
      setLoadingProgress(false);
    }
  }, []);

  // ─── Fetch patient display info ───
  const fetchPatientDetails = useCallback(async (pid) => {
    try {
      const pd = await getPatientHealthData(pid);
      setPatientHealthData(pd);
    } catch {
      setPatientHealthData(null);
    }
  }, []);

  // ─── When patient changes, reload data ───
  useEffect(() => {
    if (selectedPatient) {
      const pid = selectedPatient.sourcePatientId || selectedPatient.patientId || selectedPatient.id;
      setProgress(null);
      setProgressError(null);
      fetchProgress(pid);
      fetchPatientDetails(pid);
    } else {
      setProgress(null);
      setPatientHealthData(null);
    }
  }, [selectedPatient]);

  // ─── Patient display name ───
  const patientDisplayName = patientHealthData?.personalDetails
    ? `${patientHealthData.personalDetails.firstName || ''} ${patientHealthData.personalDetails.lastName || ''}`.trim()
    : (selectedPatient?.name || selectedPatient?.displayName || patientIdVal);

  // ─── Summary counts ───
  const metricsWithComparison = progress?.metrics?.filter(m => m.hasEnoughData) || [];
  const improved = metricsWithComparison.filter(m => m.status === 'IMPROVED').length;
  const stable = metricsWithComparison.filter(m => m.status === 'STABLE').length;
  const worsened = metricsWithComparison.filter(m => m.status === 'WORSENED').length;

  return (
    <div className="dashboard-content">
      {/* Page Header */}
      <div className="page-header-row" style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        flexWrap: 'wrap', gap: '16px', marginBottom: '20px'
      }}>
        <div className="page-title-group">
          <h1 style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <TrendingUp className="text-accent" size={28} />
            Patient Progress
          </h1>
          <p>Milestone 4 Part 4: Baseline vs. Latest Health Measurement Comparison</p>
        </div>

        <button
          className="btn-secondary"
          onClick={() => patientIdVal && fetchProgress(patientIdVal)}
          disabled={loadingProgress || !selectedPatient}
          style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          title="Refresh progress data from backend"
        >
          <RefreshCw size={16} className={loadingProgress ? 'spin-icon' : ''} />
          Refresh
        </button>
      </div>

      {/* Patient Selector */}
      <PatientSelector
        patients={patients}
        selectedPatient={selectedPatient}
        onSelectPatient={handleSelectPatient}
        loading={loadingPatients}
      />

      {selectedPatient && (
        <>
          {/* Patient Info Banner */}
          <div style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg, 12px)',
            padding: '18px 24px',
            marginBottom: '24px',
            boxShadow: 'var(--shadow-sm)',
            display: 'flex',
            alignItems: 'center',
            gap: '14px'
          }}>
            <div style={{
              background: 'linear-gradient(135deg, var(--accent-light) 0%, rgba(139,92,246,0.15) 100%)',
              color: 'var(--accent-primary)',
              padding: '12px',
              borderRadius: '12px',
              display: 'flex'
            }}>
              <Stethoscope size={22} />
            </div>
            <div>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                Patient Progress Report
              </div>
              <div style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                {patientDisplayName}
              </div>
              <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                ID: <code style={{ fontFamily: 'monospace', fontWeight: 600 }}>{patientIdVal}</code>
                {progress?.computedAt && (
                  <span style={{ marginLeft: '12px' }}>
                    <Clock size={11} style={{ verticalAlign: 'middle', marginRight: '3px' }} />
                    Computed: {formatTimestamp(progress.computedAt)}
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* Loading state */}
          {loadingProgress && (
            <div style={{
              padding: '60px 20px',
              textAlign: 'center',
              color: 'var(--text-muted)',
              background: 'var(--bg-surface)',
              borderRadius: 'var(--radius-lg)',
              border: '1px solid var(--border-color)'
            }}>
              <RefreshCw size={28} className="spin-icon" style={{ marginBottom: '12px' }} />
              <p style={{ fontWeight: 600 }}>Loading progress...</p>
              <p style={{ fontSize: '0.85rem', marginTop: '4px' }}>
                Fetching baseline and latest measurements from database
              </p>
            </div>
          )}

          {/* Error state */}
          {!loadingProgress && progressError && (
            <div style={{
              background: '#fee2e2',
              color: '#dc2626',
              border: '1px solid #fca5a5',
              borderRadius: 'var(--radius-md)',
              padding: '16px 20px',
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
              marginBottom: '20px'
            }}>
              <AlertCircle size={20} style={{ flexShrink: 0 }} />
              <span style={{ fontWeight: 500 }}>{progressError}</span>
            </div>
          )}

          {/* No data state */}
          {!loadingProgress && !progressError && progress && !progress.hasHistoricalData && (
            <div style={{
              padding: '60px 20px',
              textAlign: 'center',
              color: 'var(--text-muted)',
              background: 'var(--bg-surface)',
              borderRadius: 'var(--radius-lg)',
              border: '1px solid var(--border-color)'
            }}>
              <Activity size={40} style={{ marginBottom: '12px', opacity: 0.35 }} />
              <p style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                Not enough historical data for comparison.
              </p>
              <p style={{ fontSize: '0.85rem' }}>
                Record at least two health measurements for <strong>{patientDisplayName}</strong> in{' '}
                <strong>Health Monitoring</strong> to enable progress comparison.
              </p>
            </div>
          )}

          {/* Progress Content */}
          {!loadingProgress && !progressError && progress && (progress.hasHistoricalData || progress.hasAnyComparison) && (
            <>
              {/* Follow-up warning */}
              {progress.followUpRequired && (
                <div style={{
                  background: '#fef3c7',
                  color: '#92400e',
                  border: '1px solid #fcd34d',
                  borderRadius: 'var(--radius-md)',
                  padding: '14px 18px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  marginBottom: '20px'
                }}>
                  <AlertTriangle size={18} style={{ flexShrink: 0 }} />
                  <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>
                    {progress.followUpMessage || 'Follow-up may be required.'}
                  </span>
                </div>
              )}

              {/* Summary Cards */}
              {progress.hasAnyComparison && (
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
                  gap: '16px',
                  marginBottom: '24px'
                }}>
                  <SummaryCard
                    label="Improved"
                    count={improved}
                    color="#059669"
                    bg="#d1fae5"
                    icon={<TrendingDown size={20} />}
                  />
                  <SummaryCard
                    label="Stable"
                    count={stable}
                    color="#0284c7"
                    bg="#e0f2fe"
                    icon={<Minus size={20} />}
                  />
                  <SummaryCard
                    label="Worsened"
                    count={worsened}
                    color="#dc2626"
                    bg="#fee2e2"
                    icon={<TrendingUp size={20} />}
                  />
                  <SummaryCard
                    label="Tracked"
                    count={progress.metrics?.length || 0}
                    color="#8b5cf6"
                    bg="#ede9fe"
                    icon={<Activity size={20} />}
                  />
                </div>
              )}

              {/* Metrics Table / Cards */}
              <div style={{
                background: 'var(--bg-surface)',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-lg)',
                padding: '24px',
                boxShadow: 'var(--shadow-sm)'
              }}>
                <h3 style={{
                  margin: '0 0 20px',
                  fontSize: '1.1rem',
                  fontWeight: 700,
                  color: 'var(--text-primary)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px'
                }}>
                  <Activity size={20} style={{ color: 'var(--accent-primary)' }} />
                  Metric-by-Metric Progress
                </h3>

                {/* Desktop table header */}
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: '200px 1fr 1fr 1fr 120px',
                  gap: '16px',
                  padding: '8px 16px',
                  borderBottom: '2px solid var(--border-color)',
                  marginBottom: '8px'
                }} className="progress-table-header">
                  {['Metric', 'Baseline', 'Latest', 'Change', 'Status'].map(col => (
                    <span key={col} style={{
                      fontSize: '0.75rem',
                      fontWeight: 700,
                      textTransform: 'uppercase',
                      letterSpacing: '0.6px',
                      color: 'var(--text-muted)'
                    }}>{col}</span>
                  ))}
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  {(progress.metrics || []).map((metric, idx) => (
                    <MetricRow key={metric.metric + idx} metric={metric} />
                  ))}
                </div>
              </div>
            </>
          )}
        </>
      )}

      {/* No patient selected */}
      {!selectedPatient && !loadingPatients && (
        <div style={{
          padding: '60px 20px',
          textAlign: 'center',
          color: 'var(--text-muted)',
          background: 'var(--bg-surface)',
          borderRadius: 'var(--radius-lg)',
          border: '1px solid var(--border-color)'
        }}>
          <User size={40} style={{ marginBottom: '12px', opacity: 0.4 }} />
          <p style={{ fontWeight: 600 }}>Select a patient to view their progress</p>
        </div>
      )}

      {/* Responsive table styles */}
      <style>{`
        @media (max-width: 768px) {
          .progress-table-header { display: none !important; }
        }
      `}</style>
    </div>
  );
}

// ─── Summary Card ───
function SummaryCard({ label, count, color, bg, icon }) {
  return (
    <div style={{
      background: 'var(--bg-primary)',
      border: '1px solid var(--border-color)',
      borderRadius: 'var(--radius-md)',
      padding: '16px 18px',
      position: 'relative',
      overflow: 'hidden'
    }}>
      <div style={{
        position: 'absolute', top: 0, left: 0, right: 0,
        height: '3px', backgroundColor: color
      }} />
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span style={{
          fontSize: '0.75rem', color: 'var(--text-muted)',
          textTransform: 'uppercase', letterSpacing: '0.5px', fontWeight: 600
        }}>
          {label}
        </span>
        <span style={{ color, opacity: 0.8 }}>{icon}</span>
      </div>
      <div style={{ fontSize: '2.2rem', fontWeight: 800, color, marginTop: '6px' }}>
        {count}
      </div>
    </div>
  );
}

// ─── Metric Row ───
function MetricRow({ metric }) {
  const metricColor = METRIC_COLORS[metric.metric] || { color: '#64748b', bg: 'rgba(100,116,139,0.08)' };
  const status = metric.status;
  const statusCfg = status ? STATUS_CONFIG[status] : null;
  const hasData = metric.hasEnoughData;

  const displayBaseline = metric.isBP
    ? `${metric.baseline ?? '—'}`
    : (metric.baseline != null ? metric.baseline : '—');

  const displayLatest = metric.isBP
    ? `${metric.latest ?? '—'}`
    : (metric.latest != null ? metric.latest : '—');

  const changeStr = hasData ? formatChange(metric.change, metric.isBP) : '—';
  const changeColor = hasData && status ? getChangeColor(status) : 'var(--text-muted)';

  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: '200px 1fr 1fr 1fr 120px',
      gap: '16px',
      padding: '14px 16px',
      background: 'var(--bg-primary)',
      borderRadius: 'var(--radius-md)',
      border: '1px solid var(--border-color)',
      alignItems: 'center',
      transition: 'box-shadow 0.15s'
    }}>
      {/* Metric name */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        <span style={{
          width: '10px', height: '10px',
          borderRadius: '50%',
          background: metricColor.color,
          flexShrink: 0
        }} />
        <div>
          <div style={{ fontWeight: 700, fontSize: '0.9rem', color: 'var(--text-primary)' }}>
            {metric.metric}
          </div>
          <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)' }}>
            {metric.unit}
            {metric.historicalCount > 0 && (
              <span style={{ marginLeft: '6px', opacity: 0.7 }}>
                ({metric.historicalCount} records)
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Baseline */}
      <div>
        {hasData ? (
          <div>
            <span style={{ fontWeight: 600, fontSize: '1rem', color: 'var(--text-primary)' }}>
              {displayBaseline}
            </span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginLeft: '4px' }}>
              {metric.unit}
            </span>
            {metric.baselineRecordedAt && (
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                {formatTimestamp(metric.baselineRecordedAt)}
              </div>
            )}
          </div>
        ) : (
          <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem', fontStyle: 'italic' }}>—</span>
        )}
      </div>

      {/* Latest */}
      <div>
        {hasData ? (
          <div>
            <span style={{ fontWeight: 600, fontSize: '1rem', color: 'var(--text-primary)' }}>
              {displayLatest}
            </span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginLeft: '4px' }}>
              {metric.unit}
            </span>
            {metric.latestRecordedAt && (
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                {formatTimestamp(metric.latestRecordedAt)}
              </div>
            )}
          </div>
        ) : (
          <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem', fontStyle: 'italic' }}>—</span>
        )}
      </div>

      {/* Change */}
      <div>
        {hasData ? (
          <span style={{
            fontWeight: 700,
            fontSize: '0.95rem',
            color: changeColor,
            fontFamily: 'monospace'
          }}>
            {changeStr}
          </span>
        ) : (
          <span style={{
            fontSize: '0.8rem',
            color: 'var(--text-muted)',
            fontStyle: 'italic'
          }}>
            {metric.message || 'Not enough historical data for comparison.'}
          </span>
        )}
      </div>

      {/* Status */}
      <div>
        {hasData && statusCfg ? (
          <span style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '5px',
            padding: '4px 12px',
            borderRadius: '999px',
            fontSize: '0.8rem',
            fontWeight: 700,
            background: statusCfg.bg,
            color: statusCfg.color,
            border: `1px solid ${statusCfg.border}`,
            whiteSpace: 'nowrap'
          }}>
            <statusCfg.icon size={12} />
            {statusCfg.label}
          </span>
        ) : hasData && !status ? (
          <span style={{
            display: 'inline-block',
            padding: '4px 10px',
            borderRadius: '999px',
            fontSize: '0.78rem',
            fontWeight: 600,
            background: 'var(--bg-surface)',
            color: 'var(--text-muted)',
            border: '1px solid var(--border-color)'
          }}>
            Changed
          </span>
        ) : (
          <span style={{
            display: 'inline-block',
            padding: '4px 10px',
            borderRadius: '999px',
            fontSize: '0.78rem',
            fontWeight: 600,
            background: 'var(--bg-surface)',
            color: 'var(--text-muted)',
            border: '1px solid var(--border-color)',
            fontStyle: 'italic'
          }}>
            Insufficient data
          </span>
        )}
      </div>
    </div>
  );
}
