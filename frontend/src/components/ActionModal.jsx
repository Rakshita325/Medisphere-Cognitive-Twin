import React from 'react';
import { X, Info, AlertTriangle, CheckCircle } from 'lucide-react';

export default function ActionModal({ isOpen, onClose, modalData }) {
  if (!isOpen || !modalData) return null;

  const { title, message, type = 'info' } = modalData;

  let Icon = Info;
  let iconColor = 'var(--accent-primary)';

  if (type === 'warning') {
    Icon = AlertTriangle;
    iconColor = 'var(--status-warning-text)';
  } else if (type === 'success') {
    Icon = CheckCircle;
    iconColor = 'var(--status-success-text)';
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Icon size={22} style={{ color: iconColor }} />
            <h3>{title}</h3>
          </div>
          <button className="modal-close-btn" onClick={onClose}>
            <X size={20} />
          </button>
        </div>

        <div style={{ padding: '12px 0 20px 0', color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
          {message}
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <button className="btn-primary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
