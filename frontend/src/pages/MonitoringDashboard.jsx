import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  Heart, Droplets, Activity, Thermometer,
  AlertTriangle, CheckCircle, Clock, Wifi, WifiOff,
  User, ChevronDown, RefreshCw
} from 'lucide-react';
import { getActiveAlerts, acknowledgeAlert, getLatestVitals } from '../services/api';
import { connectAlertWebSocket } from '../services/websocket';

// Patients available for monitoring
const PATIENTS = [
  { id: 'P001', name: 'Sarah M.' },
  { id: 'P002', name: 'John D.' },
];

// Normal ranges for vital classification
const VITAL_RANGES = {
  'Heart Rate': { min: 60, max: 100, unit: 'BPM' },
  'SpO2': { min: 95, max: 100, unit: '%' },
  'Temperature': { min: 36.1, max: 37.5, unit: '°C' },
  'Blood Pressure': { systolicMax: 140, diastolicMax: 90, unit: 'mmHg' },
};

function isVitalNormal(type, value, systolic, diastolic) {
  const range = VITAL_RANGES[type];
  if (!range) return true;
  if (type === 'Blood Pressure') {
    return (systolic || 0) <= range.systolicMax && (diastolic || 0) <= range.diastolicMax;
  }
  const num = typeof value === 'string' ? parseFloat(value) : value;
  if (isNaN(num)) return true;
  return num >= range.min && num <= range.max;
}

function formatTime(timestamp) {
  if (!timestamp) return '—';
  try {
    const d = new Date(timestamp);
    if (isNaN(d.getTime())) return '—';
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  } catch {
    return '—';
  }
}

function formatDateTime(timestamp) {
  if (!timestamp) return '—';
  try {
    const d = new Date(timestamp);
    if (isNaN(d.getTime())) return '—';
    return d.toLocaleString([], {
      month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit', second: '2-digit'
    });
  } catch {
    return '—';
  }
}

// ─── Vital Card Component ───
function VitalCard({ type, value, unit, systolic, diastolic, icon: Icon, color, isNormal, lastUpdated }) {
  const displayValue = type === 'Blood Pressure'
    ? `${systolic ?? '—'}/${diastolic ?? '—'}`
    : (value ?? '—');

  return (
    <div className={`monitoring-vital-card ${!isNormal ? 'vital-abnormal' : ''}`}>
      <div className="vital-card-header">
        <div className={`vital-card-icon ${color}`}>
          <Icon size={20} />
        </div>
        <span className={`vital-status-badge ${isNormal ? 'status-normal' : 'status-abnormal'}`}>
          {isNormal ? 'Normal' : 'Abnormal'}
        </span>
      </div>
      <div className="vital-card-body">
        <div className="vital-card-type">{type}</div>
        <div className="vital-card-value">
          {type === 'Heart Rate' && !isNaN(parseFloat(value)) && (
            <span className="heartbeat-dot" />
          )}
          <span className="vital-number">{displayValue}</span>
          <span className="vital-unit">{unit || VITAL_RANGES[type]?.unit || ''}</span>
        </div>
      </div>
      <div className="vital-card-footer">
        <Clock size={12} />
        <span>{lastUpdated ? formatTime(lastUpdated) : 'Awaiting data'}</span>
      </div>
    </div>
  );
}

// ─── Simple Trend Chart (SVG) ───
function TrendChart({ data, vitalType }) {
  if (!data || data.length === 0) {
    return (
      <div className="trend-chart-empty">
        <Activity size={32} />
        <p>No trend data available yet</p>
        <p className="trend-chart-hint">Data will appear as the wearable sends readings</p>
      </div>
    );
  }

  const width = 700;
  const height = 200;
  const padding = { top: 20, right: 20, bottom: 30, left: 50 };
  const chartW = width - padding.left - padding.right;
  const chartH = height - padding.top - padding.bottom;

  // Extract values
  const values = data.map(d => {
    if (vitalType === 'Blood Pressure') {
      return d.systolic ?? d.systolicBP ?? 0;
    }
    const v = parseFloat(d.value);
    return isNaN(v) ? 0 : v;
  });

  const minVal = Math.min(...values) - 2;
  const maxVal = Math.max(...values) + 2;
  const range = maxVal - minVal || 1;

  // Build points
  const points = values.map((v, i) => {
    const x = padding.left + (i / Math.max(data.length - 1, 1)) * chartW;
    const y = padding.top + chartH - ((v - minVal) / range) * chartH;
    return { x, y, v };
  });

  const pathD = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ');

  // Area fill path
  const areaD = pathD
    + ` L ${points[points.length - 1].x} ${padding.top + chartH}`
    + ` L ${points[0].x} ${padding.top + chartH} Z`;

  // Y-axis labels (5 ticks)
  const yTicks = Array.from({ length: 5 }, (_, i) => {
    const val = minVal + (range * i / 4);
    const y = padding.top + chartH - (i / 4) * chartH;
    return { val: Math.round(val * 10) / 10, y };
  });

  // Normal range line
  const normalRange = VITAL_RANGES[vitalType];

  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="trend-chart-svg" preserveAspectRatio="xMidYMid meet">
      {/* Grid lines */}
      {yTicks.map((t, i) => (
        <g key={i}>
          <line x1={padding.left} y1={t.y} x2={width - padding.right} y2={t.y}
            stroke="var(--border-color)" strokeWidth="0.5" strokeDasharray="4 4" />
          <text x={padding.left - 8} y={t.y + 4} textAnchor="end"
            fill="var(--text-muted)" fontSize="11">{t.val}</text>
        </g>
      ))}

      {/* Normal range band */}
      {normalRange && vitalType !== 'Blood Pressure' && (() => {
        const yMax = padding.top + chartH - ((normalRange.max - minVal) / range) * chartH;
        const yMin = padding.top + chartH - ((normalRange.min - minVal) / range) * chartH;
        return (
          <rect x={padding.left} y={Math.max(yMax, padding.top)}
            width={chartW} height={Math.min(yMin - yMax, chartH)}
            fill="rgba(34, 197, 94, 0.08)" rx="2" />
        );
      })()}

      {/* Area */}
      <path d={areaD} fill="url(#trendGradient)" opacity="0.3" />

      {/* Line */}
      <path d={pathD} fill="none" stroke="var(--accent-primary)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />

      {/* Data points */}
      {points.map((p, i) => (
        <circle key={i} cx={p.x} cy={p.y} r="3"
          fill="var(--bg-surface)" stroke="var(--accent-primary)" strokeWidth="1.5" />
      ))}

      {/* Gradient def */}
      <defs>
        <linearGradient id="trendGradient" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="var(--accent-primary)" stopOpacity="0.4" />
          <stop offset="100%" stopColor="var(--accent-primary)" stopOpacity="0.02" />
        </linearGradient>
      </defs>
    </svg>
  );
}

// ─── Main Dashboard Component ───
export default function MonitoringDashboard() {
  const [selectedPatient, setSelectedPatient] = useState(PATIENTS[0]);
  const [vitals, setVitals] = useState({});
  const [alerts, setAlerts] = useState([]);
  const [trendType, setTrendType] = useState('Heart Rate');
  const [trendData, setTrendData] = useState([]);
  const [wsConnected, setWsConnected] = useState(false);
  const [lastDataTime, setLastDataTime] = useState(null);
  const [loadingAlerts, setLoadingAlerts] = useState(true);
  const [acknowledgingId, setAcknowledgingId] = useState(null);
  const [dropdownOpen, setDropdownOpen] = useState(false);

  const wsRef = useRef(null);
  const pollIntervalRef = useRef(null);

  // ─── Fetch latest vitals for the selected patient ───
  const fetchVitals = useCallback(async () => {
    try {
      const records = await getLatestVitals(selectedPatient.id);

      if (!Array.isArray(records) || records.length === 0) return;

      // Group by type and explicitly select the newest record
      const latestByType = {};
      const trendRecords = [];

      const getRecordTimestamp = (r) => {
        if (!r) return -Infinity;
        const raw = r.createdAt || r.recordedAt;
        if (!raw) return -Infinity;
        const t = new Date(raw).getTime();
        return isNaN(t) ? -Infinity : t;
      };

      for (const rec of records) {
        const type = rec.type;
        if (!type) continue;

        const recTimestamp = getRecordTimestamp(rec);
        const existingTimestamp = getRecordTimestamp(latestByType[type]);

        // Keep the newest record for each vital type (or first available)
        if (!latestByType[type] || recTimestamp > existingTimestamp) {
          latestByType[type] = rec;
        }

        // Collect records for trend
        if (type === trendType) {
          trendRecords.push(rec);
        }
      }

      setVitals(latestByType);

      // Sort trend data from oldest → newest
      trendRecords.sort((a, b) => {
        const timeA = getRecordTimestamp(a);
        const timeB = getRecordTimestamp(b);
        return timeA - timeB;
      });

      setTrendData(trendRecords);

      // Find the newest record across all vital types
      const newestRecord = records.reduce((latest, rec) => {
        const recTime = getRecordTimestamp(rec);
        const latestTime = getRecordTimestamp(latest);
        return recTime > latestTime ? rec : latest;
      }, null);

      if (newestRecord) {
        setLastDataTime(
          newestRecord.createdAt ||
          newestRecord.recordedAt
        );
      }

    } catch (err) {
      console.error('Failed to fetch vitals:', err);
    }
  }, [selectedPatient.id, trendType]);
  // ─── Fetch active alerts ───
  const fetchAlerts = useCallback(async () => {
    try {
      setLoadingAlerts(true);
      const data = await getActiveAlerts();
      if (Array.isArray(data)) {
        setAlerts(data);
      }
    } catch (err) {
      console.error('Failed to fetch alerts:', err);
    } finally {
      setLoadingAlerts(false);
    }
  }, []);

  // ─── Handle incoming WebSocket alert ───
  const handleAlertMessage = useCallback((alert) => {
    if (!alert || !alert.id) return;

    setAlerts(prev => {
      const existing = prev.findIndex(a => a.id === alert.id);
      if (existing >= 0) {
        // Update existing alert (e.g., status changed to ACKNOWLEDGED)
        const updated = [...prev];
        updated[existing] = alert;
        return updated;
      }
      // New alert — prepend
      return [alert, ...prev];
    });
  }, []);

  // ─── WebSocket connection ───
  useEffect(() => {
    const ws = connectAlertWebSocket(
      handleAlertMessage,
      () => setWsConnected(true),
      () => setWsConnected(false)
    );
    wsRef.current = ws;

    return () => {
      if (ws) {
        ws.deactivate();
      }
    };
  }, [handleAlertMessage]);

  // ─── Initial data load + polling ───
  useEffect(() => {
    fetchAlerts();
    fetchVitals();

    // Poll vitals every 6 seconds (wearable sends every 5s)
    pollIntervalRef.current = setInterval(fetchVitals, 6000);

    return () => {
      if (pollIntervalRef.current) {
        clearInterval(pollIntervalRef.current);
      }
    };
  }, [fetchAlerts, fetchVitals]);

  // ─── Handle acknowledge ───
  const handleAcknowledge = async (alertId) => {
    try {
      setAcknowledgingId(alertId);
      await acknowledgeAlert(alertId);
      // The backend WebSocket broadcast will also update via handleAlertMessage,
      // but we optimistically update here for instant UI feedback.
      setAlerts(prev => prev.map(a =>
        a.id === alertId ? { ...a, status: 'ACKNOWLEDGED' } : a
      ));
    } catch (err) {
      console.error('Failed to acknowledge alert:', err);
    } finally {
      setAcknowledgingId(null);
    }
  };

  // ─── Vital card data helpers ───
  const hr = vitals['Heart Rate'];
  const spo2 = vitals['SpO2'];
  const bp = vitals['Blood Pressure'];
  const temp = vitals['Temperature'];

  const getVal = (rec) => rec?.value ? parseFloat(rec.value) : null;
  const getTime = (rec) => rec?.createdAt || rec?.recordedAt || null;

  // Determine wearable status
  const hasData = lastDataTime !== null;
  const isRecent = hasData && (Date.now() - new Date(lastDataTime).getTime() < 30000);

  // Severity styling
  const severityClass = (severity) => {
    switch (severity) {
      case 'CRITICAL': return 'severity-critical';
      case 'HIGH': return 'severity-high';
      case 'WARNING': return 'severity-warning';
      default: return '';
    }
  };

  return (
    <div className="monitoring-dashboard">
      {/* Header */}
      <div className="monitoring-header">
        <div className="monitoring-title-area">
          <Activity size={24} className="monitoring-title-icon" />
          <div>
            <h1 className="monitoring-title">Patient Monitoring</h1>
            <p className="monitoring-subtitle">Real-time vitals, alerts, and wearable status</p>
          </div>
        </div>

        {/* Patient Selector */}
        <div className="patient-selector-wrapper">
          <div className="patient-selector" onClick={() => setDropdownOpen(!dropdownOpen)}>
            <div className="patient-selector-avatar">
              <User size={16} />
            </div>
            <div className="patient-selector-info">
              <span className="patient-selector-name">{selectedPatient.name}</span>
              <span className="patient-selector-id">{selectedPatient.id}</span>
            </div>
            <ChevronDown size={16} className={`selector-chevron ${dropdownOpen ? 'open' : ''}`} />
          </div>
          {dropdownOpen && (
            <div className="patient-dropdown">
              {PATIENTS.map(p => (
                <div
                  key={p.id}
                  className={`patient-dropdown-item ${p.id === selectedPatient.id ? 'selected' : ''}`}
                  onClick={() => {
                    setSelectedPatient(p);
                    setDropdownOpen(false);
                    setVitals({});
                    setTrendData([]);
                    setLastDataTime(null);
                  }}
                >
                  <User size={14} />
                  <span>{p.name}</span>
                  <span className="dropdown-patient-id">{p.id}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Vital Cards */}
      <div className="monitoring-vitals-grid">
        <VitalCard
          type="Heart Rate"
          value={getVal(hr)}
          unit={hr?.unit || 'BPM'}
          icon={Heart}
          color="vital-heart"
          isNormal={hr ? isVitalNormal('Heart Rate', hr.value) : true}
          lastUpdated={getTime(hr)}
        />
        <VitalCard
          type="SpO2"
          value={getVal(spo2)}
          unit={spo2?.unit || '%'}
          icon={Droplets}
          color="vital-spo2"
          isNormal={spo2 ? isVitalNormal('SpO2', spo2.value) : true}
          lastUpdated={getTime(spo2)}
        />
        <VitalCard
          type="Blood Pressure"
          systolic={bp?.systolic ?? bp?.systolicBP}
          diastolic={bp?.diastolic ?? bp?.diastolicBP}
          unit="mmHg"
          icon={Activity}
          color="vital-bp"
          isNormal={bp ? isVitalNormal('Blood Pressure', null, bp.systolic ?? bp.systolicBP, bp.diastolic ?? bp.diastolicBP) : true}
          lastUpdated={getTime(bp)}
        />
        <VitalCard
          type="Temperature"
          value={getVal(temp)}
          unit={temp?.unit || '°C'}
          icon={Thermometer}
          color="vital-temp"
          isNormal={temp ? isVitalNormal('Temperature', temp.value) : true}
          lastUpdated={getTime(temp)}
        />
      </div>

      {/* Trend Graph */}
      <div className="monitoring-section">
        <div className="section-header">
          <h2 className="section-title">Live Vital Trends</h2>
          <div className="trend-type-selector">
            {['Heart Rate', 'SpO2', 'Blood Pressure', 'Temperature'].map(t => (
              <button
                key={t}
                className={`trend-type-btn ${trendType === t ? 'active' : ''}`}
                onClick={() => setTrendType(t)}
              >
                {t === 'Blood Pressure' ? 'BP' : t}
              </button>
            ))}
          </div>
        </div>
        <div className="trend-chart-container">
          <TrendChart data={trendData} vitalType={trendType} />
        </div>
      </div>

      {/* Active Alerts */}
      <div className="monitoring-section monitoring-alerts-section">
        <div className="section-header">
          <h2 className="section-title">
            <AlertTriangle size={20} />
            Active Alerts
            {alerts.filter(a => a.status === 'ACTIVE').length > 0 && (
              <span className="alert-count-badge">
                {alerts.filter(a => a.status === 'ACTIVE').length}
              </span>
            )}
          </h2>
          <button className="refresh-btn" onClick={fetchAlerts} title="Refresh alerts">
            <RefreshCw size={14} />
          </button>
        </div>

        {loadingAlerts ? (
          <div className="alerts-loading">Loading alerts...</div>
        ) : alerts.length === 0 ? (
          <div className="alerts-empty">
            <CheckCircle size={32} />
            <p>No active alerts</p>
            <p className="alerts-empty-hint">Alerts will appear here when anomalies are detected</p>
          </div>
        ) : (
          <div className="alerts-table-wrapper">
            <table className="alerts-table">
              <thead>
                <tr>
                  <th>Severity</th>
                  <th>Patient</th>
                  <th>Vital</th>
                  <th>Value</th>
                  <th>Message</th>
                  <th>Assigned To</th>
                  <th>Notification</th>
                  <th>Time</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {alerts.map(alert => (
                  <tr key={alert.id} className={`alert-row ${severityClass(alert.severity)}`}>
                    <td>
                      <span className={`severity-badge ${severityClass(alert.severity)}`}>
                        {alert.severity}
                      </span>
                    </td>
                    <td className="alert-patient">
                      {alert.patientName || alert.patientId}
                    </td>
                    <td>{alert.vitalType}</td>
                    <td className="alert-value">
                      {alert.value} {alert.unit}
                    </td>
                    <td className="alert-message">{alert.message}</td>
                    <td>{alert.assignedDoctorRole || '—'}</td>
                    <td>{alert.notificationStatus || '—'}</td>
                    <td className="alert-time">{formatDateTime(alert.detectedAt)}</td>
                    <td>
                      <span className={`alert-status-badge ${alert.status === 'ACTIVE' ? 'status-active' : 'status-ack'}`}>
                        {alert.status}
                      </span>
                    </td>
                    <td>
                      {alert.status === 'ACTIVE' ? (
                        <button
                          className="acknowledge-btn"
                          onClick={() => handleAcknowledge(alert.id)}
                          disabled={acknowledgingId === alert.id}
                        >
                          {acknowledgingId === alert.id ? 'Acknowledging...' : 'Acknowledge'}
                        </button>
                      ) : (
                        <span className="acknowledged-label">
                          <CheckCircle size={14} />
                          Done
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Wearable Device Status */}
      <div className="monitoring-section device-status-section">
        <div className="section-header">
          <h2 className="section-title">Wearable Device</h2>
        </div>
        <div className="device-status-content">
          <div className={`device-status-indicator ${isRecent ? 'connected' : 'disconnected'}`}>
            {isRecent ? <Wifi size={20} /> : <WifiOff size={20} />}
            <span className="device-status-label">
              {isRecent ? 'Wearable Connected' : (hasData ? 'Connection Stale' : 'Waiting for wearable data')}
            </span>
          </div>
          {hasData && (
            <div className="device-last-data">
              <Clock size={14} />
              <span>Last data received: {formatDateTime(lastDataTime)}</span>
            </div>
          )}
          <div className={`ws-status ${wsConnected ? 'ws-connected' : 'ws-disconnected'}`}>
            <span className="ws-dot" />
            <span>WebSocket: {wsConnected ? 'Connected' : 'Reconnecting...'}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
