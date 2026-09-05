import React from 'react';
import { Activity, LogOut, Sun, Moon, User, Shield } from 'lucide-react';
import { useAuth } from '../services/AuthContext';

export default function Header({ theme, onToggleTheme }) {
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
    window.location.href = '/login';
  };

  const displayName = user?.fullName || user?.username || 'User';

  return (
    <header className="app-header">
      <div className="header-left">
        <div className="brand-logo">
          <Activity className="brand-icon" size={26} />
          <span>MediSphere Cognitive Twin</span>
        </div>
      </div>

      <div className="header-right">
        <button
          className="theme-toggle-btn"
          onClick={onToggleTheme}
          title={`Switch to ${theme === 'light' ? 'Dark' : 'Light'} Mode`}
        >
          {theme === 'light' ? <Moon size={18} /> : <Sun size={18} />}
        </button>

        <div className="user-status">
          <User size={16} />
          <span title={displayName}>{displayName}</span>
          {user?.role && (
            <span className="header-role-badge">
              <Shield size={12} />
              {user.role}
            </span>
          )}
        </div>

        <button className="logout-btn" onClick={handleLogout}>
          <LogOut size={14} style={{ display: 'inline', marginRight: '6px' }} />
          Logout
        </button>
      </div>
    </header>
  );
}
