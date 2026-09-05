import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Users,
  Dna,
  Activity,
  LogOut,
  User,
  Shield
} from 'lucide-react';
import { useAuth } from '../services/AuthContext';

export default function Sidebar() {
  const { user, logout, hasRole } = useAuth();

  const navItems = [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard, roles: ['ADMIN', 'DOCTOR', 'NURSE'] },
    { to: '/patients', label: 'Patients', icon: Users, roles: ['ADMIN', 'DOCTOR'] },
    { to: '/twins', label: 'Digital Twins', icon: Dna, roles: ['ADMIN', 'DOCTOR'] },
  ];

  const visibleItems = navItems.filter(item => hasRole(item.roles));

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
        {visibleItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
            >
              <Icon size={20} />
              <span>{item.label}</span>
            </NavLink>
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
