import React, { useState } from 'react';
import { AlertCircle, Activity, Heart, Brain, Wind, ShieldAlert } from 'lucide-react';
import { getPatientName } from '../services/api';

export default function DigitalTwin3DView({ patient, twinData }) {
  const [selectedOrgan, setSelectedOrgan] = useState('heart');

  const patientName = getPatientName(patient);

  const organs = [
    { id: 'brain', name: 'Brain & CNS', color: '#3b82f6', icon: Brain, status: 'Neutral / Normal' },
    { id: 'heart', name: 'Cardiovascular System', color: '#ef4444', icon: Heart, status: 'Neutral / Normal' },
    { id: 'lungs', name: 'Respiratory System', color: '#06b6d4', icon: Wind, status: 'Neutral / Normal' },
    { id: 'liver', name: 'Hepatic System', color: '#f59e0b', icon: Activity, status: 'Neutral / Normal' },
    { id: 'kidneys', name: 'Renal System', color: '#8b5cf6', icon: Activity, status: 'Neutral / Normal' },
  ];

  const currentOrgan = organs.find(o => o.id === selectedOrgan) || organs[1];

  return (
    <div className="organ-twin-container">
      <div className="organ-twin-header">
        <h3>3D Digital Health Twin</h3>
        <p>Interactive Anatomy Model — {patientName}</p>
      </div>

      <div className="body-svg-canvas">
        {/* Interactive Human Outline SVG */}
        <svg viewBox="0 0 200 400" width="100%" height="100%">
          <defs>
            <linearGradient id="bodyGlow" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#3b82f6" stopOpacity="0.15" />
              <stop offset="100%" stopColor="#1d4ed8" stopOpacity="0.05" />
            </linearGradient>
            <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur stdDeviation="3" result="blur" />
              <feComposite in="SourceGraphic" in2="blur" operator="over" />
            </filter>
          </defs>

          {/* Silhouette */}
          <path
            d="M 100 25 C 115 25, 122 35, 120 50 C 118 60, 112 65, 118 72 C 135 80, 155 95, 155 130 C 155 170, 142 200, 138 230 C 135 250, 142 300, 140 370 C 130 375, 115 375, 110 300 C 106 250, 102 240, 100 240 C 98 240, 94 250, 90 300 C 85 375, 70 375, 60 370 C 58 300, 65 250, 62 230 C 58 200, 45 170, 45 130 C 45 95, 65 80, 82 72 C 88 65, 82 60, 80 50 C 78 35, 85 25, 100 25 Z"
            fill="url(#bodyGlow)"
            stroke="var(--accent-primary)"
            strokeWidth="1.5"
            strokeDasharray="4 2"
          />

          {/* Brain Node */}
          <g 
            onClick={() => setSelectedOrgan('brain')}
            style={{ cursor: 'pointer' }}
            className="organ-node"
          >
            <circle 
              cx="100" 
              cy="42" 
              r={selectedOrgan === 'brain' ? '14' : '10'} 
              fill="#3b82f6" 
              opacity={selectedOrgan === 'brain' ? '0.9' : '0.6'}
              filter="url(#glow)"
            />
            <circle cx="100" cy="42" r="4" fill="#ffffff" />
          </g>

          {/* Lungs Node */}
          <g 
            onClick={() => setSelectedOrgan('lungs')}
            style={{ cursor: 'pointer' }}
            className="organ-node"
          >
            <circle 
              cx="86" 
              cy="110" 
              r={selectedOrgan === 'lungs' ? '14' : '10'} 
              fill="#06b6d4" 
              opacity={selectedOrgan === 'lungs' ? '0.9' : '0.6'}
              filter="url(#glow)"
            />
            <circle 
              cx="114" 
              cy="110" 
              r={selectedOrgan === 'lungs' ? '14' : '10'} 
              fill="#06b6d4" 
              opacity={selectedOrgan === 'lungs' ? '0.9' : '0.6'}
              filter="url(#glow)"
            />
          </g>

          {/* Heart Node */}
          <g 
            onClick={() => setSelectedOrgan('heart')}
            style={{ cursor: 'pointer' }}
            className="organ-node"
          >
            <circle 
              cx="106" 
              cy="116" 
              r={selectedOrgan === 'heart' ? '16' : '11'} 
              fill="#ef4444" 
              opacity={selectedOrgan === 'heart' ? '0.95' : '0.6'}
              filter="url(#glow)"
            />
            <circle cx="106" cy="116" r="5" fill="#ffffff" />
          </g>

          {/* Liver Node */}
          <g 
            onClick={() => setSelectedOrgan('liver')}
            style={{ cursor: 'pointer' }}
            className="organ-node"
          >
            <circle 
              cx="90" 
              cy="150" 
              r={selectedOrgan === 'liver' ? '14' : '10'} 
              fill="#f59e0b" 
              opacity={selectedOrgan === 'liver' ? '0.9' : '0.6'}
              filter="url(#glow)"
            />
          </g>

          {/* Kidneys Node */}
          <g 
            onClick={() => setSelectedOrgan('kidneys')}
            style={{ cursor: 'pointer' }}
            className="organ-node"
          >
            <circle 
              cx="112" 
              cy="175" 
              r={selectedOrgan === 'kidneys' ? '14' : '10'} 
              fill="#8b5cf6" 
              opacity={selectedOrgan === 'kidneys' ? '0.9' : '0.6'}
              filter="url(#glow)"
            />
          </g>
        </svg>
      </div>

      {/* Organ selector chips */}
      <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginTop: '16px', justifyContent: 'center' }}>
        {organs.map((o) => (
          <button
            key={o.id}
            onClick={() => setSelectedOrgan(o.id)}
            style={{
              padding: '4px 10px',
              borderRadius: '16px',
              fontSize: '0.75rem',
              fontWeight: 600,
              border: '1px solid var(--border-color)',
              background: selectedOrgan === o.id ? o.color : 'var(--bg-primary)',
              color: selectedOrgan === o.id ? '#ffffff' : 'var(--text-secondary)',
              transition: 'all 0.15s ease'
            }}
          >
            {o.name.split(' ')[0]}
          </button>
        ))}
      </div>

      <div style={{ marginTop: '14px', textAlign: 'center', width: '100%' }}>
        <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
          Selected: {currentOrgan.name}
        </div>
        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
          Status: {currentOrgan.status}
        </div>
      </div>

      {/* Explicit warning required by prompt rules */}
      <div className="risk-analysis-notice">
        <ShieldAlert size={18} />
        <span>Risk analysis not available yet</span>
      </div>
    </div>
  );
}
