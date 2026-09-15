import React from 'react';
import { CheckCircle2, AlertTriangle, HelpCircle } from 'lucide-react';

/**
 * ValidationCard — Displays validation status for Milestone 2 ML models.
 * Strictly adheres to rule: only displays "Passed" when real evidence/status is provided.
 */
export default function ValidationCard({ title, metric, status, description, icon: Icon }) {
  const getBadgeStyle = () => {
    switch (status) {
      case 'Passed':
        return {
          bg: 'var(--status-success-bg)',
          text: 'var(--status-success-text)',
          icon: <CheckCircle2 size={14} />
        };
      case 'Warning':
        return {
          bg: 'var(--status-warning-bg)',
          text: 'var(--status-warning-text)',
          icon: <AlertTriangle size={14} />
        };
      default:
        return {
          bg: 'var(--bg-primary)',
          text: 'var(--text-muted)',
          icon: <HelpCircle size={14} />
        };
    }
  };

  const badge = getBadgeStyle();

  return (
    <div className="validation-card" style={{
      background: 'var(--bg-surface)',
      border: '1px solid var(--border-color)',
      borderRadius: 'var(--radius-md)',
      padding: '18px 20px',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'space-between',
      gap: '12px',
      boxShadow: 'var(--shadow-sm)',
      transition: 'var(--transition-fast)'
    }}>
      <div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {Icon && <Icon size={18} style={{ color: 'var(--accent-primary)' }} />}
            <h4 style={{ margin: 0, fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-primary)' }}>
              {title}
            </h4>
          </div>
          <span style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '4px',
            background: badge.bg,
            color: badge.text,
            padding: '3px 8px',
            borderRadius: '999px',
            fontSize: '0.75rem',
            fontWeight: 600
          }}>
            {badge.icon} {status}
          </span>
        </div>

        {metric && (
          <div style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--text-primary)', margin: '4px 0' }}>
            {metric}
          </div>
        )}

        <p style={{ margin: 0, fontSize: '0.8rem', color: 'var(--text-secondary)', lineHeight: 1.4 }}>
          {description}
        </p>
      </div>
    </div>
  );
}
