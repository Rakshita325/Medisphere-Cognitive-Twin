import React, { useState, useEffect } from 'react';
import { Cpu, RefreshCw, AlertCircle, CheckCircle, XCircle, Info, Layers, Activity, Heart } from 'lucide-react';
import { getModelMetadata } from '../services/api';

/**
 * Model Information & Versioning Page.
 * Displays real model metadata from Spring Boot /api/ml/models/*
 */
export default function ModelInfoPage() {
  const [cvdMeta, setCvdMeta] = useState(null);
  const [diabetesMeta, setDiabetesMeta] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadMetadata = async () => {
    setLoading(true);
    setError(null);
    try {
      const [cvdResult, diabetesResult] = await Promise.allSettled([
        getModelMetadata('cvd'),
        getModelMetadata('diabetes'),
      ]);

      setCvdMeta(cvdResult.status === 'fulfilled' ? cvdResult.value : null);
      setDiabetesMeta(diabetesResult.status === 'fulfilled' ? diabetesResult.value : null);

      if (cvdResult.status === 'rejected' && diabetesResult.status === 'rejected') {
        setError('Unable to load model metadata. Please ensure the Spring Boot backend and Python ML service are running.');
      }
    } catch (err) {
      console.error('Failed to load model metadata:', err);
      setError('Unable to load model metadata from backend.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMetadata();
  }, []);

  const formatAccuracy = (val) => {
    if (val == null) return 'Not Available';
    return `${(val * 100).toFixed(2)}%`;
  };

  const formatValue = (val) => {
    if (val == null) return 'Not Available';
    return String(val);
  };

  const renderModelCard = (title, icon, meta, featureCount, defaultName, defaultVer) => {
    const isLoaded = meta && meta.status === 'loaded';
    const Icon = icon;

    return (
      <div className="model-info-card" style={{
        background: 'var(--bg-surface)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-lg)',
        padding: '24px',
        boxShadow: 'var(--shadow-sm)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{
              background: 'var(--accent-light)',
              color: 'var(--accent-primary)',
              padding: '10px',
              borderRadius: 'var(--radius-md)',
              display: 'flex'
            }}>
              <Icon size={22} />
            </div>
            <div>
              <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                {title}
              </h3>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Version: {meta?.version || defaultVer || 'Model version information unavailable'}
              </span>
            </div>
          </div>

          <span style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            background: isLoaded ? 'var(--status-success-bg)' : 'var(--bg-primary)',
            color: isLoaded ? 'var(--status-success-text)' : 'var(--text-muted)',
            padding: '4px 10px',
            borderRadius: '999px',
            fontSize: '0.75rem',
            fontWeight: 600
          }}>
            {isLoaded ? <CheckCircle size={14} /> : <XCircle size={14} />}
            {isLoaded ? 'Active' : (meta?.status || 'Unavailable')}
          </span>
        </div>

        {meta ? (
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
            gap: '12px'
          }}>
            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Model Name</div>
              <div style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                {meta.model || defaultName}
              </div>
            </div>

            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Version</div>
              <div style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                {meta.version || defaultVer}
              </div>
            </div>

            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Framework</div>
              <div style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                TensorFlow / Keras
              </div>
            </div>

            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Input Features</div>
              <div style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                {featureCount} numeric features
              </div>
            </div>

            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Training Method</div>
              <div style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                Federated Learning Simulation (FedAvg)
              </div>
            </div>

            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Federated Rounds</div>
              <div style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                {meta.federated_round != null ? `${meta.federated_round} rounds` : '5 rounds'}
              </div>
            </div>

            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Accuracy</div>
              <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--status-success-text)' }}>
                {formatAccuracy(meta.accuracy)}
              </div>
            </div>

            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Loss</div>
              <div style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                {meta.loss != null ? meta.loss.toFixed(4) : 'Not Available'}
              </div>
            </div>

            <div className="info-box" style={{ padding: '10px 14px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)', gridColumn: 'span 2' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Last Updated</div>
              <div style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary)' }}>
                {meta.trained_at ? new Date(meta.trained_at).toLocaleString() : 'Not Available'}
              </div>
            </div>
          </div>
        ) : (
          <div style={{
            padding: '24px',
            textAlign: 'center',
            background: 'var(--bg-primary)',
            borderRadius: 'var(--radius-md)',
            color: 'var(--text-muted)',
            fontSize: '0.85rem'
          }}>
            Model metadata unavailable. Please verify the Python ML service is running.
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="dashboard-content">
      {/* Page Header */}
      <div className="page-header-row">
        <div className="page-title-group">
          <h1>Model Information &amp; Versioning</h1>
          <p>Production ML registry metadata, framework specifications, and version provenance</p>
        </div>
        <button className="btn-secondary" onClick={loadMetadata} disabled={loading}>
          <RefreshCw size={16} className={loading ? 'spin-icon' : ''} />
          Refresh Registry
        </button>
      </div>

      {error && (
        <div style={{
          backgroundColor: 'var(--status-warning-bg)',
          color: 'var(--status-warning-text)',
          padding: '14px 18px',
          borderRadius: 'var(--radius-md)',
          marginBottom: '24px',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          border: '1px solid var(--border-color)'
        }}>
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="patients-loading">
          <div className="patients-loading-spinner" />
          <p>Loading model metadata from ML service...</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          {renderModelCard('CVD Risk Model', Heart, cvdMeta, 15, 'CVD Federated Global Model', 'CVD-v1.0')}
          {renderModelCard('Diabetes Complication Model', Activity, diabetesMeta, 8, 'Diabetes Federated Global Model', 'DIABETES-v1.0')}

          {/* Model Registry Architecture Note */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            padding: '16px 20px',
            background: 'var(--bg-surface)',
            borderRadius: 'var(--radius-md)',
            border: '1px solid var(--border-color)',
            fontSize: '0.85rem',
            color: 'var(--text-muted)'
          }}>
            <Info size={20} style={{ flexShrink: 0, color: 'var(--accent-primary)' }} />
            <span>
              <strong>Model Versioning Architecture:</strong> Models are saved in Keras HDF5/SavedModel format with immutable metadata tracking federated communication rounds, test partition accuracy, and global aggregation convergence.
            </span>
          </div>
        </div>
      )}
    </div>
  );
}
