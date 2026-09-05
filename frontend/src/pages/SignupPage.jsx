import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Activity, UserPlus, AlertCircle, ArrowLeft, Eye, EyeOff, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../services/AuthContext';

export default function SignupPage() {
  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [role, setRole] = useState('DOCTOR');
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

  const navigate = useNavigate();
  const { signup, login } = useAuth();

  const validate = () => {
    const errs = {};
    if (!fullName.trim()) errs.fullName = 'Full name is required.';
    if (!username.trim()) {
      errs.username = 'Username is required.';
    } else if (username.trim().length < 3) {
      errs.username = 'Username must be at least 3 characters.';
    } else if (!/^[a-zA-Z0-9_]+$/.test(username.trim())) {
      errs.username = 'Username can only contain letters, numbers, and underscores.';
    }

    if (!password) {
      errs.password = 'Password is required.';
    } else if (password.length < 6) {
      errs.password = 'Password must be at least 6 characters.';
    }

    if (password !== confirmPassword) {
      errs.confirmPassword = 'Passwords do not match.';
    }

    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validationErrors = validate();
    setErrors(validationErrors);
    setSuccessMessage('');

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    setLoading(true);

    try {
      await signup(fullName.trim(), username.trim(), password, role);
      setSuccessMessage('Account created successfully! Logging you in...');
      
      // Automatically log the user in
      try {
        await login(username.trim(), password);
        setTimeout(() => {
          navigate('/dashboard', { replace: true });
        }, 1000);
      } catch {
        // If auto-login fails, redirect to login page
        setTimeout(() => {
          navigate('/login', { replace: true, state: { message: 'Account created! Please log in.' } });
        }, 1500);
      }
    } catch (err) {
      setErrors({ submit: err.message || 'Registration failed. Please try again.' });
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

          <h1 className="auth-title">Create Account</h1>
          <p className="auth-description">Register for the MediSphere Clinical Platform</p>

          {successMessage && (
            <div style={{
              background: 'var(--status-success-bg)',
              color: 'var(--status-success-text)',
              border: '1px solid rgba(34, 197, 94, 0.2)',
              padding: '12px 16px',
              borderRadius: 'var(--radius-md)',
              fontSize: '0.875rem',
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              marginBottom: '20px'
            }}>
              <CheckCircle2 size={16} />
              <span>{successMessage}</span>
            </div>
          )}

          {errors.submit && (
            <div className="auth-error">
              <AlertCircle size={16} />
              <span>{errors.submit}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="auth-form">
            <div className="auth-field">
              <label htmlFor="signup-fullname">Full Name</label>
              <input
                id="signup-fullname"
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="e.g. Dr. Jane Doe"
                autoFocus
                required
              />
              {errors.fullName && <span className="auth-field-error">{errors.fullName}</span>}
            </div>

            <div className="auth-field">
              <label htmlFor="signup-username">Username</label>
              <input
                id="signup-username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="e.g. janedoe"
                autoComplete="username"
                required
              />
              {errors.username && <span className="auth-field-error">{errors.username}</span>}
            </div>

            <div className="auth-field">
              <label htmlFor="signup-password">Password</label>
              <div className="auth-password-wrapper">
                <input
                  id="signup-password"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Min. 6 characters"
                  autoComplete="new-password"
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
              {errors.password && <span className="auth-field-error">{errors.password}</span>}
            </div>

            <div className="auth-field">
              <label htmlFor="signup-confirm">Confirm Password</label>
              <input
                id="signup-confirm"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Re-enter your password"
                autoComplete="new-password"
                required
              />
              {errors.confirmPassword && <span className="auth-field-error">{errors.confirmPassword}</span>}
            </div>

            <div className="auth-field">
              <label htmlFor="signup-role">Role</label>
              <select
                id="signup-role"
                value={role}
                onChange={(e) => setRole(e.target.value)}
              >
                <option value="DOCTOR">Doctor</option>
                <option value="NURSE">Nurse</option>
              </select>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                Note: Administrator accounts are managed internally.
              </span>
            </div>

            <button
              type="submit"
              className="btn-primary auth-submit"
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="auth-spinner" />
                  Creating Account...
                </>
              ) : (
                <>
                  <UserPlus size={18} />
                  Create Account
                </>
              )}
            </button>
          </form>

          <div className="auth-footer-links">
            <Link to="/" className="auth-back-link">
              <ArrowLeft size={16} />
              Back to Home
            </Link>
            <Link to="/login" className="auth-alt-link">
              Already have an account? Sign In
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
