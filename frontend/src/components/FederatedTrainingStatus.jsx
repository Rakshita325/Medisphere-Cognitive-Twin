import React from 'react';
import { Cpu, ShieldCheck, CheckCircle, Clock } from 'lucide-react';

/**
 * FederatedTrainingStatus — Displays federated learning simulation metrics.
 * Uses real backend metadata when available.
 */
export default function FederatedTrainingStatus({ cvdMeta, diabetesMeta }) {
  const meta = cvdMeta || diabetesMeta;
  const currentRound = meta?.federated_round ?? 5;
  const totalRounds = 5;
  const clientCount = meta?.client_count ?? 3;
  const convergenceStatus = meta?.convergence || 'Converged';
  const version = meta?.version || 'v1.0.0';

  return (
    <div className="federated-status-card" style={{
      background: 'var(--bg-surface)',
      border: '1px solid var(--border-color)',
      borderRadius: 'var(--radius-lg)',
      padding: '20px 24px',
      boxShadow: 'var(--shadow-sm)'
    }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{
            background: 'var(--accent-light)',
            color: 'var(--accent-primary)',
            padding: '8px',
            borderRadius: 'var(--radius-md)',
            display: 'flex'
          }}>
            <Cpu size={20} />
          </div>
          <div>
            <h3 style={{ fontSize: '1rem', fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
              Federated Learning Status
            </h3>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', margin: 0 }}>
              Privacy-preserving federated model training simulation
            </p>
          </div>
        </div>
        <span className="badge" style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '6px',
          background: 'var(--status-success-bg)',
          color: 'var(--status-success-text)',
          fontSize: '0.75rem',
          fontWeight: 600,
          padding: '4px 10px',
          borderRadius: '999px'
        }}>
          <CheckCircle size={13} /> Active
        </span>
      </div>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))',
        gap: '12px',
        marginBottom: '16px'
      }}>
        <div className="info-box" style={{ padding: '12px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Current Round</div>
          <div style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)' }}>
            Round {currentRound}
          </div>
        </div>

        <div className="info-box" style={{ padding: '12px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Total Rounds</div>
          <div style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)' }}>
            {totalRounds} / {totalRounds}
          </div>
        </div>

        <div className="info-box" style={{ padding: '12px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Participating Sites</div>
          <div style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)' }}>
            {clientCount} Hospitals
          </div>
        </div>

        <div className="info-box" style={{ padding: '12px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Convergence</div>
          <div style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--status-success-text)' }}>
            {typeof convergenceStatus === 'string' ? convergenceStatus : 'Complete'}
          </div>
        </div>

        <div className="info-box" style={{ padding: '12px', background: 'var(--bg-primary)', borderRadius: 'var(--radius-md)' }}>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Global Version</div>
          <div style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)' }}>
            {version}
          </div>
        </div>
      </div>

      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        fontSize: '0.8rem',
        color: 'var(--text-muted)',
        background: 'var(--bg-primary)',
        padding: '10px 14px',
        borderRadius: 'var(--radius-md)',
        border: '1px solid var(--border-color)'
      }}>
        <ShieldCheck size={16} style={{ color: 'var(--status-success-text)', flexShrink: 0 }} />
        <span>
          Patient data remains at participating sites; model updates are aggregated securely via FedAvg.
        </span>
      </div>
    </div>
  );
}
