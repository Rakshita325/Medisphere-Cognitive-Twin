import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Activity, LogIn, AlertCircle, ArrowLeft, Eye, EyeOff, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../services/AuthContext';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const infoMessage = location.state?.message || '';

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError('Please enter both username and password.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      await login(username.trim(), password);
      navigate('/dashboard', { replace: true });
    } catch (err) {
      setError(err.message || 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-container">
        <div className="auth-card">
          <div className="auth-header">
            <Link to="/" className="auth-brand">
              <Activity size={32} className="auth-brand-icon" />
              <div>
                <div className="auth-brand-name">MediSphere</div>
                <div className="auth-brand-subtitle">Cognitive Twin</div>
              </div>
            </Link>
          </div>

          <h1 className="auth-title">Welcome Back</h1>
          <p className="auth-description">Sign in to access the clinical dashboard</p>

          {infoMessage && (
            <div style={{
              background: 'var(--status-info-bg)',
              color: 'var(--status-info-text)',
              border: '1px solid rgba(14, 165, 233, 0.2)',
              padding: '12px 16px',
              borderRadius: 'var(--radius-md)',
              fontSize: '0.875rem',
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              marginBottom: '20px'
            }}>
              <CheckCircle2 size={16} />
              <span>{infoMessage}</span>
            </div>
          )}

          {error && (
            <div className="auth-error">
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="auth-form">
            <div className="auth-field">
              <label htmlFor="login-username">Username</label>
              <input
                id="login-username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Enter your username"
                autoComplete="username"
                autoFocus
                required
              />
            </div>

            <div className="auth-field">
              <label htmlFor="login-password">Password</label>
              <div className="auth-password-wrapper">
                <input
                  id="login-password"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter your password"
                  autoComplete="current-password"
                  required
                />
                <button
                  type="button"
                  className="auth-password-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                  tabIndex={-1}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              className="btn-primary auth-submit"
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="auth-spinner" />
                  Signing in...
                </>
              ) : (
                <>
                  <LogIn size={18} />
                  Sign In
                </>
              )}
            </button>
          </form>

          <div className="auth-footer-links">
            <Link to="/" className="auth-back-link">
              <ArrowLeft size={16} />
              Back to Home
            </Link>
            <Link to="/signup" className="auth-alt-link">
              Create an account
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
