import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Users,
  Dna,
  Activity,
  LogOut,
  User,
  Shield,
  BrainCircuit,
  BarChart3,
  TrendingUp,
  Bell,
  ClipboardList,
  FileBarChart,
  Cpu
} from 'lucide-react';
import { useAuth } from '../services/AuthContext';

const sidebarSections = [
  {
    label: 'MAIN',
    items: [
      { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard, roles: ['ADMIN', 'DOCTOR', 'NURSE'] },
      { to: '/patients', label: 'Patients', icon: Users, roles: ['ADMIN', 'DOCTOR'] },
      { to: '/twins', label: 'Digital Twins', icon: Dna, roles: ['ADMIN', 'DOCTOR'] },
    ],
  },
  {
    label: 'AI ANALYSIS',
    items: [
      { to: '/risk-predictions', label: 'Risk Predictions', icon: BrainCircuit, roles: ['ADMIN', 'DOCTOR'] },
      { to: '/shap', label: 'SHAP Explainability', icon: BarChart3, roles: ['ADMIN', 'DOCTOR'] },
      { to: '/analytics', label: 'Analytics', icon: TrendingUp, roles: ['ADMIN', 'DOCTOR'] },
    ],
  },
  {
    label: 'MONITORING',
    items: [
      { to: '/alerts', label: 'Alerts', icon: Bell, roles: ['ADMIN', 'DOCTOR', 'NURSE'] },
    ],
  },
  {
    label: 'CLINICAL',
    items: [
      { to: '/care-plans', label: 'Care Plans', icon: ClipboardList, roles: ['ADMIN', 'DOCTOR'] },
      { to: '/reports', label: 'Reports', icon: FileBarChart, roles: ['ADMIN', 'DOCTOR'] },
    ],
  },
  {
    label: 'SYSTEM',
    items: [
      { to: '/model-info', label: 'Model Information', icon: Cpu, roles: ['ADMIN', 'DOCTOR'] },
    ],
  },
];

export default function Sidebar() {
  const { user, logout, hasRole } = useAuth();

  const handleLogout = () => {
    logout();
    window.location.href = '/login';
  };

  const displayName = user?.fullName || user?.username || 'User';

  return (
    <aside className="app-sidebar">
      <div className="sidebar-brand">
        <Activity size={22} className="sidebar-brand-icon" />
        <span className="sidebar-brand-text">MediSphere</span>
      </div>

      <nav className="sidebar-nav">
        {sidebarSections.map((section) => {
          const visibleItems = section.items.filter(item => hasRole(item.roles));
          if (visibleItems.length === 0) return null;

          return (
            <div key={section.label} className="sidebar-section">
              <div className="sidebar-section-label">{section.label}</div>
              {visibleItems.map((item) => {
                const Icon = item.icon;
                return (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                  >
                    <Icon size={18} />
                    <span>{item.label}</span>
                  </NavLink>
                );
              })}
            </div>
          );
        })}
      </nav>

      <div className="sidebar-footer">
        <div className="sidebar-user-info">
          <div className="sidebar-user-avatar">
            <User size={16} />
          </div>
          <div className="sidebar-user-details">
            <span className="sidebar-user-name" title={displayName}>{displayName}</span>
            <span className="sidebar-user-role">
              <Shield size={12} />
              {user?.role || 'Unknown'}
            </span>
          </div>
        </div>
        <button className="sidebar-logout-btn" onClick={handleLogout}>
          <LogOut size={16} />
          <span>Logout</span>
        </button>
      </div>
    </aside>
  );
}
